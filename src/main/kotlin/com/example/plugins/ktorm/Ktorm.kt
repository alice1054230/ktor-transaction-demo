package com.example.plugins.ktorm

import com.example.plugins.exposed.TestMemoData
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ktorm.database.Database

fun Application.configureKtorm() {
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

    routing {
        route("/ktorm/test") {
            get("/{id}") {
                val id = call.parameters["id"]!!.toInt()
                val entity: TestMemoEntity? = database.useTransaction {
                    testMemoDAO.find(id)
                }
                if (entity != null) {
                    call.respond(
                        TestMemoData(
                            id = entity.id,
                            memo = entity.memo,
                            createTime = entity.createTime.toEpochMilli(),
                            updateTime = entity.updateTime?.toEpochMilli()
                        )
                    )
                } else {
                    call.respond("not found")
                }
            }

            // post
            get("/create") {
                val memo: String? = call.parameters["memo"]
                database.useTransaction {
                    testMemoDAO.insertMemo(memo)
                }
                call.respond("OK")
            }

            // put
            get("/update/{id}") {
                val id = call.parameters["id"]!!.toInt()
                val memo: String? = call.parameters["memo"]
                database.useTransaction {
                    testMemoDAO.updateMemo(memo, id)
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

                database.useTransaction { originalTransaction ->
                    for (i in 0..100) {
                        withContext(myDispatcher) {
                            val childThread = Thread.currentThread()
                            if (childThread != parentThread)
                                threadCount ++
                            database.useTransaction { currentTransaction ->
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

        route("/ktorm/test2") {
            get("/{id}") {
                val id = call.parameters["id"]!!.toInt()
                val entity: TestMemoEntity? = testMemoDAO.findWithContext(id)
                if (entity != null) {
                    call.respond(
                        TestMemoData(
                            id = entity.id,
                            memo = entity.memo,
                            createTime = entity.createTime.toEpochMilli(),
                            updateTime = entity.updateTime?.toEpochMilli()
                        )
                    )
                } else {
                    call.respond("not found")
                }
            }

            // post
            get("/create") {
                val memo: String? = call.parameters["memo"]
                testMemoDAO.insertMemoWithContext(memo)
                call.respond("OK")
            }

            // put
            get("/update/{id}") {
                val id = call.parameters["id"]!!.toInt()
                val memo: String? = call.parameters["memo"]
                testMemoDAO.updateMemoWithContext(memo, id)
                call.respond("OK")
            }
        }
    }
}