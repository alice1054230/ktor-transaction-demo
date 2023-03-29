package com.example.plugins.exposed

import java.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransaction
import org.jetbrains.exposed.sql.update

/**
 *  in exposed, any DB process should be in a transaction
 */
class TestMemoDAO(
    private val database: Database, // if database is singleton, no need to pass it.
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ITestMemoDAO {
    override suspend fun find(id: Int, transaction: Transaction): TestMemoData? = transaction.suspendedTransaction(dispatcher) {
        return@suspendedTransaction doFind(id)
    }

    override suspend fun updateMemo(memo: String?, id: Int, transaction: Transaction): Int = transaction.suspendedTransaction(dispatcher) {
        return@suspendedTransaction doUpdateMemo(memo, id)
    }

    override suspend fun insertMemo(memo: String?, transaction: Transaction): Unit = transaction.suspendedTransaction(dispatcher) {
        doInsert(memo)
    }

    override suspend fun findForUpdate(id: Int, transaction: Transaction): TestMemoData? = transaction.suspendedTransaction(dispatcher) {
        return@suspendedTransaction doFindForUpdate(id)
    }

    override suspend fun find(id: Int): TestMemoData? = newSuspendedTransaction(dispatcher) {
        return@newSuspendedTransaction doFind(id)
    }

    override suspend fun updateMemo(memo: String?, id: Int): Int = newSuspendedTransaction(dispatcher) {
        return@newSuspendedTransaction doUpdateMemo(memo, id)
    }

    override suspend fun insertMemo(memo: String?): Unit = newSuspendedTransaction(dispatcher) {
        doInsert(memo)
    }

    private fun doFind(
        id: Int
    ): TestMemoData? = TestMemoTable.select { TestMemoTable.id eq id }
        .map { row ->
            TestMemoData(
                id = row[TestMemoTable.id],
                memo = row[TestMemoTable.memo],
                createTime = row[TestMemoTable.createTime].toEpochMilli(),
                updateTime = row[TestMemoTable.updateTime]?.toEpochMilli()
            )
        }.singleOrNull()

    private fun doUpdateMemo(memo: String?, id: Int): Int {
        return TestMemoTable.update({ TestMemoTable.id eq id }) {
            it[this.memo] = memo
            it[this.updateTime] = Instant.now()
        }
    }

    private fun doInsert(memo: String?): InsertStatement<Number> {
        return TestMemoTable.insert {
            it[this.memo] = memo
        }
    }

    private fun doFindForUpdate(
        id: Int
    ): TestMemoData? = TestMemoTable.select { TestMemoTable.id eq id }
        .forUpdate()
        .map { row ->
            TestMemoData(
                id = row[TestMemoTable.id],
                memo = row[TestMemoTable.memo],
                createTime = row[TestMemoTable.createTime].toEpochMilli(),
                updateTime = row[TestMemoTable.updateTime]?.toEpochMilli()
            )
        }.singleOrNull()
}