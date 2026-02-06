# Household Setup User Guide

## Overview

The Expense Tracker app allows you to share expenses with family members or roommates through households. This guide will walk you through setting up and managing your household.

## Table of Contents

1. [Creating Your First Household](#creating-your-first-household)
2. [Joining an Existing Household](#joining-an-existing-household)
3. [Inviting Members](#inviting-members)
4. [Managing Your Household](#managing-your-household)
5. [Understanding Sync](#understanding-sync)
6. [Troubleshooting](#troubleshooting)

---

## Creating Your First Household

### Step 1: Sign In

1. Open the Expense Tracker app
2. Tap **"Sign in with Google"**
3. Select your Google account
4. Grant necessary permissions

### Step 2: Create Household

After signing in for the first time, you'll see the household setup screen:

1. Tap **"Create Household"**
2. Enter a household name (examples: "Smith Family", "Apartment 4B", "College Roommates")
3. Tap **"Create"**

### Step 3: Data Migration

If you have existing expenses:

1. A migration dialog will appear
2. The app will upload all your local expenses to the cloud
3. Wait for migration to complete (usually takes a few seconds)
4. You can continue using the app during migration

### Step 4: Get Your Invite Code

After creation:

1. Your household is created with a unique invite code
2. Find the invite code in **Settings → Household**
3. Share this code with family members you want to invite

---

## Joining an Existing Household

### Prerequisites

- You need an invite code from a household member
- You must be signed in with your Google account

### Steps to Join

1. Open the app and sign in
2. On the household setup screen, tap **"Join Household"**
3. Enter the invite code provided by a household member
4. Tap **"Join"**
5. Wait for initial sync to complete
6. You'll now see all household expenses

### What Happens When You Join

- Your device syncs with the household's expenses
- You can see expenses added by other members
- Your existing local expenses remain on your device (not shared)
- You can add new expenses that will be visible to all household members

---

## Inviting Members

### Method 1: Share Invite Code

1. Open the app
2. Navigate to **Settings → Household**
3. Tap **"Invite Members"**
4. Choose how to share:
   - **Copy to Clipboard**: Paste in any messaging app
   - **Share**: Use Android's share menu to send via SMS, email, WhatsApp, etc.

### Method 2: Show Code in Person

1. Go to **Settings → Household**
2. Show the invite code on your screen
3. The other person can manually type it when joining

### Invite Code Format

- Invite codes are 8 characters long
- Mix of uppercase letters and numbers
- Example: `ABC12XYZ`
- Case-insensitive when entering

---

## Managing Your Household

### Viewing Household Information

Navigate to **Settings → Household** to see:

- **Household Name**: The name you chose when creating
- **Members**: List of all household members
- **Invite Code**: Code for inviting new members
- **Last Sync**: When your device last synced with the cloud

### Viewing Members

In the Household screen, you can see:

- Member names (from their Google accounts)
- Member email addresses
- When each member last synced
- Total number of members

### Leaving a Household

If you need to leave a household:

1. Go to **Settings → Household**
2. Scroll to the bottom
3. Tap **"Leave Household"**
4. Confirm your decision

**Important**: 
- Leaving a household removes your access to shared expenses
- Your local copy of expenses remains on your device
- You can create a new household or join a different one

### Removing Members (Creator Only)

If you created the household:

1. Go to **Settings → Household**
2. Find the member you want to remove
3. Tap the remove icon next to their name
4. Confirm removal

**Note**: Removed members lose access to household expenses immediately.

---

## Understanding Sync

### How Sync Works

The app uses real-time synchronization:

- **Automatic**: Changes sync automatically when you have internet
- **Real-time**: Other members see your changes within seconds
- **Bidirectional**: Changes flow both ways (your device ↔ cloud ↔ other devices)
- **Offline-capable**: Works without internet, syncs when reconnected

### Sync Status Indicators

Each expense shows its sync status:

| Icon | Status | Meaning |
|------|--------|---------|
| ✅ | Synced | Expense is saved to the cloud |
| 🔄 | Syncing | Currently uploading/downloading |
| ☁️ | Offline | No internet connection |
| ⚠️ | Error | Sync failed (will retry automatically) |

### Manual Sync

To manually trigger a sync:

1. Go to the expense list
2. Pull down from the top of the screen
3. Release to refresh
4. Wait for sync to complete

### Sync Frequency

- Changes sync within 500 milliseconds of being made
- Real-time listeners provide instant updates from other users
- Offline changes sync immediately when connection is restored

---

## Troubleshooting

### Problem: Can't Join Household

**Possible Causes:**
- Invalid invite code
- No internet connection
- Household no longer exists

**Solutions:**
1. Double-check the invite code (case doesn't matter)
2. Ensure you have internet connection
3. Ask the household creator for a new invite code
4. Try signing out and signing back in

### Problem: Expenses Not Syncing

**Possible Causes:**
- No internet connection
- Firestore quota exceeded (rare)
- Authentication expired

**Solutions:**
1. Check your internet connection
2. Pull down to manually refresh
3. Go to **Settings → Household** to check sync status
4. Try signing out and signing back in
5. Restart the app

### Problem: Don't See Other Members' Expenses

**Possible Causes:**
- Not connected to internet
- Other members haven't added expenses yet
- Sync hasn't completed

**Solutions:**
1. Pull down to refresh the expense list
2. Check sync status in Settings
3. Ensure you're in the same household (check household name)
4. Wait a few seconds for initial sync

### Problem: Duplicate Expenses

**Possible Causes:**
- Sync conflict (rare)
- Added expense while offline, then synced

**Solutions:**
1. Delete the duplicate expense
2. The app should prevent duplicates automatically
3. If it persists, report the issue

### Problem: Migration Taking Too Long

**Possible Causes:**
- Large number of expenses
- Slow internet connection
- Firestore rate limits

**Solutions:**
1. Wait patiently (can take 1-2 minutes for 1000+ expenses)
2. Ensure stable internet connection
3. Don't close the app during migration
4. If it fails, it will retry automatically

### Problem: Invite Code Not Working

**Possible Causes:**
- Code was typed incorrectly
- Household was deleted
- Code expired (doesn't happen, but check with creator)

**Solutions:**
1. Verify the code character by character
2. Ask the household creator to check if household still exists
3. Request a new invite code
4. Try copying and pasting instead of typing

---

## Best Practices

### For Household Creators

1. **Choose a Clear Name**: Use a name everyone will recognize
2. **Share Code Securely**: Only share with trusted family/friends
3. **Verify Members**: Check the member list periodically
4. **Communicate**: Let members know when you remove someone

### For All Members

1. **Stay Connected**: Keep internet on for real-time sync
2. **Check Sync Status**: Verify expenses are synced before closing app
3. **Use Categories Consistently**: Agree on category usage with household
4. **Add Descriptions**: Help others understand expenses
5. **Don't Delete Others' Expenses**: Unless agreed upon

### For Offline Use

1. **Expect Delays**: Offline changes sync when you reconnect
2. **Avoid Conflicts**: Don't edit the same expense on multiple devices while offline
3. **Check After Reconnecting**: Pull to refresh after going back online
4. **Be Patient**: Large offline queues may take time to sync

---

## Privacy & Security

### Data Protection

- All data is encrypted in transit (HTTPS)
- Firestore security rules enforce household-based access
- Only household members can see household expenses
- Authentication required for all operations

### What's Shared

When you join a household, members can see:
- Your name (from Google account)
- Your email address
- Expenses you add to the household
- When you last synced

### What's NOT Shared

- Your password (handled by Google)
- Expenses from other households
- Your personal device information
- Your location

### Leaving Safely

When you leave a household:
- Your access is immediately revoked
- Other members can still see expenses you added
- You keep a local copy of expenses on your device
- You can delete your local copy if desired

---

## FAQ

### Can I be in multiple households?

Currently, you can only be in one household at a time. To switch households, you must leave your current one and join or create another.

### What happens if I delete the app?

- Your household membership remains active
- When you reinstall and sign in, you'll rejoin your household
- All expenses will sync back to your device

### Can I rename my household?

Currently, household names cannot be changed after creation. You would need to create a new household and invite members again.

### Is there a limit to household members?

There's no hard limit, but performance is optimized for typical family sizes (2-10 members).

### Do I need internet all the time?

No! The app works offline. Changes sync automatically when you reconnect.

### What if two people edit the same expense?

The app uses "last-write-wins" conflict resolution. The most recent change is kept automatically.

### Can I export household expenses?

Yes! Use the CSV export feature in the app to export all expenses, including those from household members.

---

## Support

If you encounter issues not covered in this guide:

1. Check the app's Settings for sync status
2. Try signing out and back in
3. Restart the app
4. Check your internet connection
5. Ensure you have the latest app version

For technical issues, refer to the main [README.md](README.md) or contact support.

---

## Quick Reference

### Common Actions

| Action | Steps |
|--------|-------|
| Create Household | Sign in → Create Household → Enter name |
| Join Household | Sign in → Join Household → Enter code |
| Invite Member | Settings → Household → Invite Members |
| View Members | Settings → Household |
| Leave Household | Settings → Household → Leave Household |
| Manual Sync | Pull down on expense list |
| Check Sync Status | Settings → Household → Last Sync |

### Sync Status Icons

| Icon | Meaning |
|------|---------|
| ✅ | Synced successfully |
| 🔄 | Currently syncing |
| ☁️ | Offline mode |
| ⚠️ | Sync error |

---

*Last Updated: November 2025*
