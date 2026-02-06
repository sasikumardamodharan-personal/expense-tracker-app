# Manual Testing Guide - Firestore Sync Feature

This guide provides step-by-step instructions for manually testing the Firestore sync functionality to verify all requirements are met.

## Prerequisites

- Two Android devices or emulators (Device A and Device B)
- Firebase project configured with Firestore
- Google Sign-In enabled
- Internet connectivity on both devices

## Test Scenarios

### 1. Household Creation and Joining

**Requirement:** 1.1, 1.2, 1.3, 1.4

#### Test Steps:

**On Device A:**
1. Launch the app
2. Sign in with Google account
3. When prompted, select "Create Household"
4. Enter household name (e.g., "Smith Family")
5. Tap "Create"
6. Verify household is created successfully
7. Navigate to Settings → Household
8. Note the invite code displayed

**Expected Results:**
- ✓ Household creation prompt appears on first sign-in
- ✓ Household is created with unique ID
- ✓ Invite code is generated and displayed
- ✓ User is added as household member

**On Device B:**
1. Launch the app
2. Sign in with different Google account
3. When prompted, select "Join Household"
4. Enter the invite code from Device A
5. Tap "Join"
6. Verify successful join message

**Expected Results:**
- ✓ Join household option is available
- ✓ Invite code is accepted
- ✓ User is added to household member list
- ✓ Both devices show same household name

---

### 2. Real-Time Sync Between Devices

**Requirement:** 2.1, 2.2, 2.3, 3.1, 3.2, 3.3, 3.4, 3.5

#### Test Steps:

**Add Expense on Device A:**
1. On Device A, tap the "+" FAB button
2. Enter amount: $50.00
3. Select category: Food
4. Enter description: "Groceries"
5. Tap "Save"
6. Verify expense appears in list with sync indicator

**Verify on Device B:**
1. Wait up to 5 seconds
2. Check if expense appears automatically
3. Verify all details match (amount, category, description)

**Expected Results:**
- ✓ Expense syncs to Firestore immediately
- ✓ Device B receives update within 5 seconds
- ✓ All expense details are accurate
- ✓ Sync status indicator shows "Synced"

**Edit Expense on Device B:**
1. On Device B, tap the expense
2. Change amount to $55.00
3. Change description to "Groceries and snacks"
4. Tap "Save"

**Verify on Device A:**
1. Wait up to 5 seconds
2. Check if changes appear automatically
3. Verify updated amount and description

**Expected Results:**
- ✓ Edit syncs to Firestore immediately
- ✓ Device A receives update within 5 seconds
- ✓ Updated details are accurate

**Delete Expense on Device A:**
1. On Device A, swipe left on the expense
2. Tap "Delete"
3. Confirm deletion

**Verify on Device B:**
1. Wait up to 5 seconds
2. Verify expense is removed from list

**Expected Results:**
- ✓ Deletion syncs to Firestore immediately
- ✓ Device B removes expense within 5 seconds
- ✓ Expense is no longer visible on either device

---

### 3. Offline Mode and Auto-Sync

**Requirement:** 4.1, 4.2, 4.3, 4.4, 4.5

#### Test Steps:

**Add Expense While Offline:**
1. On Device A, turn off WiFi and mobile data
2. Tap the "+" FAB button
3. Enter amount: $25.00
4. Select category: Transport
5. Enter description: "Bus fare"
6. Tap "Save"
7. Verify expense appears in list
8. Check sync status indicator (should show offline icon)

**Expected Results:**
- ✓ Expense is saved to local database
- ✓ Offline indicator is displayed
- ✓ No error messages appear
- ✓ Expense is visible in list

**Restore Connectivity:**
1. Turn WiFi back on
2. Wait for automatic sync (up to 10 seconds)
3. Check sync status indicator

**Expected Results:**
- ✓ Sync status changes from offline to syncing
- ✓ Sync completes automatically
- ✓ Sync status shows "Synced"
- ✓ No user intervention required

**Verify on Device B:**
1. Check if offline expense appears
2. Verify all details are correct

