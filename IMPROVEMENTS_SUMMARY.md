# Improvements Summary

## Issues to Fix

### 1. Home Page Not Refreshing After Adding Expense
**Problem**: List doesn't update immediately after adding an expense
**Solution**: The Flow should auto-update, but we'll add pull-to-refresh as backup

### 2. Add Categories via App
**Problem**: Categories are hardcoded, users can't add their own
**Solution**: Create a Category Management screen where users can:
- View all categories
- Add new categories with custom name, icon, and color
- Edit existing categories
- Delete unused categories

### 3. Currency Selection
**Problem**: App only shows USD ($)
**Solution**: Add Settings screen with currency selection:
- Indian Rupee (₹)
- US Dollar ($)
- Euro (€)
- British Pound (£)
- And more...

## Implementation Plan

### Quick Fix for Refresh Issue
The app uses Room database with Flow, which should automatically update the UI when data changes. If it's not working, it might be because:
1. The Flow collection is not set up correctly
2. The database transaction isn't completing
3. Navigation is causing the screen to not recompose

**Immediate solution**: I'll verify the Flow setup and add pull-to-refresh as a user-friendly way to manually refresh.

### For Category Management & Currency
These are new features that require:
1. New database tables/fields
2. New screens
3. New ViewModels
4. Navigation updates

Would you like me to:
- **Option A**: Just fix the refresh issue now (5 minutes)
- **Option B**: Implement all three improvements (30-45 minutes)

Let me know which you prefer, or I can start with the quick fix and then add the other features!
