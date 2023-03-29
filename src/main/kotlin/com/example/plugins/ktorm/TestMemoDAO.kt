package com.example.plugins.ktorm

import java.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ktorm.database.Database
import org.ktorm.dsl.eq
import org.ktorm.dsl.from
import org.ktorm.dsl.insert
import org.ktorm.dsl.map
import org.ktorm.dsl.select
import org.ktorm.dsl.update
import org.ktorm.dsl.where
import org.ktorm.support.postgresql.LockingMode
import org.ktorm.support.postgresql.locking

class TestMemoDAO(
    private val database: Database,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ITestMemoDAO {
    override fun find(id: Int): TestMemoEntity? {
        return database.from(TestMemoTable)
            .select(TestMemoTable.columns)
            .where(TestMemoTable.id eq id)
            .map { TestMemoTable.createEntity(it) }
            .singleOrNull()
    }

    override fun updateMemo(memo: String?, id: Int): Int {
        return database.update(TestMemoTable) {
            set(it.memo, memo)
            set(it.updateTime, Instant.now())
            where {
                it.id eq id
            }
        }
    }

    override fun insertMemo(memo: String?): Int {
        return database.insert(TestMemoTable) {
            set(it.memo, memo)
        }
    }

    override fun findForUpdate(id: Int): TestMemoEntity? {
        return database.from(TestMemoTable)
            .select(TestMemoTable.columns)
            .where(TestMemoTable.id eq id)
            .locking(LockingMode.FOR_UPDATE)
            .map { TestMemoTable.createEntity(it) }
            .singleOrNull()
    }

    override suspend fun findWithContext(id: Int): TestMemoEntity? = withContext(dispatcher) {
        return@withContext find(id)
    }

    override suspend fun updateMemoWithContext(memo: String?, id: Int): Int = withContext(dispatcher) {
        return@withContext updateMemo(memo, id)
    }

    override suspend fun insertMemoWithContext(memo: String?): Int = withContext(dispatcher) {
        return@withContext insertMemo(memo)
    }

}
