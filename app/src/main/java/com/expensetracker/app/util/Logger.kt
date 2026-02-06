package com.expensetracker.app.util

import android.util.Log

/**
 * Centralized logging utility for the Expense Tracker app.
 * 
 * This utility provides conditional logging based on build configuration:
 * - Debug logs (d) are only shown in debug builds
 * - Info logs (i) are shown in all builds
 * - Warning logs (w) are shown in all builds
 * - Error logs (e) are shown in all builds
 * 
 * Usage:
 * ```kotlin
 * Logger.d(TAG, "Debug message")
 * Logger.e(TAG, "Error message", exception)
 * ```
 * 
 * To disable all logging in production, set ENABLE_LOGGING to false.
 */
object Logger {
    
    /**
     * Master switch for logging. Set to false to disable all logs in production.
     */
    private const val ENABLE_LOGGING = true
    
    /**
     * Enable debug logs only in debug builds
     * Note: Set to true for now. In production, use BuildConfig.DEBUG when available.
     */
    private const val ENABLE_DEBUG_LOGS = true
    
    /**
     * Log a debug message. Only shown in debug builds.
     * 
     * @param tag Used to identify the source of a log message
     * @param message The message to log
     */
    fun d(tag: String, message: String) {
        if (ENABLE_LOGGING && ENABLE_DEBUG_LOGS) {
            Log.d(tag, message)
        }
    }
    
    /**
     * Log an info message. Shown in all builds.
     * 
     * @param tag Used to identify the source of a log message
     * @param message The message to log
     */
    fun i(tag: String, message: String) {
        if (ENABLE_LOGGING) {
            Log.i(tag, message)
        }
    }
    
    /**
     * Log a warning message. Shown in all builds.
     * 
     * @param tag Used to identify the source of a log message
     * @param message The message to log
     */
    fun w(tag: String, message: String) {
        if (ENABLE_LOGGING) {
            Log.w(tag, message)
        }
    }
    
    /**
     * Log a warning message with throwable. Shown in all builds.
     * 
     * @param tag Used to identify the source of a log message
     * @param message The message to log
     * @param throwable An exception to log
     */
    fun w(tag: String, message: String, throwable: Throwable) {
        if (ENABLE_LOGGING) {
            Log.w(tag, message, throwable)
        }
    }
    
    /**
     * Log an error message. Shown in all builds.
     * 
     * @param tag Used to identify the source of a log message
     * @param message The message to log
     */
    fun e(tag: String, message: String) {
        if (ENABLE_LOGGING) {
            Log.e(tag, message)
        }
    }
    
    /**
     * Log an error message with throwable. Shown in all builds.
     * 
     * @param tag Used to identify the source of a log message
     * @param message The message to log
     * @param throwable An exception to log
     */
    fun e(tag: String, message: String, throwable: Throwable) {
        if (ENABLE_LOGGING) {
            Log.e(tag, message, throwable)
        }
    }
    
    /**
     * Log a verbose message. Only shown in debug builds.
     * 
     * @param tag Used to identify the source of a log message
     * @param message The message to log
     */
    fun v(tag: String, message: String) {
        if (ENABLE_LOGGING && ENABLE_DEBUG_LOGS) {
            Log.v(tag, message)
        }
    }
}
