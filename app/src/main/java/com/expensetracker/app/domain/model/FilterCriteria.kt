package com.expensetracker.app.domain.model

data class FilterCriteria(
    val startDate: Long? = null,
    val endDate: Long? = null,
    val categoryIds: Set<Long> = emptySet()
)
