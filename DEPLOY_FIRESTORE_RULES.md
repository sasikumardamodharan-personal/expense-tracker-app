# Firestore Security Rules Deployment Guide

## Quick Start

This guide walks you through deploying and testing the Firestore security rules for the Expense Tracker app.

## Prerequisites

1. **Firebase CLI**: Install the Firebase command-line tools
   ```bash
   npm install -g firebase-tools
   ```

2. **Firebase Project**: Ensure you have a Firebase project set up
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Create or select your project

3. **Authentication**: Login to Firebase CLI
   ```bash
   firebase login
   ```

## Step 1: Initialize Firebase (If Not Already Done)

If this is your first time deploying, initialize Firebase in your project:

```bash
cd expense-tracker-app
firebase init
```

Select the following options:
- Choose "Firestore" when prompted for features
- Select your Firebase project
- Accept default file names (firestore.rules, firestore.indexes.json)

## Step 2: Deploy Security Rules

Deploy the security rules to your Firebase project:

```bash
# Deploy only Firestore rules
firebase deploy --only firestore:rules

# Or deploy both rules and indexes
firebase deploy --only firestore
```

Expected output:
```
✔  Deploy complete!

Project Console: https://console.firebase.google.com/project/YOUR-PROJECT/overview
```

## Step 3: Verify Deployment

### Via Firebase Console