**Expected Results:**
- ✓ Expense appears on Device B
- ✓ All details match

**Multiple Offline Changes:**
1. On Device A, turn off connectivity again
2. Add 3 different expenses
3. Edit 1 existing expense
4. Delete 1 existing expense
5. Turn connectivity back on
6. Wait for sync

**Expected Results:**
- ✓ All changes are queued locally
- ✓ All changes sync when connectivity restored
- ✓ Device B reflects all changes
- ✓ No data loss occurs

---

### 4. Data Migration

**Requirement:** 6.1, 6.2, 6.3, 6.4, 6.5

#### Test Steps:

**Prepare Local Data:**
1. Install app on fresh device (Device C)
2. Skip sign-in (if possible) or use local-only mode
3. Add 10 expenses with various categories
4. Verify all expenses are saved locally

**Migrate to Cloud:**
1. Sign in with Google account
2. Create or join a household
3. When prompted, confirm data migration
4. Observe migration progress dialog

**Expected Results:**
- ✓ Migration prompt appears
- ✓ Progress indicator shows migration status
- ✓ All 10 expenses are uploaded
- ✓ Success message appears when complete

**Verify Migration:**
1. Check all expenses in list
2. Verify sync status shows "Synced"
3. On another device in same household, verify expenses appear

**Expected Results:**
- ✓ All expenses preserved with correct details
- ✓ Dates and amounts are accurate
- ✓ Categories are maintained
- ✓ Expenses visible on other devices

**Migration Error Handling:**
1. On fresh device, add expenses
2. Turn off connectivity
3. Attempt to migrate
4. Observe error message

**Expected Results:**
- ✓ Clear error message about connectivity
- ✓ Option to retry when online
- ✓ Local data remains intact

---

### 5. Category Sync

**Requirement:** 5.1, 5.2, 5.3, 5.4, 5.5

#### Test Steps:

**Add Custom Category on Device A:**
1. Navigate to Settings → Categories
2. Tap "Add Category"
3. Enter name: "Pets"
4. Select icon and color
5. Tap "Save"

**Verify on Device B:**
1. Wait up to 5 seconds
2. Navigate to Settings → Categories
3. Check if "Pets" category appears

**Expected Results:**
- ✓ Category syncs to Firestore
- ✓ Device B receives new category
- ✓ Icon and color are preserved
- ✓ Category available for expense selection

**Use Synced Category:**
1. On Device B, add expense
2. Select "Pets" category
3. Complete and save expense

**Verify on Device A:**
1. Check if expense appears
2. Verify "Pets" category is displayed correctly

**Expected Results:**
- ✓ Expense with custom category syncs
- ✓ Category displays correctly on both devices

---

### 6. Conflict Resolution

**Requirement:** 8.1, 8.2, 8.3, 8.4

#### Test Steps:

**Simultaneous Edits:**
1. On both devices, turn off connectivity
2. On Device A, edit same expense: change amount to $100
3. On Device B, edit same expense: change amount to $200
4. Turn connectivity back on for Device A first
5. Wait for sync
6. Turn connectivity back on for Device B
7. Wait for sync

**Expected Results:**
- ✓ No errors occur
- ✓ Last-write-wins: Device B's change ($200) should win
- ✓ Both devices show $200 after sync
- ✓ No data corruption

**Verify Conflict Logs:**
1. Check app logs (if accessible)
2. Verify conflict was detected and resolved

**Expected Results:**
- ✓ Conflict logged for debugging
- ✓ Resolution strategy applied correctly

---

### 7. Household Member Management

**Requirement:** 7.1, 7.2, 7.3, 7.4

#### Test Steps:

**View Members:**
1. On Device A, navigate to Settings → Household
2. Tap "View Members"
3. Verify list of household members

**Expected Results:**
- ✓ All members displayed with names and emails
- ✓ Last sync time shown for each member
- ✓ Total member count displayed

**Leave Household:**
1. On Device B, navigate to Settings → Household
2. Tap "Leave Household"
3. Confirm action
4. Verify household is left

**Expected Results:**
- ✓ Confirmation dialog appears
- ✓ User is removed from household
- ✓ Expenses no longer visible
- ✓ Prompted to create/join new household

