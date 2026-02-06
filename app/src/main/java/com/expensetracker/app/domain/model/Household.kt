package com.expensetracker.app.domain.model

/**
 * Domain model for a household
 * 
 * A household is a shared group where multiple users can view and manage expenses together.
 */
data class Household(
    val id: String,
    val name: String,
    val createdBy: String,
    val createdAt: Long,
    val memberIds: List<String>,
    val inviteCode: String
)
