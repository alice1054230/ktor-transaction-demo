package com.example.plugins.exposed

import kotlinx.serialization.Serializable

@Serializable
data class TestMemoData(
    val id: Int,
    val memo: String?,
    val createTime: Long,
    val updateTime: Long?
)
