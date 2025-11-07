package com.functions.goaltribe


data class Goal(
    // Default values are crucial for Firestore toObject mapping
    val goalName: String = "",
    val progress: Int = 0,
    val motivation: String = "",
    val runHabit: Boolean = false,
    val stretchHabit: Boolean = false,
    val targetDate: String = "",
    val id: String = "" // Added for easy reference later
)
