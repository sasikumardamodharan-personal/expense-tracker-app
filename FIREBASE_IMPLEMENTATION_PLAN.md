# Firebase Implementation Plan

## Overview
Migrate the Expense Tracker app to use Firebase for real-time multi-user synchronization with offline support.

**Estimated Time:** 1-2 weeks
**Complexity:** Medium-High
**Cost:** Free (Firebase Spark plan)

---

## Phase 1: Firebase Setup & Authentication (2-3 days)

### Step 1.1: Firebase Console Setup (30 minutes)
**You need to do this:**

1. **Create Firebase Project:**
   - Go to https://console.firebase.google.com/
   - Click "Add project"
   - Name: "Expense Tracker" (or your choice)
   - Disable Google Analytics (optional, not needed)
   - Click "Create project"

2. **Add Android App:**
   - Click "Add app" → Android icon
   - Package name: `com.expensetracker.app` (must match your app)
   - App nickname: "Expense Tracker Android"
   - Click "Register app"

3. **Download google-services.json:**
   - Download the `google-services.json` file
   - Place it in: `expense-tracker-app/app/` directory
   - **Important:** This file contains your Firebase config

4. **Enable Authentication:**
   - In Firebase Console → Build → Authentication
   - Click "Get started"
   - Enable "Google" sign-in method
   - Add your email as test user

