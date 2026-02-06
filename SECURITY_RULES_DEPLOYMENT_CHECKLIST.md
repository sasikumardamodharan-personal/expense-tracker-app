# Firestore Security Rules Deployment Checklist

Use this checklist when deploying Firestore security rules to ensure all steps are completed.

## Pre-Deployment

### Environment Setup
- [ ] Firebase CLI installed (`npm install -g firebase-tools`)
- [ ] Logged into Firebase (`firebase login`)
- [ ] Correct Firebase project selected (`firebase use <project-id>`)
- [ ] Dependencies installed (`npm install`)

### Code Review
- [ ] Security rules reviewed by team
- [ ] All helper functions tested
- [ ] Data validation rules verified
- [ ] No hardcoded values or test data
- [ ] Comments and documentation updated

### Testing
- [ ] All test scenarios pass in emulator
- [ ] Manual testing completed
- [ ] Automated tests run successfully (`npm run test:rules`)
- [ ] Edge cases tested
- [ ] Performance tested with realistic data volume

## Deployment

### Development Environment
- [ ] Switch to development project (`firebase use development`)
- [ ] Deploy rules (`firebase deploy --only firestore:rules`)
- [ ] Verify deployment in Firebase Console
- [ ] Check "Published" timestamp
- [ ] Run smoke tests

### Staging Environment (if applicable)
- [ ] Switch to staging project (`firebase use staging`)
- [ ] Deploy rules (`firebase deploy --only firestore:rules`)
- [ ] Verify deployment in Firebase Console
- [ ] Run full test suite
- [ ] Monitor for 24 hours

### Production Environment
- [ ] Backup current rules (`firebase firestore:rules:get > firestore.rules.backup`)
- [ ] Switch to production project (`firebase use production`)
- [ ] Deploy rules (`firebase deploy --only firestore:rules`)
- [ ] Verify deployment in Firebase Console
- [ ] Check "Published" timestamp
- [ ] Run smoke tests immediately

## Post-Deployment

### Verification
- [ ] Test household creation
- [ ] Test household joining
- [ ] Test expense CRUD operations
- [ ] Test category sync
- [ ] Test member removal
- [ ] Verify non-member access is denied

### Monitoring (First 24 Hours)
- [ ] Monitor denied requests in Firebase Console
- [ ] Check for unusual patterns
- [ ] Review error logs
- [ ] Monitor app crash reports
- [ ] Check user feedback channels

### Documentation
- [ ] Update deployment log
- [ ] Document any issues encountered
- [ ] Update team on deployment status
- [ ] Add notes for future deployments

## Rollback Plan (If Needed)

### Immediate Rollback
- [ ] Identify the issue
- [ ] Restore backup rules (`firebase deploy --only firestore:rules`)
- [ ] Verify rollback successful
- [ ] Notify team
- [ ] Document the issue

### Investigation
- [ ] Review denied requests
- [ ] Check error logs
- [ ] Identify root cause
- [ ] Create fix plan
- [ ] Test fix in development

## Success Criteria

All of the following must be true:
- [ ] Rules deployed successfully
- [ ] No increase in denied requests (beyond expected)
- [ ] No user-reported access issues
- [ ] All test scenarios pass
- [ ] Monitoring shows normal patterns
- [ ] Team notified of successful deployment

## Sign-Off

**Deployed By**: ___________________  
**Date**: ___________________  
**Time**: ___________________  
**Environment**: [ ] Development [ ] Staging [ ] Production  
**Status**: [ ] Success [ ] Rollback Required  

**Notes**:
_____________________________________________
_____________________________________________
_____________________________________________

## Emergency Contacts

**Firebase Admin**: ___________________  
**Team Lead**: ___________________  
**On-Call Engineer**: ___________________  

## Quick Commands Reference

```bash
# View current project
firebase projects:list

# Switch project
firebase use <project-id>

# Deploy rules only
firebase deploy --only firestore:rules

# Deploy rules and indexes
firebase deploy --only firestore

# Get current rules
firebase firestore:rules:get

# Start emulator
firebase emulators:start

# View deployment history
firebase firestore:rules:list
```

## Troubleshooting

### Rules not updating
1. Wait 2-3 minutes for propagation
2. Clear browser cache
3. Verify deployment timestamp in console
4. Redeploy if necessary

### Denied requests increasing
1. Check Firebase Console → Usage → Security Rules Evaluations
2. Identify which rules are denying access
3. Verify user authentication status
4. Check household membership data
5. Review recent code changes

### Performance issues
1. Check query patterns
2. Verify indexes are deployed
3. Review rule complexity
4. Consider caching strategies
5. Monitor Firestore usage metrics

## Additional Resources

- [FIRESTORE_RULES_README.md](FIRESTORE_RULES_README.md)
- [DEPLOY_FIRESTORE_RULES.md](DEPLOY_FIRESTORE_RULES.md)
- [SECURITY_RULES_REFERENCE.md](SECURITY_RULES_REFERENCE.md)
- [Firebase Console](https://console.firebase.google.com/)
