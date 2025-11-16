package com.expensetracker.app.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Utility class for debouncing rapid user interactions
 */
class Debouncer(
    private val scope: CoroutineScope,
    private val delayMillis: Long = 300L
) {
    private var debounceJob: Job? = null

    /**
     * Execute the action after the debounce delay
     * Cancels previous pending actions
     */
    fun debounce(action: () -> Unit) {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(delayMillis)
            action()
        }
    }

    /**
     * Cancel any pending debounced action
     */
    fun cancel() {
        debounceJob?.cancel()
    }
}

/**
 * Throttle utility to prevent rapid repeated actions
 */
class Throttler(
    private val scope: CoroutineScope,
    private val intervalMillis: Long = 500L
) {
    private var lastExecutionTime = 0L
    private var throttleJob: Job? = null

    /**
     * Execute the action only if enough time has passed since last execution
     */
    fun throttle(action: () -> Unit) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastExecutionTime >= intervalMillis) {
            lastExecutionTime = currentTime
            action()
        } else {
            // Schedule for later if within throttle window
            throttleJob?.cancel()
            throttleJob = scope.launch {
                val remainingDelay = intervalMillis - (currentTime - lastExecutionTime)
                delay(remainingDelay)
                lastExecutionTime = System.currentTimeMillis()
                action()
            }
        }
    }

    /**
     * Cancel any pending throttled action
     */
    fun cancel() {
        throttleJob?.cancel()
    }
}
