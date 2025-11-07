package com.functions.goaltribe

import com.google.firebase.Timestamp


data class Challenge(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val createdBy: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val participants: Int = 0
)
