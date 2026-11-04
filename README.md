# StorageLens 🔍📱
**Open-Source Android Storage Analyzer & Disk Manager**

StorageLens is a modern, privacy-first, open-source Android storage analyzer engineered with **Jetpack Compose**, **Material 3**, and **Kotlin Coroutines**. It empowers users to understand device storage consumption, identify large files, explore directories with breadcrumbs, and safely manage duplicate files without violating user privacy.

---

## ✨ Features

- 📊 **Real-time Storage Dashboard**:
  - Accurate device storage capacity, used, and available space directly from native Android APIs (`StatFs` and `StorageManager`).
  - Smooth animated circular storage gauge with electric blue and cyan accents.
  - Multi-category storage breakdown (Images, Videos, Audio, Documents, Downloads, Apps & System, Other Files) with proportional segmented horizontal bars.
  - Prominent asynchronous **"Scan Storage"** action with real-time progress and cancellation support.

- 🗂️ **Deep Folder Explorer**:
  - Interactive breadcrumb navigation (`Internal Storage > DCIM > Camera`).
  - Dynamic folder size and file count computation.
  - Multi-criteria sorting: Largest first, Smallest first, Alphabetical (A-Z), and Recently modified.
  - SAF (Storage Access Framework) custom folder picker to access user-designated directories safely.
  - Direct actions: Open in compatible apps, Share via FileProvider, and Safe Delete.

- 🐘 **Large Files Finder**:
  - Dedicated quick-filter chips: `> 100 MB`, `> 250 MB`, `> 500 MB`, `> 1 GB`.
  - Filter by media type (Videos, Images, Documents, Audio, Other).
  - Cumulative size aggregation and date-modified badges.
  - Deletion confirmation dialogs ensuring files are never deleted accidentally.

- 👥 **Duplicate Files Finder**:
  - **Two-stage detection**: High-speed preliminary file-size filtering followed by streaming chunked **SHA-256 hashing**.
  - Identifies exact duplicate copies regardless of file names.
  - Grouped display with original copy protection and selective duplicate deletion.

- 💡 **Actionable Storage Insights**:
  - Stale file detection (files untouched for over 180 days).
  - Largest individual files ranking.
  - Largest directories ranking.
  - Downloads folder usage analysis.

- 🎨 **Modern Design & Theming**:
  - Near-black background (`#0B0D12`) and dark charcoal cards (`#151922`).
  - Electric Blue (`#2979FF`) and Electric Cyan (`#00E5FF`) accents with subtle borders (`#222938`).
  - Full support for Dark, Light, and System themes via Jetpack DataStore Preferences.

- 🛡️ **Privacy & Scoped Storage Compliant**:
  - **100% Offline**: No internet permission in `AndroidManifest.xml`.
  - **Zero Telemetry**: No analytics, trackers, or advertising SDKs.
  - **Scoped Storage Compliant**: Uses Android MediaStore and SAF APIs without demanding unnecessary broad filesystem access.

---

## 🏗️ Architecture & Technology Stack

StorageLens is built following modern Android Architecture Components (MVVM) and Clean Architecture principles:

```
app/
 ├── data/
 │    ├── datasource/      # DeviceStorageDataSource, MediaStoreDataSource, FileSystemDataSource, SafDataSource
 │    ├── models/          # StorageStats, StorageCategory, FileItem, FolderItem, DuplicateGroup, ScanState
 │    ├── preferences/     # UserPreferencesRepository (Jetpack DataStore)
 │    └── repository/      # StorageRepository & StorageRepositoryImpl
 ├── domain/
 │    └── usecases/        # GetDeviceStorage, ScanStorage, GetLargeFiles, FindDuplicates, GetStorageInsights, DeleteFile
 ├── permissions/          # PermissionManager (Granular Media permissions API 33+ & API 29-32)
 ├── scanner/              # StorageScanner (Coroutines Dispatchers.IO), DuplicateDetector (SHA-256)
 ├── ui/
 │    ├── components/      # CircularStorageGauge, CategoryProgressBar, StorageCard, FileListItem, FolderListItem, Breadcrumbs
 │    ├── screens/         # Overview, Explore, LargeFiles, Insights, Settings
 │    └── theme/           # Color palette, Material 3 Typography & Theme
 ├── navigation/           # StorageLensNavHost, BottomNavBar, Screen routes
 └── utils/                # StorageFormatter, HashUtils (streaming SHA-256), MimeUtils, DateFormatter
```

- **Language**: Kotlin 1.9.23
- **UI Toolkit**: Jetpack Compose with Material 3
- **Asynchronous Operations**: Kotlin Coroutines & StateFlow
- **Persistence**: Jetpack DataStore Preferences
- **Minimum SDK**: Android 10 (API 29)
- **Target SDK**: Android 14 (API 34)
- **Build System**: Gradle 8.5 with Kotlin DSL (`build.gradle.kts`)

---

## 🚀 Building & Running

### Prerequisites
- JDK 17 (Eclipse Temurin, OpenJDK, or Zulu)
- Android SDK with platform `android-34` and build-tools `34.0.0`

### Build APK
```bash
./gradlew assembleDebug
```
The compiled debug APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

### Run Unit Tests
```bash
./gradlew test
```

---

## 📄 License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.
