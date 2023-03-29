package com.example.plugins.ktorm

interface ITestMemoDAO {
    fun find(id: Int): TestMemoEntity?
    fun updateMemo(memo: String?, id: Int): Int
    fun insertMemo(memo: String?): Int

    fun findForUpdate(id: Int): TestMemoEntity?

    suspend fun findWithContext(id: Int): TestMemoEntity?
    suspend fun updateMemoWithContext(memo: String?, id: Int): Int
    suspend fun insertMemoWithContext(memo: String?): Int
}