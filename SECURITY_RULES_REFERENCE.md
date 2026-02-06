# Firestore Security Rules Quick Reference

## Access Control Matrix

| Resource | Read | Create | Update | Delete | Conditions |
|----------|------|--------|--------|--------|------------|
| **Households** | ✓ | ✓ | ✓ | ✗ | Must be authenticated member |
| **Household Members** | ✓ | ✓ | ✓ | ✓ | Must be household member |
| **Expenses** | ✓ | ✓ | ✓ | ✓ | Must be household member |
| **Categories** | ✓ | ✓ | ✓ | ✓ | Must be household member |

## Key Security Principles

### 1. Authentication Required
All operations require user authentication (`request.auth != null`)

### 2. Household-Based Access
Access is controlled by household membership (`request.auth.uid in memberIds`)

### 3. Data Validation
All writes are validated for:
- Required fields presence
- Correct data types
- Field constraints (length, range)
- Immutable fields (createdBy, createdAt)

### 4. Audit Trail
All modifications must include:
- `modifiedBy`: User ID of modifier
- `modifiedAt`: Timestamp of modification

## Collection Paths

```
/households/{householdId}
/households/{householdId}/members/{userId}
/expenses/{householdId}/expenses/{expenseId}
/categories/{householdId}/categories/{categoryId}
```

## Helper Functions

### `isAuthenticated()`
Checks if user is logged in

### `isMemberOfHousehold(householdId)`
Checks if authenticated user is a member of the specified household

### `isValidHousehold()`
Validates household data structure and constraints

### `isValidExpense()`
Validates expense data structure and constraints

### `isValidCategory()`
Validates category data structure and constraints

## Data Validation Rules

### Household
- `name`: String, 1-100 characters
- `createdBy`: String (user ID)
- `createdAt`: Timestamp
- `memberIds`: Array of strings
- `inviteCode`: String

### Expense
- `id`: String
- `householdId`: String (must match collection path)
- `amount`: Number (>= 0)
- `description`: String (max 500 characters)
- `categoryId`: Integer
- `date`: Timestamp
- `createdBy`: String (immutable)
- `createdAt`: Timestamp (immutable)
- `modifiedBy`: String
- `modifiedAt`: Timestamp
- `isDeleted`: Boolean

### Category
- `id`: String
- `householdId`: String (must match collection path)
- `name`: String, 1-50 characters
- `icon`: String
- `createdBy`: String (immutable)
- `createdAt`: Timestamp (immutable)
- `modifiedAt`: Timestamp
- `isDeleted`: Boolean

## Common Operations

### Create Household
```javascript
// Allowed if:
// - User is authenticated
// - User is the creator
// - User is in memberIds
// - Data is valid
```

### Join Household
```javascript
// Update household to add user to memberIds
// Allowed if:
// - User is authenticated
// - User is already a member (to add others)
```

### Create Expense
```javascript
// Allowed if:
// - User is household member
// - householdId matches collection path
// - createdBy is current user
// - Data is valid
```

### Update Expense
```javascript
// Allowed if:
// - User is household member
// - householdId matches collection path
// - modifiedBy is current user
// - createdBy and createdAt unchanged
// - Data is valid
```

### Delete Expense
```javascript
// Allowed if:
// - User is household member
```

## Security Best Practices

1. ✓ Always authenticate users before operations
2. ✓ Validate all input data
3. ✓ Use helper functions for DRY code
4. ✓ Maintain audit trail (createdBy, modifiedBy)
5. ✓ Prevent modification of immutable fields
6. ✓ Use householdId to scope all data
7. ✓ Test rules thoroughly before production
8. ✗ Never disable security rules
9. ✗ Never trust client-side validation alone
10. ✗ Never expose sensitive data in rules

## Testing Checklist

- [ ] Authenticated user can create household
- [ ] Household member can read household data
- [ ] Non-member cannot read household data
- [ ] Household member can create expense
- [ ] Non-member cannot create expense
- [ ] Household member can update expense
- [ ] Non-member cannot update expense
- [ ] Household member can delete expense
- [ ] Non-member cannot delete expense
- [ ] Removed member loses access immediately
- [ ] Data validation rejects invalid data
- [ ] Immutable fields cannot be changed

## Error Messages

| Error | Cause | Solution |
|-------|-------|----------|
| `PERMISSION_DENIED` | User not authenticated or not a member | Verify authentication and household membership |
| `INVALID_ARGUMENT` | Data validation failed | Check required fields and data types |
| `NOT_FOUND` | Document doesn't exist | Verify document path and existence |
| `FAILED_PRECONDITION` | Trying to modify immutable field | Don't modify createdBy or createdAt |

## Monitoring

### Key Metrics to Track
- Security rule evaluation count
- Denied request count
- Denied request patterns
- Average evaluation time

### Where to Monitor
Firebase Console → Firestore Database → Usage → Security Rules Evaluations

## Quick Deploy

```bash
# Deploy rules only
firebase deploy --only firestore:rules

# Deploy rules and indexes
firebase deploy --only firestore

# Test locally with emulator
firebase emulators:start
```

## Emergency Procedures

### If Rules Are Too Restrictive
1. Review denied requests in Firebase Console
2. Identify the failing rule
3. Update rule to allow legitimate access
4. Test with emulator
5. Deploy updated rules

### If Rules Are Too Permissive
1. Immediately deploy more restrictive rules
2. Audit recent access logs
3. Notify security team
4. Review and update rules
5. Test thoroughly before redeploying

## Support Resources

- [Firebase Security Rules Docs](https://firebase.google.com/docs/firestore/security/get-started)
- [Rules Playground](https://console.firebase.google.com/)
- [Emulator Suite](https://firebase.google.com/docs/emulator-suite)
- [Best Practices](https://firebase.google.com/docs/firestore/security/rules-conditions)
