# Firestore Security Rules Implementation

## Overview

This directory contains the Firestore security rules implementation for the Expense Tracker app. The security rules enforce household-based access control, ensuring that only authenticated household members can access and modify shared expense data.

## Files

| File | Purpose |
|------|---------|
| `firestore.rules` | Main security rules file defining access control |
| `firestore.indexes.json` | Database indexes for query optimization |
| `firebase.json` | Firebase project configuration |
| `FIRESTORE_SECURITY_RULES.md` | Detailed documentation of security rules |
| `DEPLOY_FIRESTORE_RULES.md` | Step-by-step deployment guide |
| `SECURITY_RULES_REFERENCE.md` | Quick reference for developers |
| `firestore-rules-test.js` | Automated test script (Node.js) |
| `package.json` | NPM dependencies for testing |

## Quick Start

### 1. Install Dependencies

```bash
npm install
```

### 2. Deploy Rules

```bash
# Login to Firebase
firebase login

# Deploy security rules
npm run deploy:rules
```

### 3. Test Rules

```bash
# Start Firebase emulator
npm run emulator:start

# In another terminal, run tests
npm run test:rules
```

## Security Model

### Household-Based Access Control

All data in the app is scoped to households. Users must be members of a household to access its data.

```
User A (Member) ──┐
                  ├──> Household 1 ──> Expenses, Categories
User B (Member) ──┘

User C (Not Member) ──X──> Cannot access Household 1 data
```

### Access Rules

1. **Authentication Required**: All operations require user authentication
2. **Membership Required**: Users must be household members to access data
3. **Data Validation**: All writes are validated for structure and constraints
4. **Audit Trail**: All modifications track who and when

## Requirements Coverage

This implementation satisfies the following requirements from the spec:

### ✓ Requirement 1.4: Household Member Management
- Only household members can access household data
- Members can be added and removed
- Access is immediately revoked when removed

### ✓ Requirement 2.1: Expense Cloud Storage (Create)
- Only household members can create expenses
- Expenses are stored under household collection
- Creator is tracked in createdBy field

### ✓ Requirement 2.2: Expense Cloud Storage (Update)
- Only household members can update expenses
- Modifier is tracked in modifiedBy field
- Immutable fields (createdBy, createdAt) are protected

### ✓ Requirement 2.3: Expense Cloud Storage (Delete)
- Only household members can delete expenses
- Soft delete is supported via isDeleted flag

### ✓ Requirement 7.4: Member-Only Access
- Non-members cannot access household data
- Non-members cannot access expenses
- Non-members cannot access categories
- Access is verified on every operation

## Architecture

### Collection Structure

```
firestore/
├── households/
│   └── {householdId}/
│       ├── name
│       ├── createdBy
│       ├── createdAt
│       ├── memberIds[]
│       ├── inviteCode
│       └── members/
│           └── {userId}/
│               ├── userId
│               ├── email
│               ├── displayName
│               └── joinedAt
│
├── expenses/
│   └── {householdId}/
│       └── expenses/
│           └── {expenseId}/
│               ├── id
│               ├── householdId
│               ├── amount
│               ├── description
│               ├── categoryId
│               ├── date
│               ├── createdBy
│               ├── createdAt
│               ├── modifiedBy
│               ├── modifiedAt
│               └── isDeleted
│
└── categories/
    └── {householdId}/
        └── categories/
            └── {categoryId}/
                ├── id
                ├── householdId
                ├── name
                ├── icon
                ├── createdBy
                ├── createdAt
                ├── modifiedAt
                └── isDeleted
```

### Security Rule Flow

```
Request
  │
  ├─> Is user authenticated?
  │   ├─> No ──> DENY
  │   └─> Yes
  │       │
  │       ├─> Is user a household member?
  │       │   ├─> No ──> DENY
  │       │   └─> Yes
  │       │       │
  │       │       ├─> Is data valid?
  │       │       │   ├─> No ──> DENY
  │       │       │   └─> Yes ──> ALLOW
```

## Testing

### Manual Testing Scenarios

See `FIRESTORE_SECURITY_RULES.md` for detailed test scenarios:

1. **Household Access Permissions**
   - Member can read/write
   - Non-member cannot access

