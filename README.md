# Expense Tracker Android App

A modern Android expense tracking application built with Kotlin and Jetpack Compose.

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material Design 3
- **Architecture**: MVVM with Clean Architecture
- **Database**: Room (SQLite)
- **Dependency Injection**: Hilt
- **Asynchronous**: Coroutines + Flow
- **Navigation**: Compose Navigation

## Project Structure

```
app/
├── data/
│   ├── local/          # Room database entities and DAOs
│   └── repository/     # Repository implementations
├── domain/
│   ├── model/          # Domain models
│   └── usecase/        # Business logic use cases
├── presentation/
│   ├── screens/        # Composable screens
│   └── viewmodel/      # ViewModels
├── di/                 # Hilt dependency injection modules
└── ui/
    └── theme/          # Material Design 3 theme
```

## Requirements

- Android Studio Hedgehog or later
- Android SDK 34
- Minimum SDK 24 (Android 7.0)
- Kotlin 1.9.20
- Gradle 8.2.0

## Dependencies

### Core
- AndroidX Core KTX 1.12.0
- Lifecycle Runtime KTX 2.6.2
- Activity Compose 1.8.1

### Compose
- Compose BOM 2023.10.01
- Material3
- Lifecycle Compose 2.6.2

### Navigation
- Navigation Compose 2.7.5

### Room
- Room Runtime 2.6.1
- Room KTX 2.6.1

### Hilt
- Hilt Android 2.48
- Hilt Navigation Compose 1.1.0

### Coroutines
- Kotlinx Coroutines Android 1.7.3
- Kotlinx Coroutines Core 1.7.3

## Building the Project

1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle files
4. Run the app on an emulator or physical device

## Features

### Core Features
- Add, edit, and delete expenses
- Categorize expenses
- Filter expenses by date range and category
- View spending summaries and statistics
- Local data persistence with Room
- Material Design 3 theming with light/dark mode support

### Cloud Sync Features
- **Firebase Authentication**: Secure user authentication with Google Sign-In
- **Firestore Sync**: Real-time expense synchronization across devices
- **Household Management**: Create or join households to share expenses with family members
- **Multi-User Support**: Multiple users can collaborate on shared household expenses
- **Offline Support**: Work offline with automatic sync when connection is restored
- **Category Sync**: Shared categories across household members
- **Conflict Resolution**: Automatic conflict resolution with last-write-wins strategy

## Getting Started with Sync

> **📖 For a comprehensive guide, see [HOUSEHOLD_SETUP_GUIDE.md](HOUSEHOLD_SETUP_GUIDE.md)**

### First-Time Setup

When you first sign in to the app, you'll be prompted to set up a household:

1. **Create a New Household**
   - Choose "Create Household"
   - Enter a household name (e.g., "Smith Family")
   - Your household will be created with a unique invite code
   - All your existing expenses will be migrated to the cloud

2. **Join an Existing Household**
   - Choose "Join Household"
   - Enter the invite code shared by a household member
   - Your device will sync with the household's expenses

### Inviting Family Members

To invite others to your household:

1. Open the app and navigate to **Settings → Household**
2. Tap **"Invite Members"**
3. Share the invite code via messaging apps, email, or copy to clipboard
4. The invited person can use this code when they first sign in

### Understanding Sync Status

The app displays sync status indicators next to each expense:

- ✅ **Green Checkmark**: Expense is synced to the cloud
- 🔄 **Spinning Icon**: Expense is currently syncing
- ☁️ **Cloud with Slash**: You're offline (changes will sync when online)
- ⚠️ **Exclamation Mark**: Sync error occurred

### Working Offline

The app works seamlessly offline:

1. Add, edit, or delete expenses without internet connection
2. Changes are saved locally and queued for sync
3. When internet connection is restored, all changes automatically sync
4. Pull down on the expense list to manually trigger sync

### Managing Your Household

Access household management from **Settings → Household**:

- View all household members
- See when each member last synced
- Share the invite code with new members
- Leave the household (if needed)

### Data Migration

When you first enable sync, the app automatically migrates your existing expenses:

- All local expenses are uploaded to Firestore
- A progress dialog shows migration status
- Migration happens in the background
- You can continue using the app during migration

### Conflict Resolution

If two users edit the same expense simultaneously:

- The app uses a "last-write-wins" strategy
- The most recent change (by timestamp) is kept
- Conflicts are resolved automatically
- No manual intervention required

### Performance & Data Usage

The sync feature is optimized for efficiency:

- Only changed data is synced (not the entire database)
- Changes are batched to reduce network calls
- Sync operations are throttled (max once per 500ms)
- Firestore offline persistence minimizes data usage
- Real-time listeners (no polling) for instant updates

## Documentation

### For Users
- **[HOUSEHOLD_SETUP_GUIDE.md](HOUSEHOLD_SETUP_GUIDE.md)** - Complete user guide for household setup and management

### For Developers
- **[SYNC_ARCHITECTURE.md](SYNC_ARCHITECTURE.md)** - Technical documentation for sync implementation
- **[FIRESTORE_SECURITY_RULES.md](FIRESTORE_SECURITY_RULES.md)** - Detailed security rules documentation
- **[MANUAL_TESTING_GUIDE.md](MANUAL_TESTING_GUIDE.md)** - Manual testing procedures
- **[LOGGING_STRATEGY.md](LOGGING_STRATEGY.md)** - Logging guidelines and best practices

## Firebase Setup

### Firestore Security Rules

The app uses Firestore security rules to enforce household-based access control. See the following documentation:

- **[FIRESTORE_RULES_README.md](FIRESTORE_RULES_README.md)** - Overview and quick start
- **[FIRESTORE_SECURITY_RULES.md](FIRESTORE_SECURITY_RULES.md)** - Detailed security rules documentation
- **[DEPLOY_FIRESTORE_RULES.md](DEPLOY_FIRESTORE_RULES.md)** - Step-by-step deployment guide
- **[SECURITY_RULES_REFERENCE.md](SECURITY_RULES_REFERENCE.md)** - Quick reference for developers

### Deploying Security Rules

```bash
# Install Firebase CLI
npm install -g firebase-tools

# Login to Firebase
firebase login

# Deploy security rules
cd expense-tracker-app
firebase deploy --only firestore:rules
```

### Testing Security Rules

```bash
# Install dependencies
npm install

# Start Firebase emulator
npm run emulator:start

# Run automated tests
npm run test:rules
```

For detailed testing instructions, see [FIRESTORE_SECURITY_RULES.md](FIRESTORE_SECURITY_RULES.md).

## Contributing

When contributing to this project, please:

1. Follow the existing code style and architecture patterns
2. Add inline documentation (KDoc) for public APIs
3. Update relevant documentation files
4. Use the `Logger` utility for logging (see [LOGGING_STRATEGY.md](LOGGING_STRATEGY.md))
5. Write tests for new features
6. Ensure Firestore security rules are updated if data models change

## Project Documentation Structure

```
expense-tracker-app/
├── README.md                          # Main project documentation
├── HOUSEHOLD_SETUP_GUIDE.md          # User guide for household features
├── SYNC_ARCHITECTURE.md              # Technical sync implementation docs
├── FIRESTORE_SECURITY_RULES.md       # Security rules documentation
├── FIRESTORE_RULES_README.md         # Quick start for security rules
├── DEPLOY_FIRESTORE_RULES.md         # Deployment guide
├── SECURITY_RULES_REFERENCE.md       # Developer reference
├── MANUAL_TESTING_GUIDE.md           # Testing procedures
└── LOGGING_STRATEGY.md               # Logging guidelines
```

## License

This project is for educational purposes.
