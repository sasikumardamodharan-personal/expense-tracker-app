package com.expensetracker.app.domain.model

/**
 * Domain model representing a user in the expense tracker app
 */
data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