2. **Expense CRUD Permissions**
   - Member can create/read/update/delete
   - Non-member cannot perform any operation

3. **Category Sync Permissions**
   - Member can create/read/update/delete
   - Non-member cannot access

4. **Member Removal Access Revocation**
   - Removed member loses access immediately

### Automated Testing

Run the test script:

```bash
# Set up service account credentials
export GOOGLE_APPLICATION_CREDENTIALS="path/to/service-account-key.json"

# Run tests
npm run test:rules
```

### Emulator Testing

Test locally without affecting production:

```bash
# Start emulator
npm run emulator:start

# Access UI at http://localhost:4000
# Test operations in Firestore tab
```

## Deployment

### Development Environment

```bash
# Deploy to development project
firebase use development
npm run deploy:rules
```

### Production Environment

```bash
# Deploy to production project
firebase use production
npm run deploy:rules
```

### Verification

After deployment:

1. Check Firebase Console → Firestore Database → Rules
2. Verify "Published" timestamp
3. Run test scenarios
4. Monitor for denied requests

## Monitoring

### Firebase Console

1. Go to Firestore Database → Usage
2. Review "Security Rules Evaluations"
3. Check for denied requests
4. Investigate patterns

### Metrics to Track

- Total rule evaluations
- Denied request count
- Denied request rate
- Average evaluation time

### Alerts

Set up alerts for:
- High denied request rate
- Unusual access patterns
- Failed authentication attempts

## Troubleshooting

### Common Issues

**"Missing or insufficient permissions"**
- Verify user is authenticated
- Check user is in household memberIds
- Confirm householdId is correct

**"Invalid argument"**
- Check all required fields are present
- Verify data types match validation rules
- Ensure field constraints are met

**Rules not updating**
- Wait a few minutes for propagation
- Clear browser cache
- Redeploy rules

### Debug Mode

Enable Firestore debug logging in Android app:

```kotlin
FirebaseFirestore.setLoggingEnabled(true)
```

Check Logcat for detailed rule evaluation logs.

## Security Best Practices

1. ✓ **Never disable security rules** in production
2. ✓ **Always validate data** on write operations
3. ✓ **Use helper functions** to keep rules DRY
4. ✓ **Test thoroughly** before deploying
5. ✓ **Monitor access patterns** regularly
6. ✓ **Audit rules quarterly** for improvements
7. ✓ **Document changes** in version control
8. ✓ **Maintain rollback capability**

## Maintenance

### Regular Tasks

- **Weekly**: Review denied requests
- **Monthly**: Analyze access patterns
- **Quarterly**: Audit and update rules
- **Annually**: Security review

### Updates

When updating rules:

1. Test changes in emulator
2. Deploy to development environment
3. Run full test suite
4. Monitor for issues
5. Deploy to production
6. Verify and monitor

## Support

### Documentation

- `FIRESTORE_SECURITY_RULES.md` - Detailed documentation
- `DEPLOY_FIRESTORE_RULES.md` - Deployment guide
- `SECURITY_RULES_REFERENCE.md` - Quick reference

### External Resources

- [Firebase Security Rules Docs](https://firebase.google.com/docs/firestore/security/get-started)
- [Rules Language Reference](https://firebase.google.com/docs/rules/rules-language)
- [Best Practices](https://firebase.google.com/docs/firestore/security/rules-conditions)

### Getting Help

- Check Firebase Console for error details
- Review Logcat for client-side errors
- Consult Firebase documentation
- Contact Firebase support

## Changelog

### Version 1.0.0 (2024-11-22)

**Initial Implementation**
- Household-based access control
- Expense CRUD permissions
- Category sync permissions
- Data validation rules
- Member management
- Audit trail tracking

**Requirements Satisfied**
- Requirement 1.4: Household member management
- Requirement 2.1: Expense create permissions
- Requirement 2.2: Expense update permissions
- Requirement 2.3: Expense delete permissions
- Requirement 7.4: Member-only access

**Files Created**
- firestore.rules
- firestore.indexes.json
- firebase.json
- Documentation files
- Test scripts

## License

[Your License Here]

## Contributors

[Your Team/Contributors Here]
