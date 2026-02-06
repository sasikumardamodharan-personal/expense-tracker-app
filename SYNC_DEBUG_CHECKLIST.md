# Sync Debugging Checklist

When expenses aren't appearing in Firestore, check these items in order:

## 1. Check Logcat for Sync Messages

Filter Logcat by these tags to see what's happening:
- `ExpenseSyncCoordinator` - Shows sync operations
- `FirestoreExpenseRepo` - Shows Firestore write operations
- `AddEditExpenseViewModel` - Shows when save is triggered

Look for these specific messages:
- ✅ "Syncing expense X to Firestore" - Sync was triggered
- ✅ "Successfully synced expense X" - Sync completed
- ❌ "No household ID, skipping sync" - Household not set up
- ❌ "Offline, sync will occur when connectivity is restored" - No internet
- ❌ "Failed to sync expense" - Sync error occurred

## 2. Verify Household Setup

In your app, check:
1. Go to Settings screen
2. Verify you see a Household ID displayed
3. If no household ID, you need to create/join a household first

## 3. Verify Authentication

Check Logcat for:
- `AuthManager` or `FirebaseAuth` tags
- Ensure you see "User authenticated" or similar messages
- If not authenticated, sign in again

## 4. Check Firebase Console

1. Open Firebase Console: https://console.firebase.google.com
2. Navigate to your project
3. Go to Firestore Database
4. Look for collection structure:
   ```
   expenses/
     └── {householdId}/
         └── expenses/
             └── {expenseId}
   ```

## 5. Check Network Connectivity

- Ensure device/emulator has internet access
- Try opening a browser on the device
- Check if Firebase is reachable

## 6. Common Issues

### Issue: "No household ID, skipping sync"
**Solution**: Create or join a household first
1. Go to Household Setup screen
2. Either create a new household or join with an invite code

### Issue: Expenses save locally but don't sync
**Solution**: Check if sync coordinator is started
- Sync should start automatically when household is set up
- Look for "Starting sync coordinator" in logs

### Issue: "User not authenticated"
**Solution**: Sign in to Firebase Auth
- Ensure Firebase Authentication is configured
- User must be signed in before syncing

### Issue: Permission denied errors
**Solution**: Check Firestore security rules
- Rules must allow authenticated users to write to their household
- See FIRESTORE_SECURITY_RULES.md for correct rules

## 7. Manual Test Steps

1. **Clear app data** (optional, for clean test)
2. **Sign in** to Firebase Auth
3. **Create/Join household**
4. **Add an expense**
5. **Check Logcat** for sync messages
6. **Check Firebase Console** for the expense document

## 8. Enable Verbose Logging

If you need more details, you can temporarily add more logs:

In `AddEditExpenseViewModel.kt`, after line 228, add:
```kotlin
Log.d("DEBUG_SYNC", "About to sync expense: id=$expenseIdToSync, expense=$savedExpense")
```

In `ExpenseSyncCoordinator.kt`, at the start of `syncExpenseToCloud`, add:
```kotlin
Log.d("DEBUG_SYNC", "syncExpenseToCloud called: expense.id=${expense.id}, householdId=$householdId")
```
