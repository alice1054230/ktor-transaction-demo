package com.example.plugins.ktorm

import java.time.Instant
import org.ktorm.entity.Entity

interface TestMemoEntity : Entity<TestMemoEntity> {
    val id: Int
    var memo: String?
    val createTime: Instant
    var updateTime: Instant?
}
