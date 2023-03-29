package com.example.plugins.ktorm

import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.timestamp
import org.ktorm.schema.varchar

object TestMemoTable : Table<TestMemoEntity>("test_memo") {
    val id = int("id").bindTo { it.id }
    val memo = varchar("memo").bindTo { it.memo }
    val createTime = timestamp("create_time").bindTo { it.createTime }
    val updateTime = timestamp("update_time").bindTo { it.updateTime }
}
