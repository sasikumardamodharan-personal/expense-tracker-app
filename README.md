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

## Features (Planned)

- Add, edit, and delete expenses
- Categorize expenses
- Filter expenses by date range and category
- View spending summaries and statistics
- Local data persistence
- Material Design 3 theming with light/dark mode support

## License

This project is for educational purposes.