5. **Enable Firestore Database:**
   - In Firebase Console → Build → Firestore Database
   - Click "Create database"
   - Start in **production mode** (we'll add rules later)
   - Choose location: `us-central` (or closest to you)

6. **Set up Security Rules:**
   ```javascript
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       // Users can only read/write their own groups
       match /groups/{groupId} {
         allow read, write: if request.auth != null && 
           request.auth.uid in resource.data.members;
       }
       
       // Expenses within a group
       match /groups/{groupId}/expenses/{expenseId} {
         allow read, write: if request.auth != null && 
           request.auth.uid in get(/databases/$(database)/documents/groups/$(groupId)).data.members;
       }
     }
   }
   ```

---

### Step 1.2: Add Firebase Dependencies (I'll do this)

**build.gradle.kts (Project level):**
```kotlin
plugins {
    id("com.google.gms.google-services") version "4.4.0" apply false
}
```

**build.gradle.kts (App level):**
```kotlin
plugins {
    id("com.google.gms.google-services")
}

dependencies {
    // Firebase BOM (Bill of Materials)
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    
    // Firebase Authentication
    implementation("com.google.firebase:firebase-auth-ktx")
    
    // Firebase Firestore
    implementation("com.google.firebase:firebase-firestore-ktx")
    
    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.7.0")
}
```

---

### Step 1.3: Implement Authentication (I'll do this)

**Files to Create:**
1. `data/auth/AuthManager.kt` - Handle sign-in/sign-out
2. `presentation/screens/SignInScreen.kt` - Sign-in UI
3. `presentation/viewmodel/AuthViewModel.kt` - Auth state management

**Features:**
- Google Sign-In button
- Sign-out functionality
- User profile display
- Auth state persistence

---

## Phase 2: Data Model Migration (2-3 days)

### Step 2.1: Firestore Data Structure

```
firestore/
├── users/
│   └── {userId}/
│       ├── email: "user@example.com"
│       ├── displayName: "John Doe"
│       ├── photoUrl: "https://..."
│       └── createdAt: timestamp
│
├── groups/
│   └── {groupId}/
│       ├── name: "Family Expenses"
│       ├── members: ["userId1", "userId2"]
│       ├── createdBy: "userId1"
│       ├── createdAt: timestamp
│       └── expenses/ (subcollection)
│           └── {expenseId}/
│               ├── amount: 50.00
│               ├── categoryId: "food"
│               ├── categoryName: "Food"
│               ├── date: timestamp
│               ├── description: "Lunch"
│               ├── userId: "userId1"
│               ├── userName: "John"
│               ├── createdAt: timestamp
│               └── updatedAt: timestamp
│
└── categories/
    └── {categoryId}/
        ├── name: "Food"
        ├── iconName: "🍔"
        ├── colorHex: "#FF6B6B"
        ├── isCustom: false
        └── sortOrder: 1
```

---

### Step 2.2: Create Firebase Models (I'll do this)

**Files to Create:**
1. `data/firebase/models/FirebaseExpense.kt`
2. `data/firebase/models/FirebaseGroup.kt`
3. `data/firebase/models/FirebaseUser.kt`
4. `data/firebase/models/FirebaseCategory.kt`

**Example:**
```kotlin
data class FirebaseExpense(
    val id: String = "",
    val amount: Double = 0.0,
    val categoryId: String = "",
    val categoryName: String = "",
    val date: Timestamp = Timestamp.now(),
    val description: String = "",
    val userId: String = "",
    val userName: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)
```

---

### Step 2.3: Create Firebase Repository (I'll do this)

**Files to Create:**
1. `data/firebase/FirebaseExpenseRepository.kt`
2. `data/firebase/FirebaseGroupRepository.kt`
3. `data/firebase/FirebaseCategoryRepository.kt`

**Features:**
- Real-time listeners for data changes
- Offline persistence
- Conflict resolution
- Error handling

---

## Phase 3: Sync Logic Implementation (3-4 days)

### Step 3.1: Hybrid Storage Strategy

**Approach:** Keep both local Room database AND Firebase
- **Local Room:** Fast access, offline support
- **Firebase:** Sync, multi-user, backup

**Sync Flow:**
```
User adds expense
    ↓
Save to Room (immediate)
    ↓
Upload to Firebase (background)
    ↓
Firebase notifies other users
    ↓
Other users download to Room
    ↓
UI updates automatically
```

---

### Step 3.2: Sync Manager (I'll do this)

**File to Create:**
`data/sync/SyncManager.kt`

**Features:**
- Listen to Firebase changes
- Update local database
- Handle conflicts (last-write-wins)
- Queue offline changes
- Sync when online

**Conflict Resolution:**
```kotlin
// Strategy: Last write wins (based on updatedAt timestamp)
if (firebaseExpense.updatedAt > localExpense.updatedAt) {
    // Firebase is newer, update local
    updateLocal(firebaseExpense)
} else {
    // Local is newer, upload to Firebase
    uploadToFirebase(localExpense)
}
```

---

### Step 3.3: Offline Support (I'll do this)

**Features:**
- Enable Firestore offline persistence
- Queue writes when offline
- Sync automatically when online
- Show sync status to user

**Implementation:**
```kotlin
// Enable offline persistence
val settings = firestoreSettings {
    isPersistenceEnabled = true
    cacheSizeBytes = FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED
}
firestore.firestoreSettings = settings
```

---

## Phase 4: Group Management (2-3 days)

### Step 4.1: Create/Join Groups (I'll do this)

**Files to Create:**
1. `presentation/screens/GroupManagementScreen.kt`
2. `presentation/screens/CreateGroupScreen.kt`
3. `presentation/screens/JoinGroupScreen.kt`
4. `presentation/viewmodel/GroupViewModel.kt`

**Features:**
- Create new expense group
- Generate invite code
- Join group with code
- View group members
- Leave group

---

### Step 4.2: Group UI Flow

**User Flow:**
```
1. Sign in with Google
2. See "Create Group" or "Join Group"
3. Create Group:
   - Enter group name
   - Get invite code
   - Share code with family
4. Join Group:
   - Enter invite code
   - Join existing group
5. View expenses from all group members
```

---

## Phase 5: UI Updates (2-3 days)

### Step 5.1: Update Expense List (I'll do this)

**Changes:**
- Show who added each expense
- Show user avatars
- Show sync status indicator
- Show offline indicator

**UI Elements:**
```
┌─────────────────────────────────┐
│ 🍔 Food - $50.00                │
│ Added by John                   │
│ 2 hours ago                     │
│ [Synced ✓]                      │
└─────────────────────────────────┘
```

---

### Step 5.2: Add Settings Options (I'll do this)

**New Settings:**
- Current group name
- Group members list
- Switch groups
- Leave group
- Sign out

---

### Step 5.3: Add Sync Status (I'll do this)

**Features:**
- Sync indicator in toolbar
- "Syncing..." when uploading
- "Offline" when no internet
- "Synced" when up-to-date
- Pull-to-refresh

---

## Phase 6: Migration & Testing (2-3 days)

### Step 6.1: Data Migration (I'll do this)

**Feature:** Migrate existing local data to Firebase

**Flow:**
```
1. User signs in for first time
2. App detects local expenses
3. Show "Upload existing data?" dialog
4. If yes:
   - Create default group
   - Upload all expenses to Firebase
   - Mark as migrated
5. Continue with Firebase sync
```

---

### Step 6.2: Testing Checklist

**Single User:**
- [ ] Sign in with Google
- [ ] Create group
- [ ] Add expense → syncs to Firebase
- [ ] Edit expense → syncs changes
- [ ] Delete expense → syncs deletion
- [ ] Works offline
- [ ] Syncs when back online

**Multi-User:**
- [ ] User A creates group
- [ ] User B joins with code
- [ ] User A adds expense → User B sees it
- [ ] User B adds expense → User A sees it
- [ ] Both edit different expenses → no conflicts
- [ ] Both edit same expense → last write wins
- [ ] One user offline → syncs when online

**Edge Cases:**
- [ ] No internet on first launch
- [ ] Sign out and sign in again
- [ ] Multiple groups
- [ ] Leave group
- [ ] Delete account

---

## Implementation Timeline

### Week 1:
- **Day 1-2:** Firebase setup + Authentication
- **Day 3-4:** Data model migration
- **Day 5:** Sync logic basics

### Week 2:
- **Day 1-2:** Group management
- **Day 3-4:** UI updates
- **Day 5:** Testing & bug fixes

---

## Files to Create (Summary)

### Authentication (5 files):
1. `data/auth/AuthManager.kt`
2. `data/auth/AuthRepository.kt`
3. `presentation/screens/SignInScreen.kt`
4. `presentation/viewmodel/AuthViewModel.kt`
5. `domain/model/User.kt`

### Firebase Data (8 files):
1. `data/firebase/models/FirebaseExpense.kt`
2. `data/firebase/models/FirebaseGroup.kt`
3. `data/firebase/models/FirebaseUser.kt`
4. `data/firebase/models/FirebaseCategory.kt`
5. `data/firebase/FirebaseExpenseRepository.kt`
6. `data/firebase/FirebaseGroupRepository.kt`
7. `data/firebase/FirebaseCategoryRepository.kt`
8. `data/firebase/FirebaseConfig.kt`

### Sync Logic (3 files):
1. `data/sync/SyncManager.kt`
2. `data/sync/SyncWorker.kt`
3. `data/sync/ConflictResolver.kt`

### Group Management (4 files):
1. `presentation/screens/GroupManagementScreen.kt`
2. `presentation/screens/CreateGroupScreen.kt`
3. `presentation/screens/JoinGroupScreen.kt`
4. `presentation/viewmodel/GroupViewModel.kt`

### UI Updates (3 files):
1. `presentation/components/SyncStatusIndicator.kt`
2. `presentation/components/UserAvatar.kt`
3. `presentation/screens/MigrationScreen.kt`

**Total: ~23 new files + modifications to existing files**

---

## Dependencies to Add

```kotlin
// Firebase
implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
implementation("com.google.firebase:firebase-auth-ktx")
implementation("com.google.firebase:firebase-firestore-ktx")

// Google Sign-In
implementation("com.google.android.gms:play-services-auth:20.7.0")

// WorkManager (for background sync)
implementation("androidx.work:work-runtime-ktx:2.9.0")

// Coil (for user avatars)
implementation("io.coil-kt:coil-compose:2.5.0")
```

---

## Security Considerations

### Firestore Rules:
```javascript
// Only authenticated users
// Users can only access groups they're members of
// Users can only modify their own expenses
```

### Data Privacy:
- User emails stored in Firebase
- Expense data visible to group members
- No end-to-end encryption (data readable by Firebase)
- Can add encryption layer if needed

---

## Cost Analysis

### Firebase Free Tier (Spark Plan):
- **Firestore:** 1GB storage, 50K reads/day, 20K writes/day
- **Authentication:** Unlimited
- **Hosting:** 10GB storage, 360MB/day transfer

### Typical Usage (Family of 4):
- **Storage:** ~10MB per year
- **Reads:** ~1000 per day (well under limit)
- **Writes:** ~100 per day (well under limit)

**Conclusion:** Will stay free indefinitely for personal use! 🎉

---

## Migration Strategy

### Option A: Fresh Start (Recommended)
- Start with Firebase from day 1
- Keep local data as backup
- Users can export old data if needed

### Option B: Gradual Migration
- Keep Room as primary
- Add Firebase as sync layer
- Migrate over time

**Recommendation:** Option A (cleaner, simpler)

---

## Next Steps

### What You Need to Do:
1. ✅ Create Firebase project (30 minutes)
2. ✅ Download google-services.json
3. ✅ Enable Authentication & Firestore
4. ✅ Set up security rules

### What I'll Do:
1. Add Firebase dependencies
2. Implement authentication
3. Create Firebase repositories
4. Build sync logic
5. Add group management
6. Update UI
7. Test everything

---

## Questions Before We Start?

1. **Group Setup:** Do you want users to create their own groups, or should I create a default "Family" group?

2. **Existing Data:** Should we migrate existing local expenses to Firebase, or start fresh?

3. **User Names:** Use Google account name, or let users set custom display names?

4. **Invite System:** Invite codes (simple) or email invites (more complex)?

5. **Offline Behavior:** Show "offline" indicator, or hide it and sync silently?

Let me know your preferences and I'll start implementing! 🚀
