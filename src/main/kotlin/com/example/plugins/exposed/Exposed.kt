package com.example.plugins.exposed

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.time.Instant
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransaction

fun Application.configureExposed() {
    val dataSource = HikariConfig().apply {
        poolName = "localPgPool"
        driverClassName = "org.postgresql.Driver"
        jdbcUrl = "jdbc:postgresql://127.0.0.1:5429/local"
        minimumIdle = 10
        maximumPoolSize = 30
        transactionIsolation = "TRANSACTION_READ_COMMITTED"
        connectionTestQuery = "SELECT 1"
        metricRegistry = null
        username = "admin"
        password = "admin"
    }.let {
        HikariDataSource(it)
    }
    val database: Database = Database.connect(dataSource)
    val testMemoDAO: ITestMemoDAO = TestMemoDAO(database)
    routing{
        route("/exposed/test") {
            get("/{id}") {
                val id = call.parameters["id"]!!.toInt()
                val model: TestMemoData? = newSuspendedTransaction(Dispatchers.IO, database) {
                    testMemoDAO.find(id, this)
                }
                if (model != null) {
                    call.respond(model)
                } else {
                    call.respond("not found")
                }
            }

            // post
            get("/create") {
                val memo: String? = call.parameters["memo"]
                newSuspendedTransaction(Dispatchers.IO, database) {
                    testMemoDAO.insertMemo(memo, this)
                }
                call.respond("OK")
            }

            // put
            get("/update/{id}") {
                val id = call.parameters["id"]!!.toInt()
                val memo: String? = call.parameters["memo"]
                newSuspendedTransaction(Dispatchers.IO, database) {
                    testMemoDAO.updateMemo(memo, id, this)
                }
                call.respond("OK")
            }

            val myDispatcher = Dispatchers.IO.limitedParallelism(4)
            val myScope = object : CoroutineScope {
                override val coroutineContext: CoroutineContext = myDispatcher.limitedParallelism(3)
            }

            val testFunc: suspend () -> Unit = {
                val parentThread = Thread.currentThread()
                var count = 0
                for (i in 0..100) {
                    withContext(myDispatcher) {
                        val childThread = Thread.currentThread()
                        if (childThread != parentThread)
                            count ++
                    }
                }
                println("$count threads are different from original thread")
                val currentThread = Thread.currentThread()
                if (parentThread != currentThread)
                    println("different thread. original: ${parentThread.hashCode()}, current: ${currentThread.hashCode()}")
            }

            get("/concurrent") {
                joinAll(
                    myScope.launch {
                        testFunc()
                    },
                    myScope.launch {
                        testFunc()
                    },
                    myScope.launch {
                        testFunc()
                    },
                    myScope.launch {
                        testFunc()
                    }
                )
                call.respond("OK")
            }

            val testDbFunc: suspend () -> Unit = {
                val parentThread = Thread.currentThread()
                var threadCount = 0
                var transactionCount = 0

                newSuspendedTransaction(myDispatcher, database) {
                    val originalTransaction = this@newSuspendedTransaction
                    for (i in 0..100) {
                        withContext(myDispatcher) {
                            val childThread = Thread.currentThread()
                            if (childThread != parentThread)
                                threadCount ++
                            originalTransaction.suspendedTransaction {
                                val currentTransaction = this@suspendedTransaction
                                if (originalTransaction != currentTransaction)
                                    transactionCount ++
                            }
                        }
                    }
                }
                println("$threadCount threads are different from original thread")
                println("$transactionCount transactions are different from original transaction")
                val currentThread = Thread.currentThread()
                if (parentThread != currentThread)
                    println("different thread. original: ${parentThread.hashCode()}, current: ${currentThread.hashCode()}")
            }

            get("/concurrent/db") {
                joinAll(
                    myScope.launch {
                        testDbFunc()
                    },
                    myScope.launch {
                        testDbFunc()
                    },
                    myScope.launch {
                        testDbFunc()
                    },
                    myScope.launch {
                        testDbFunc()
                    }
                )
                call.respond("OK")
            }
        }
    }
}

object TestMemoTable: Table("test_memo") {
    val id: Column<Int> = integer("id")
    val memo: Column<String?> = varchar("memo", 50).nullable()
    val createTime: Column<Instant> = timestamp("create_time")
    val updateTime: Column<Instant?> = timestamp("update_time").nullable()
}