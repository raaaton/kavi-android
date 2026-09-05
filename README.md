# Kavi for Android

Native Android port of **Kavi**, built with Kotlin and Jetpack Compose.

> Development is currently on the `android-port` branch. The iOS repository [`raaaton/kavi`](https://github.com/raaaton/kavi) is the product and data-contract reference and remains separate/read-only for Android development.

## Current milestone

This repository contains the first functional Android foundation:

- native Kotlin + Jetpack Compose application
- dark Kavi visual identity with mint `#46D7A7`
- Navigation Compose
- Room local database
- Preferences DataStore
- local-first/offline library
- folders and Unfiled decks
- folder ordering
- deck/card creation, editing and deletion
- persistent card ordering
- pin and star state
- deck/folder duplication with fresh UUIDs
- Room cascade semantics matching the iOS model
- "delete folder but keep decks" behavior
- Study/Test serializable state models
- custom Test configuration models
- backup schema v1/v2 decoding and v2 encoding core
- UUID-based backup merge/upsert service
- unit tests for persistence invariants
- Android CI for tests + debug APK build

The complete Study/Test run UI, search/import UI, authored-test editor, bulk import UI, localization pass, and cloud sync are **not** part of this milestone yet.

## Build

Requirements:

- JDK 17
- Android SDK 36
- Gradle 8.13, or Android Studio configured to use the project-compatible Gradle distribution

From the repository root:

```bash
gradle :app:assembleDebug
```

Run unit tests:

```bash
gradle :app:testDebugUnitTest
```

Or open the repository in Android Studio, sync Gradle, select the `app` run configuration, and run it on an Android device/emulator (API 24+).

## Architecture

The codebase deliberately stays small:

```text
app/src/main/java/com/raton/kavi/
├── KaviApplication.kt
├── MainActivity.kt
├── data/
│   ├── Entities.kt
│   ├── Daos.kt
│   ├── KaviDatabase.kt
│   ├── LibraryRepository.kt
│   ├── PreferencesRepository.kt
│   └── BackupService.kt
├── domain/
│   ├── StudyModels.kt
│   ├── TestModels.kt
│   ├── SessionCodecs.kt
│   └── BackupModels.kt
└── ui/
    ├── KaviApp.kt
    ├── LibraryScreens.kt
    ├── SettingsScreen.kt
    └── theme/KaviTheme.kt
```

There is intentionally no backend, account system, Supabase integration, analytics, runtime network dependency, Flutter, React Native, or Kotlin Multiplatform layer.

See [`PROJECT.md`](PROJECT.md) for the detailed Android technical map and parity status.
