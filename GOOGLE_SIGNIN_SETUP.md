# Google Sign-In Setup Guide

## Current Status
⚠️ **Action Required**: Google Sign-In needs to be configured in Firebase Console

## Issue
The `google-services.json` file is missing OAuth client configuration, which is needed for Google Sign-In to work.

## Quick Setup (Recommended for Testing)

**For immediate testing without SHA-1 setup:**

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select project: **expense-tracker-22393**
3. Go to **Authentication** → **Sign-in method**
4. Enable **Google** provider
5. Select a support email and save
6. Go to **Project Settings** → **General**
7. Under **Your apps**, find the Web App (if not exists, add one)
8. Copy the **Web Client ID** (looks like: `xxxxx.apps.googleusercontent.com`)
9. Update `app/src/main/res/values/strings.xml`:
   ```xml
   <string name="default_web_client_id">YOUR_WEB_CLIENT_ID_HERE</string>
   ```
10. Rebuild and test on emulator

This will work for emulators. For physical devices, you'll need SHA-1 (see detailed steps below).

---

## Detailed Setup Steps (For Physical Devices)

### 1. Get SHA-1 Certificate Fingerprint

#### Method 1: Using Gradle (Recommended)

Run this command from your project directory:

```cmd
cd expense-tracker-app
gradlew signingReport
```

Look for the **SHA-1** fingerprint in the output (under `debug` variant for development).

#### Method 2: Using keytool (If Gradle doesn't work)

**For Windows:**
```cmd
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

**For Mac/Linux:**
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

#### Method 3: Using Android Studio

1. Open Android Studio
2. Open your project
3. Click **Gradle** tab on the right side
4. Navigate to: **app → Tasks → android → signingReport**
5. Double-click **signingReport**
6. Check the **Run** tab at the bottom for SHA-1

#### Method 4: Get from Firebase Console (Easiest for Testing)

For development/testing, you can skip SHA-1 temporarily:
1. Go to Firebase Console → Authentication → Sign-in method
2. Enable Google Sign-In
3. Use the Web Client ID that Firebase generates
4. This will work for emulators but may not work on physical devices without SHA-1

Look for the **SHA-1** fingerprint in the output. It looks like:
```
SHA1: A1:B2:C3:D4:E5:F6:G7:H8:I9:J0:K1:L2:M3:N4:O5:P6:Q7:R8:S9:T0
```

### 2. Add SHA-1 to Firebase Console

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project: **expense-tracker-22393**
3. Click the gear icon ⚙️ → **Project Settings**
4. Scroll down to **Your apps** section
5. Find your Android app: `com.expensetracker.app`
6. Click **Add fingerprint**
7. Paste your SHA-1 fingerprint
8. Click **Save**

### 3. Enable Google Sign-In

1. In Firebase Console, go to **Authentication**
2. Click **Sign-in method** tab
3. Click **Google** provider
4. Click **Enable**
5. Select a support email
6. Click **Save**

### 4. Download Updated google-services.json

1. Go back to **Project Settings**
2. Scroll to **Your apps**
3. Click **google-services.json** download button
4. Replace the existing file at: `expense-tracker-app/app/google-services.json`

### 5. Get Web Client ID

**Option A: From Firebase Console (Easiest)**
1. Go to Firebase Console → **Project Settings**
2. Scroll to **Your apps** section
3. If you don't see a Web app, click **Add app** → **Web** (</>) and register it
4. You'll see the **Web Client ID** displayed
5. Copy it (format: `1234567890-abcdefg.apps.googleusercontent.com`)

**Option B: From google-services.json**
After downloading the updated `google-services.json`, look for the `oauth_client` array with entries. Find the one with `"client_type": 3` (Web client) and copy the `client_id` value.

**Update strings.xml:**
```xml
<string name="default_web_client_id">1234567890-abcdefg.apps.googleusercontent.com</string>
```

**Note:** The resource may auto-generate after you replace google-services.json and rebuild, but manual update is more reliable.

### 6. Rebuild the Project

After updating the files:
1. Clean the project: `./gradlew clean`
2. Rebuild: `./gradlew build`
3. Run the app

## Verification

Once configured, you should be able to:
1. Launch the app
2. See the sign-in screen
3. Tap "Sign in with Google"
4. See Google account picker
5. Sign in successfully
6. See your profile in Settings

## Troubleshooting

### "Sign in failed" error
- Verify SHA-1 is correctly added to Firebase Console
- Make sure Google Sign-In is enabled in Authentication
- Check that google-services.json is updated

### "Developer error" message
- The Web Client ID is incorrect or missing
- Download fresh google-services.json from Firebase Console

### Account picker doesn't appear
- Check device has Google Play Services
- Verify internet connection
- Try on a different device/emulator

## Current Configuration

**Project ID**: expense-tracker-22393
**Package Name**: com.expensetracker.app
**App ID**: 1:1081947747424:android:0cb4934d19cb193c0e3f05

## Next Steps

After Google Sign-In is working:
1. Test sign-in flow
2. Test sign-out flow
3. Verify user data is saved to Firestore
4. Test app restart (should auto-sign in)
