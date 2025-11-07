package com.functions.goaltribe

data class Tribe(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "",
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val avatarBase64: String? = null
)
