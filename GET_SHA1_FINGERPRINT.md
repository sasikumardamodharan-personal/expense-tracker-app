# Get SHA-1 Fingerprint for Physical Device

## The Issue
Google Sign-In works on emulators but fails on physical devices with "sign in cancelled" because the SHA-1 fingerprint isn't registered.

## Quick Solution - Get SHA-1 from Android Studio

### Method 1: Using Android Studio Gradle Panel (Easiest)

1. **Open Android Studio**
2. **Open your project** (expense-tracker-app)
3. **Find Gradle panel** on the right side
4. **Navigate to:** 
   ```
   expense-tracker-app
   └── app
       └── Tasks
           └── android
               └── signingReport
   ```
5. **Double-click** `signingReport`
6. **Check the Run tab** at the bottom
7. **Look for** the debug SHA-1 fingerprint:
   ```
   Variant: debug
   Config: debug
   Store: C:\Users\YourName\.android\debug.keystore
   Alias: AndroidDebugKey
   MD5: ...
   SHA1: A1:B2:C3:D4:E5:F6:... (COPY THIS!)
   SHA-256: ...
   ```

### Method 2: Using Command Line (If you have Java installed)

Open Command Prompt and run:

```cmd
"%JAVA_HOME%\bin\keytool" -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

If JAVA_HOME is not set, try finding keytool in:
```cmd
"C:\Program Files\Android\Android Studio\jbr\bin\keytool" -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

### Method 3: Get from Google Play Console (For Release)

If you're testing with a release build:
1. Go to Google Play Console
2. Select your app
3. Go to Release → Setup → App Integrity
4. Copy the SHA-1 certificate fingerprint

---

## Add SHA-1 to Firebase Console

Once you have the SHA-1 fingerprint:

1. **Go to Firebase Console**: https://console.firebase.google.com/
2. **Select your project**: expense-tracker-22393
3. **Click the gear icon** ⚙️ → **Project Settings**
4. **Scroll down** to "Your apps" section
5. **Find your Android app**: com.expensetracker.app
6. **Click "Add fingerprint"**
7. **Paste your SHA-1** fingerprint
8. **Click "Save"**

### Important Notes:
- You may need to add BOTH debug and release SHA-1 fingerprints
- Debug SHA-1 is for development/testing
- Release SHA-1 is for production builds
- Each computer/developer needs their own debug SHA-1

---

## After Adding SHA-1

1. **Wait 5 minutes** for Firebase to propagate changes
2. **Download updated google-services.json** (optional, but recommended):
   - Go to Project Settings
   - Scroll to Your apps
   - Click google-services.json download button
   - Replace the file in `app/google-services.json`
3. **Rebuild your app**
4. **Uninstall the old app** from your device
5. **Install and test** the new build

---

## Testing Checklist

After adding SHA-1:

- [ ] SHA-1 added to Firebase Console
- [ ] Waited 5 minutes
- [ ] Rebuilt the app
- [ ] Uninstalled old app from device
- [ ] Installed new build
- [ ] Tested sign-in on physical device
- [ ] Sign-in successful!

---

## Common SHA-1 Values

Your debug keystore SHA-1 will look something like:
```
A1:B2:C3:D4:E5:F6:G7:H8:I9:J0:K1:L2:M3:N4:O5:P6:Q7:R8:S9:T0
```

Make sure to copy the entire fingerprint including all colons.

---

## Troubleshooting

### "keytool is not recognized"
- Java/JDK is not installed or not in PATH
- Use Android Studio method instead

### "Sign in cancelled" still appears
- Wait 5-10 minutes after adding SHA-1
- Make sure you added the correct SHA-1 (debug vs release)
- Uninstall and reinstall the app
- Clear Google Play Services cache on device

### Multiple developers
- Each developer needs to add their own debug SHA-1
- Get SHA-1 from each developer's machine
- Add all SHA-1 fingerprints to Firebase Console

---

## Quick Test on Emulator

If you want to test immediately without SHA-1 setup:

1. Use an Android emulator with Google Play Services
2. The current configuration should work on emulators
3. Physical device testing requires SHA-1

---

## Current Configuration

**Project ID**: expense-tracker-22393
**Package Name**: com.expensetracker.app
**Web Client ID**: 1081947747424-btorkl9j2510sr00cns0o4opjr6rjpue.apps.googleusercontent.com

**Status**: ✅ Configured for emulators
**Status**: ⚠️ Needs SHA-1 for physical devices
