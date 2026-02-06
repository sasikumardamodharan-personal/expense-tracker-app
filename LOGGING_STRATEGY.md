# Logging Strategy

## Overview

The Expense Tracker app uses a centralized logging utility (`Logger`) to manage log output across different build configurations. This ensures that debug logs are not exposed in production builds while maintaining essential error and warning logs for troubleshooting.

## Logging Utility

### Location
`app/src/main/java/com/expensetracker/app/util/Logger.kt`

### Usage

```kotlin
import com.expensetracker.app.util.Logger

class MyClass {
    companion object {
        private const val TAG = "MyClass"
    }
    
    fun myFunction() {
        // Debug log (only in debug builds)
        Logger.d(TAG, "Processing started")
        
        // Info log (all builds)
        Logger.i(TAG, "User action completed")
        
        // Warning log (all builds)
        Logger.w(TAG, "Potential issue detected")
        
        // Error log (all builds)
        Logger.e(TAG, "Operation failed", exception)
    }
}
```

## Log Levels

### Debug (`Logger.d`)
- **Purpose**: Detailed information for debugging during development
- **Visibility**: Debug builds only
- **Examples**:
  - "Syncing expense 123 to Firestore"
  - "Connectivity restored, updating status"
  - "Remote version is newer, updating local"

### Info (`Logger.i`)
- **Purpose**: General informational messages about app flow
- **Visibility**: All builds
- **Examples**:
  - "User signed in successfully"
  - "Household created: Smith Family"
  - "Migration completed"

### Warning (`Logger.w`)
- **Purpose**: Potentially harmful situations that don't prevent operation
- **Visibility**: All builds
- **Examples**:
  - "No household ID, skipping sync"
  - "Retry attempt 2 for expense sync"
  - "Offline, sync will occur when connectivity is restored"

### Error (`Logger.e`)
- **Purpose**: Error events that might still allow the app to continue
- **Visibility**: All builds
- **Examples**:
  - "Failed to sync expense after retries"
  - "Migration failed: No internet connection"
  - "Error syncing expense from cloud"

### Verbose (`Logger.v`)
- **Purpose**: Most detailed logging for deep debugging
- **Visibility**: Debug builds only
- **Examples**: Rarely used, for extremely detailed tracing

## Build Configuration

### Debug Build
- All log levels are enabled
- Debug and verbose logs are shown
- Useful for development and testing

### Release Build
- Debug and verbose logs are disabled
- Info, warning, and error logs are enabled
- Helps with production troubleshooting without exposing sensitive debug info

## Current Logging in Sync Components

### ExpenseSyncCoordinator
- **Debug**: Sync operations, connectivity changes, conflict resolution
- **Warning**: Missing household ID, retry attempts, offline state
- **Error**: Sync failures, migration errors

### HouseholdManager
- **Debug**: Household operations, member management
- **Warning**: Invalid states, missing data
- **Error**: Creation/join failures, authentication errors

### DataMigrationService
- **Debug**: Migration progress, batch uploads
- **Warning**: Retry attempts, validation issues
- **Error**: Migration failures, network errors

### CategorySyncCoordinator
- **Debug**: Category sync operations
- **Warning**: Sync conflicts, missing data
- **Error**: Sync failures

## Migration from android.util.Log

If you find direct `android.util.Log` usage in the codebase, migrate to `Logger`:

### Before
```kotlin
import android.util.Log

Log.d(TAG, "Debug message")
Log.e(TAG, "Error message", exception)
```

### After
```kotlin
import com.expensetracker.app.util.Logger

Logger.d(TAG, "Debug message")
Logger.e(TAG, "Error message", exception)
```

## Best Practices

### DO
- Use appropriate log levels for different situations
- Include relevant context in log messages (IDs, states, etc.)
- Log errors with exceptions for stack traces
- Use consistent TAG naming (usually class name)
- Log important state changes and errors

### DON'T
- Log sensitive user data (passwords, tokens, personal info)
- Log excessively in tight loops
- Use debug logs for production-critical information
- Include PII (Personally Identifiable Information)
- Log full objects (use specific fields instead)

## Examples

### Good Logging

```kotlin
// Clear, contextual debug log
Logger.d(TAG, "Syncing expense ${expense.id} to household $householdId")

// Error with exception
Logger.e(TAG, "Failed to sync expense ${expense.id}", exception)

// Warning with actionable info
Logger.w(TAG, "Retry attempt $attemptCount of $maxRetries for expense ${expense.id}")
```

### Bad Logging

```kotlin
// Too vague
Logger.d(TAG, "Syncing")

// Sensitive data
Logger.d(TAG, "User email: ${user.email}")

// Excessive detail
Logger.d(TAG, "Expense object: $expense")
```

## Disabling Logs in Production

To completely disable logging in production:

1. Open `Logger.kt`
2. Set `ENABLE_LOGGING = false`
3. Rebuild the app

This will remove all log statements at compile time (when using ProGuard/R8).

## ProGuard/R8 Configuration

Add to `proguard-rules.pro` to remove log statements in release builds:

```proguard
# Remove all logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

-assumenosideeffects class com.expensetracker.app.util.Logger {
    public static *** d(...);
    public static *** v(...);
}
```

## Monitoring and Analytics

For production monitoring, consider integrating:
- **Firebase Crashlytics**: Automatic crash reporting
- **Firebase Analytics**: User behavior tracking
- **Custom Events**: Track sync success/failure rates

These provide better insights than logs for production apps.

## Future Enhancements

1. **Remote Logging**: Send error logs to a backend service
2. **Log Levels by Module**: Different log levels for different components
3. **Performance Logging**: Track operation durations
4. **User-Facing Logs**: Export logs for user support
5. **Structured Logging**: JSON-formatted logs for parsing

---

*Last Updated: November 2025*
