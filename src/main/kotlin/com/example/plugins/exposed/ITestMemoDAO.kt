package com.example.plugins.exposed

import org.jetbrains.exposed.sql.Transaction

interface ITestMemoDAO {
    suspend fun find(id: Int, transaction: Transaction): TestMemoData?
    suspend fun updateMemo(memo: String?, id: Int, transaction: Transaction): Int
    suspend fun insertMemo(memo: String?, transaction: Transaction)

    suspend fun findForUpdate(id: Int, transaction: Transaction): TestMemoData?

    /**
     *  with new transaction
     */
    suspend fun find(id: Int): TestMemoData?
    /**
     *  with new transaction
     */
    suspend fun updateMemo(memo: String?, id: Int): Int
    /**
     *  with new transaction
     */
    suspend fun insertMemo(memo: String?)
}