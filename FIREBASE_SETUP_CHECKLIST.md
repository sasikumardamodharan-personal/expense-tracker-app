# Firebase Setup Checklist

## ✅ Step 1: Project-Level build.gradle.kts - DONE
Added Google services plugin to root build.gradle.kts

## ✅ Step 2: App-Level build.gradle.kts - DONE
Added:
- Google services plugin
- Firebase BOM (34.6.0)
- Firebase Authentication
- Firebase Firestore
- Google Sign-In
- WorkManager
- Coil (for avatars)

## ⏳ Step 3: Add google-services.json - YOU NEED TO DO THIS

### Instructions:
1. You should have downloaded `google-services.json` from Firebase Console
2. Copy the file to: `expense-tracker-app/app/google-services.json`
3. The file should be in the same directory as `build.gradle.kts`

### File Location:
```
expense-tracker-app/
├── app/
│   ├── build.gradle.kts
│   ├── google-services.json  ← PUT IT HERE
│   └── src/
```

### Verify:
After placing the file, you should see it in the `app/` directory alongside `build.gradle.kts`

---

## Next Steps After Adding google-services.json:

### 1. Sync Gradle
- Open Android Studio
- Click "Sync Now" or "Sync Project with Gradle Files"
- Wait for sync to complete
- Check for any errors

### 2. Verify Firebase Setup
Run this command to verify:
```bash
cd expense-tracker-app
./gradlew :app:dependencies | grep firebase
```

You should see Firebase dependencies listed.

### 3. Test Build
```bash
./gradlew :app:assembleDebug
```

Should build successfully without errors.

---

## Common Issues & Solutions

### Issue 1: "google-services.json not found"
**Solution:** Make sure the file is in `app/` directory, not in `app/src/`

### Issue 2: "Package name mismatch"
**Solution:** Verify package name in google-services.json matches:
```json
{
  "client": [{
    "client_info": {
      "android_client_info": {
        "package_name": "com.expensetracker.app"  ← Must match
      }
    }
  }]
}
```

### Issue 3: "Plugin not found"
**Solution:** Make sure you synced Gradle after adding the plugin

### Issue 4: Build fails with Firebase errors
**Solution:** 
1. Clean build: `./gradlew clean`
2. Sync Gradle again
3. Rebuild: `./gradlew :app:assembleDebug`

---

## What's Next?

Once google-services.json is added and Gradle syncs successfully, I'll implement:

### Phase 1: Authentication (2-3 days)
1. ✅ Firebase setup - DONE
2. ⏳ Google Sign-In screen
3. ⏳ Authentication manager
4. ⏳ User profile storage

### Phase 2: Firestore Integration (2-3 days)
1. ⏳ Firebase data models
2. ⏳ Firestore repositories
3. ⏳ Real-time listeners

### Phase 3: Sync Logic (3-4 days)
1. ⏳ Sync manager
2. ⏳ Conflict resolution
3. ⏳ Offline support

### Phase 4: Group Management (2-3 days)
1. ⏳ Create/join groups
2. ⏳ Invite system
3. ⏳ Member management

### Phase 5: UI Updates (2-3 days)
1. ⏳ Show user info on expenses
2. ⏳ Sync status indicators
3. ⏳ Group settings

---

## Current Status

- ✅ Firebase dependencies added
- ✅ Google services plugin configured
- ⏳ Waiting for google-services.json file
- ⏳ Ready to start implementation

**Action Required:** Place google-services.json in `app/` directory and sync Gradle!

Once that's done, let me know and I'll start implementing the authentication system! 🚀