1. Open [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Navigate to **Firestore Database** → **Rules**
4. Verify the rules match the content of `firestore.rules`
5. Check the "Published" timestamp to confirm recent deployment

### Via Firebase CLI

```bash
firebase firestore:rules:get
```

## Step 4: Test Security Rules

### Option A: Manual Testing via Firebase Console

1. Go to Firebase Console → Firestore Database → Rules
2. Click **"Rules Playground"** tab
3. Test different scenarios:

**Test Household Read (Should Succeed for Members)**
- Operation: `get`
- Path: `/databases/(default)/documents/households/test-household-123`
- Authentication: Set `auth.uid` to a user ID in the household's memberIds
- Click **"Run"**
- Expected: ✓ Allow

**Test Household Read (Should Fail for Non-Members)**
- Operation: `get`
- Path: `/databases/(default)/documents/households/test-household-123`
- Authentication: Set `auth.uid` to a user ID NOT in memberIds
- Click **"Run"**
- Expected: ✗ Deny

**Test Expense Create (Should Succeed for Members)**
- Operation: `create`
- Path: `/databases/(default)/documents/expenses/test-household-123/expenses/expense-456`
- Authentication: Set `auth.uid` to a member user ID
- Request data:
  ```json
  {
    "id": "expense-456",
    "householdId": "test-household-123",
    "amount": 50.00,
    "description": "Test",
    "categoryId": 1,
    "date": {"_seconds": 1700000000, "_nanoseconds": 0},
    "createdBy": "member-user-id",
    "createdAt": {"_seconds": 1700000000, "_nanoseconds": 0},
    "modifiedBy": "member-user-id",
    "modifiedAt": {"_seconds": 1700000000, "_nanoseconds": 0},
    "isDeleted": false
  }
  ```
- Click **"Run"**
- Expected: ✓ Allow

### Option B: Testing with Firebase Emulator (Recommended)

The Firebase Emulator allows you to test security rules locally without affecting production data.

1. **Start the emulator**:
   ```bash
   firebase emulators:start
   ```

2. **Access the Emulator UI**:
   - Open browser to `http://localhost:4000`
   - Navigate to Firestore tab

3. **Run test scenarios**:
   - Create test households and users
   - Test CRUD operations with different user contexts
   - Verify access control works as expected

### Option C: Automated Testing (Advanced)

For automated testing, you can use the Firebase Emulator with test scripts:

1. **Install dependencies**:
   ```bash
   npm install --save-dev @firebase/rules-unit-testing
   ```

2. **Create test file** (example: `firestore.rules.test.js`):
   ```javascript
   const { initializeTestEnvironment } = require('@firebase/rules-unit-testing');
   
   // See Firebase documentation for complete test examples
   ```

3. **Run tests**:
   ```bash
   npm test
   ```

## Step 5: Verify Requirements

Ensure all requirements from the spec are met:

### ✓ Requirement 1.4: Household Member Management
- [x] Only household members can access household data
- [x] Members can be added and removed
- [x] Access is immediately revoked when removed

### ✓ Requirement 2.1, 2.2, 2.3: Expense CRUD
- [x] Only household members can create expenses
- [x] Only household members can update expenses
- [x] Only household members can delete expenses
- [x] Expenses are stored under household collection

### ✓ Requirement 7.4: Member-Only Access
- [x] Non-members cannot access household data
- [x] Non-members cannot access expenses
- [x] Non-members cannot access categories

## Common Test Scenarios

### Scenario 1: New User Joins Household

**Setup**:
1. Create household with User A
2. User A generates invite code
3. User B joins using invite code

**Tests**:
- User B can read household data ✓
- User B can create expenses ✓
- User B can read expenses created by User A ✓
- User B can update expenses ✓

### Scenario 2: User Leaves Household

**Setup**:
1. User B is a member of household
2. User A removes User B from household

**Tests**:
- User B cannot read household data ✗
- User B cannot create expenses ✗
- User B cannot read expenses ✗
- User B cannot update expenses ✗

### Scenario 3: Unauthorized Access Attempts

**Setup**:
1. User C is not a member of any household
2. User C tries to access household data

**Tests**:
- User C cannot read household data ✗
- User C cannot create expenses ✗
- User C cannot read expenses ✗
- User C cannot access categories ✗

## Monitoring and Debugging

### View Security Rule Evaluations

1. Go to Firebase Console → Firestore Database
2. Click **"Usage"** tab
3. Review **"Security Rules Evaluations"**
4. Look for denied requests and patterns

### Enable Debug Logging

In your Android app, enable Firestore debug logging:

```kotlin
FirebaseFirestore.setLoggingEnabled(true)
```

Check Logcat for security rule evaluation details.

### Common Issues and Solutions

**Issue**: "Missing or insufficient permissions" error
- **Cause**: User is not authenticated or not a member of household
- **Solution**: Verify user is logged in and memberIds array is correct

**Issue**: Rules not updating after deployment
- **Cause**: Cache or deployment delay
- **Solution**: Wait a few minutes, clear browser cache, redeploy

**Issue**: Data validation errors
- **Cause**: Missing required fields or incorrect data types
- **Solution**: Review validation functions in firestore.rules, ensure all fields are present

## Rollback Procedure

If you need to rollback to previous rules:

1. **View deployment history**:
   ```bash
   firebase firestore:rules:list
   ```

2. **Get previous version**:
   ```bash
   firebase firestore:rules:get --version <version-number>
   ```

3. **Save to file and redeploy**:
   ```bash
   firebase firestore:rules:get --version <version-number> > firestore.rules
   firebase deploy --only firestore:rules
   ```

## Production Checklist

Before deploying to production:

- [ ] All test scenarios pass
- [ ] Security rules are reviewed and approved
- [ ] Indexes are deployed (firestore.indexes.json)
- [ ] Monitoring is set up
- [ ] Rollback procedure is documented
- [ ] Team is notified of deployment
- [ ] Backup of previous rules is saved

## Next Steps

After successful deployment:

1. **Monitor Usage**: Check Firebase Console for denied requests
2. **Update Documentation**: Keep this guide updated with any changes
3. **Regular Audits**: Review security rules quarterly
4. **Performance**: Monitor query performance and add indexes as needed

## Support

For issues or questions:
- Firebase Documentation: https://firebase.google.com/docs/firestore/security/get-started
- Firebase Support: https://firebase.google.com/support
- Project Issues: [Your project issue tracker]

## Changelog

- **2024-11-22**: Initial security rules deployment
  - Implemented household-based access control
  - Added expense CRUD permissions
  - Added category sync permissions
  - Implemented data validation
