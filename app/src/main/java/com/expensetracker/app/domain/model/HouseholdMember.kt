package com.expensetracker.app.domain.model

/**
 * Domain model for a household member
 */
data class HouseholdMember(
    val userId: String,
    val email: String,
    val displayName: String,
    val joinedAt: Long
)
