# TODO: Improvements List

## Priority Order

### 🔴 HIGH PRIORITY (Fix First)

#### 1. Fix Swipe-to-Delete Stuck Issue
**Problem**: When swiping to delete, the row moves left and gets stuck. It doesn't return to normal position whether you delete or cancel.

**Current Behavior**:
- Swipe left → Row moves left
- Dialog appears
- Delete OR Cancel → Row stays stuck on the left
- Row doesn't return to original position

**Expected Behavior**:
- Swipe left → Row moves left
- Dialog appears
- **Delete** → Row should be removed from list
- **Cancel** → Row should smoothly slide back to original position

**Technical Issue**:
- The `dismissState.reset()` in `LaunchedEffect` isn't working properly
- Need to manually handle the dismiss state after dialog interaction
- May need to use a different approach (e.g., `rememberUpdatedState` or manual state management)

**Solution Approach**:
1. Remove the automatic `LaunchedEffect` reset
2. Add explicit state reset in the dialog's `onDismiss` callback
3. Or use a simpler approach: show delete button on swipe instead of dialog
4. Or use a different swipe library/approach

---

### 🟡 MEDIUM PRIORITY

#### 2. Add Category Management
**Feature**: Allow users to add and manage custom categories within the app

**Requirements**:
- View all existing categories
- Add new category with:
  - Custom name
  - Icon selection (emoji or icon picker)
  - Color picker
- Edit existing categories
- Delete unused categories (with validation - can't delete if expenses exist)

**Implementation Needed**:
- New screen: `CategoryManagementScreen.kt`
- New ViewModel: `CategoryManagementViewModel.kt`
- Update navigation
- Add "Manage Categories" option in settings or filter screen
- Database already supports this (categories table exists)

---

#### 3. Add Currency Selection
**Feature**: Allow users to choose their preferred currency

**Requirements**:
- Settings screen with currency selector
- Support currencies:
  - Indian Rupee (₹)
  - US Dollar ($)
  - Euro (€)
  - British Pound (£)
  - Japanese Yen (¥)
  - And more...
- Save preference in DataStore
- Update all amount displays throughout app
- Format numbers according to currency locale

**Implementation Needed**:
- New screen: `SettingsScreen.kt`
- New ViewModel: `SettingsViewModel.kt`
- DataStore for preferences
- Currency formatter utility
- Update all screens that display amounts
- Add settings icon/button in main screen

---

### 🟢 LOW PRIORITY (Nice to Have)

#### 4. Auto-Refresh After Adding Expense
**Problem**: List doesn't automatically refresh after adding an expense. User must manually tap refresh button.

**Current Workaround**: Refresh button works perfectly

**Why Low Priority**: 
- Manual refresh works fine
- Not a blocker for app usage
- More complex to fix (involves PagingSource invalidation)

**Technical Challenge**:
- Room Flow should auto-update but PagingSource caches data
- Need to properly invalidate PagingSource when data changes
- May require custom PagingSource implementation
- Or use a different approach (non-paged list for small datasets)

**Possible Solutions**:
1. Invalidate PagingSource in repository after insert/update/delete
2. Use `invalidate()` callback in PagingSource
3. Switch to regular Flow for small datasets (< 100 items)
4. Add a broadcast/event system to notify list of changes

---

## Implementation Plan for Tomorrow

### Session 1: Fix Swipe-to-Delete (30 minutes)
1. Read the current SwipeableExpenseItem implementation
2. Try approach 1: Fix dismiss state reset
3. If that doesn't work, try approach 2: Simpler swipe with button
4. Test thoroughly on device
5. Verify smooth animation

### Session 2: Add Category Management (45 minutes)
1. Create CategoryManagementScreen
2. Create CategoryManagementViewModel
3. Add navigation route
4. Implement:
   - List all categories
   - Add new category form
   - Edit category
   - Delete category (with validation)
5. Test on device

### Session 3: Add Currency Selection (45 minutes)
1. Create SettingsScreen
2. Create SettingsViewModel
3. Add DataStore for preferences
4. Create currency formatter utility
5. Update all amount displays
6. Add settings navigation
7. Test currency switching

### Session 4: Auto-Refresh (Optional - 30 minutes)
- Only if time permits
- Try PagingSource invalidation approach
- If too complex, skip for now (manual refresh works)

---

## Current Status

### ✅ Working Features
- Manual refresh button (🔄) works perfectly
- Add expense
- Edit expense
- View summary
- Filter by category and date
- Smooth animations
- Haptic feedback
- Accessibility support

### ⚠️ Known Issues
1. Swipe-to-delete gets stuck (HIGH PRIORITY)
2. No auto-refresh after adding expense (LOW PRIORITY - workaround exists)

### 📋 Requested Features
1. Category management
2. Currency selection (Indian Rupee support)

---

## Notes for Tomorrow

### Before Starting:
- Make sure you have the latest code
- Test current app to reproduce issues
- Have device/emulator ready for testing

### Testing Checklist:
- [ ] Swipe-to-delete works smoothly
- [ ] Cancel returns item to position
- [ ] Delete removes item from list
- [ ] Can add custom categories
- [ ] Can edit categories
- [ ] Can delete unused categories
- [ ] Can select currency
- [ ] Currency persists after app restart
- [ ] All amounts show in selected currency

### Success Criteria:
- Swipe-to-delete works without getting stuck
- Users can add their own categories
- Users can select Indian Rupee (₹) as currency
- All features work smoothly on real device

---

## Questions to Consider Tomorrow

1. **Category Icons**: Should we use:
   - Emoji picker?
   - Predefined icon set?
   - Both options?

2. **Currency Formatting**: Should we:
   - Use device locale for number formatting?
   - Allow custom format (e.g., ₹1,00,000 vs ₹100,000)?

3. **Settings Location**: Where should settings be?
   - New tab/screen?
   - In top menu?
   - In filter screen?

4. **Auto-Refresh**: Should we:
   - Spend time fixing it?
   - Keep manual refresh as is?
   - Add pull-to-refresh gesture?

---

## Ready for Tomorrow! 🚀

All issues documented and prioritized. We'll tackle them in order:
1. Fix swipe-to-delete (most important)
2. Add category management
3. Add currency selection
4. Auto-refresh (if time permits)

See you tomorrow! 👋