**Verify on Device A:**
1. Navigate to household members
2. Check if Device B's user is removed

**Expected Results:**
- ✓ Member list updated
- ✓ Removed user no longer appears

---

### 8. Error Handling

**Requirement:** 10.1, 10.2, 10.3, 10.4, 10.5

#### Test Steps:

**Network Error:**
1. Turn off connectivity
2. Add expense
3. Observe error message

**Expected Results:**
- ✓ User-friendly message: "No internet connection"
- ✓ Indicates changes will sync when online
- ✓ No crash or technical error

**Authentication Error:**
1. Sign out from Firebase (if possible via dev tools)
2. Attempt to add expense
3. Observe error message

**Expected Results:**
- ✓ Message: "Session expired, please sign in again"
- ✓ Redirected to sign-in screen
- ✓ Local data preserved

**Permission Error:**
1. Manually revoke Firestore permissions (via Firebase console)
2. Attempt to add expense
3. Observe error message

**Expected Results:**
- ✓ Message about permission issue
- ✓ Suggestion to check household membership
- ✓ No crash

---

### 9. Performance and Battery

**Requirement:** 9.1, 9.2, 9.3, 9.4, 9.5

#### Test Steps:

**Monitor Battery Usage:**
1. Use app normally for 1 hour
2. Add/edit/delete expenses regularly
3. Check battery usage in device settings

**Expected Results:**
- ✓ Battery usage is reasonable (< 5% per hour)
- ✓ No excessive background activity
- ✓ Sync doesn't drain battery

**Monitor Data Usage:**
1. Reset data usage counter
2. Use app for 1 hour with sync enabled
3. Check data usage in device settings

**Expected Results:**
- ✓ Data usage is minimal (< 5 MB per hour)
- ✓ Only changed data is synced
- ✓ No unnecessary network calls

**Rapid Changes:**
1. Quickly add 10 expenses in succession
2. Observe sync behavior

**Expected Results:**
- ✓ Changes are throttled/batched
- ✓ Not every change triggers immediate sync
- ✓ All changes eventually sync
- ✓ No performance degradation

---

### 10. Pull-to-Refresh

**Requirement:** 3.5

#### Test Steps:

**Manual Sync:**
1. On Device A, add expense
2. On Device B, pull down on expense list
3. Observe refresh animation
4. Verify new expense appears

**Expected Results:**
- ✓ Pull-to-refresh gesture works
- ✓ Sync progress indicator shown
- ✓ List updates with latest data
- ✓ Manual sync completes successfully

---

## Verification Checklist

After completing all test scenarios, verify the following:

### Functional Requirements
- [ ] Household creation works correctly
- [ ] Household joining with invite code works
- [ ] Expenses sync to cloud automatically
- [ ] Real-time updates appear within 5 seconds
- [ ] Offline mode saves changes locally
- [ ] Auto-sync works when connectivity restored
- [ ] Data migration preserves all data
- [ ] Categories sync across devices
- [ ] Conflict resolution uses last-write-wins
- [ ] Member management works correctly

### Non-Functional Requirements
- [ ] Sync is fast (< 5 seconds)
- [ ] Battery usage is reasonable
- [ ] Data usage is minimal
- [ ] No crashes or errors
- [ ] Error messages are user-friendly
- [ ] UI is responsive during sync
- [ ] No data loss occurs

### Edge Cases
- [ ] Multiple offline changes sync correctly
- [ ] Simultaneous edits resolve properly
- [ ] Large number of expenses sync efficiently
- [ ] Network interruptions handled gracefully
- [ ] Authentication errors handled properly

---

## Reporting Issues

If any test fails, document:
1. Test scenario name
2. Steps to reproduce
3. Expected result
4. Actual result
5. Device information
6. Screenshots/logs if available

---

## Notes

- Some tests require Firebase console access for advanced scenarios
- Real-time sync timing may vary based on network conditions
- Battery and data usage tests should be conducted over extended periods
- Conflict resolution tests require precise timing and may need multiple attempts
