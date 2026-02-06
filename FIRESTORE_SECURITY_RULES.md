# Firestore Security Rules Documentation

## Overview

This document describes the Firestore security rules implemented for the Expense Tracker app and provides instructions for deployment and testing.

## Security Rules Summary

### Household Access Control

**Requirements Addressed:** 1.4, 7.4

- **Read Access**: Only authenticated users who are members of a household can read household data
- **Create Access**: Any authenticated user can create a new household
- **Update Access**: Only household members can update household information
- **Delete Access**: Households cannot be deleted (prevented for data integrity)

### Expense Access Control

**Requirements Addressed:** 2.1, 2.2, 2.3

- **Read Access**: Only household members can read expenses
- **Create Access**: Only household members can create expenses, and they must set themselves as the creator
- **Update Access**: Only household members can update expenses, and they must set themselves as the modifier
- **Delete Access**: Only household members can delete expenses

### Category Access Control

**Requirements Addressed:** 5.1, 5.2, 5.3, 5.4, 5.5

- **Read Access**: Only household members can read categories
- **Create Access**: Only household members can create categories
- **Update Access**: Only household members can update categories
- **Delete Access**: Only household members can delete categories

## Data Validation

The security rules enforce the following validations:

### Household Validation
- Name must be a non-empty string (1-100 characters)
- Must have createdBy, createdAt, memberIds, and inviteCode fields
- Creator must be in the memberIds list

### Expense Validation
- Amount must be a non-negative number
- Description must be a string (max 500 characters)
- Must have all required fields (id, householdId, amount, description, categoryId, date, createdBy, createdAt, modifiedBy, modifiedAt, isDeleted)
- householdId must match the collection path
- createdBy and createdAt cannot be modified after creation

### Category Validation
- Name must be a non-empty string (1-50 characters)
- Must have all required fields (id, householdId, name, icon, createdBy, createdAt, modifiedAt, isDeleted)
- householdId must match the collection path
- createdBy and createdAt cannot be modified after creation

## Deployment Instructions

### Prerequisites
1. Install Firebase CLI: `npm install -g firebase-tools`
2. Login to Firebase: `firebase login`
3. Initialize Firebase in your project (if not already done): `firebase init`

### Deploy Security Rules

```bash
# Navigate to the project directory
cd expense-tracker-app

# Deploy only Firestore rules
firebase deploy --only firestore:rules

# Or deploy all Firebase resources
firebase deploy
```

### Verify Deployment

After deployment, verify the rules are active:
1. Go to Firebase Console
2. Navigate to Firestore Database
3. Click on "Rules" tab
4. Verify the rules match the content of `firestore.rules`

## Testing Instructions

### Manual Testing Scenarios

#### Test 1: Household Access Permissions

**Objective**: Verify that only household members can access household data

**Steps**:
1. Create a household with User A
2. Try to read the household with User B (not a member) - Should FAIL
3. Add User B to the household
4. Try to read the household with User B - Should SUCCEED
5. Try to update household with User B - Should SUCCEED
6. Remove User B from the household
7. Try to read the household with User B - Should FAIL

**Expected Results**:
- Non-members cannot read or write household data
- Members can read and write household data
- Removed members lose access immediately

#### Test 2: Expense CRUD Permissions

**Objective**: Verify expense operations are restricted to household members

**Steps**:
1. Create a household with User A
2. Create an expense with User A - Should SUCCEED
3. Try to create an expense with User B (not a member) - Should FAIL
4. Add User B to the household
5. Create an expense with User B - Should SUCCEED
6. Update the expense with User B - Should SUCCEED
7. Delete the expense with User B - Should SUCCEED
8. Remove User B from the household
9. Try to create an expense with User B - Should FAIL

**Expected Results**:
- Only household members can perform CRUD operations on expenses
- Non-members are denied all access
- Expenses must have valid householdId matching the collection path

#### Test 3: Category Sync Permissions

**Objective**: Verify category operations are restricted to household members

**Steps**:
1. Create a household with User A
2. Create a category with User A - Should SUCCEED
3. Try to create a category with User B (not a member) - Should FAIL
4. Add User B to the household
5. Create a category with User B - Should SUCCEED
6. Update the category with User B - Should SUCCEED
7. Try to read categories with User C (not a member) - Should FAIL
8. Add User C to the household
9. Read categories with User C - Should SUCCEED

**Expected Results**:
- Only household members can perform CRUD operations on categories
- Non-members cannot read or write categories
- All household members see the same categories

#### Test 4: Member-Only Access Verification

**Objective**: Verify that access is immediately revoked when a user is removed from a household

**Steps**:
1. Create a household with User A
2. Add User B to the household
3. User B creates an expense
4. User B reads all expenses - Should SUCCEED
5. User A removes User B from the household
6. User B tries to read expenses - Should FAIL
7. User B tries to create an expense - Should FAIL
8. User B tries to update their previously created expense - Should FAIL

**Expected Results**:
- Access is immediately revoked upon removal from household
- Previously created data remains but is no longer accessible to removed user
- All operations fail with permission denied error

### Automated Testing with Firebase Emulator

You can test security rules locally using the Firebase Emulator Suite:

```bash
# Install Firebase emulator
firebase init emulators

# Start the emulator
firebase emulators:start

# Run tests (if you have test scripts)
npm test
```

### Testing with Firebase Console

1. Go to Firebase Console → Firestore Database
2. Click on "Rules" tab
3. Click "Rules Playground"
4. Select operation type (get, list, create, update, delete)
5. Enter document path
6. Set authentication context
7. Click "Run" to test

## Security Best Practices

1. **Never disable security rules** - Always maintain proper access control
2. **Validate all data** - Ensure data types and constraints are enforced
3. **Use helper functions** - Keep rules DRY and maintainable
4. **Test thoroughly** - Test all access scenarios before deploying to production
5. **Monitor access** - Use Firebase Console to monitor denied requests
6. **Regular audits** - Periodically review and update security rules

## Troubleshooting

### Common Issues

**Issue**: Permission denied errors in production
- **Solution**: Verify user is authenticated and is a member of the household
- **Check**: Ensure memberIds array is properly updated when users join/leave

**Issue**: Rules not updating after deployment
- **Solution**: Clear browser cache and refresh Firebase Console
- **Check**: Verify deployment was successful with `firebase deploy --only firestore:rules`

**Issue**: Data validation errors
- **Solution**: Ensure all required fields are present and have correct types
- **Check**: Review the validation functions in firestore.rules

## Monitoring

Monitor security rule violations in Firebase Console:
1. Go to Firestore Database
2. Click on "Usage" tab
3. Review "Security Rules Evaluations"
4. Check for denied requests and investigate patterns

## References

- [Firebase Security Rules Documentation](https://firebase.google.com/docs/firestore/security/get-started)
- [Security Rules Testing](https://firebase.google.com/docs/rules/unit-tests)
- [Best Practices](https://firebase.google.com/docs/firestore/security/rules-conditions)
