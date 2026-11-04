# Contributing to StorageLens 🤝

Thank you for your interest in contributing to **StorageLens**! We welcome all contributions—from bug fixes and performance improvements to documentation and UI enhancements.

## 📋 Code of Conduct
Please be polite, respectful, and constructive in all issues, pull requests, and discussions.

## 🛠️ Development Setup
1. Fork and clone the repository:
   ```bash
   git clone https://github.com/<your-username>/StorageLens.git
   cd StorageLens
   ```
2. Open the project in **Android Studio Hedgehog (2023.1.1)** or newer.
3. Ensure you have **JDK 17** installed and configured in Android Studio Gradle settings.
4. Build the project:
   ```bash
   ./gradlew assembleDebug
   ```

## 📐 Guidelines
- **Kotlin Style**: Follow standard Android Kotlin coding conventions.
- **Compose Best Practices**: Use unidirectional data flow (UDF) with `StateFlow` and immutable data classes.
- **Scoped Storage**: StorageLens strictly complies with modern Android Scoped Storage. Do **not** add network permissions or bypass storage boundaries.
- **Privacy First**: StorageLens is 100% offline. Pull requests that introduce tracking, telemetry, or remote network calls will not be accepted.

## 🚀 Submitting a Pull Request
1. Create a feature branch:
   ```bash
   git checkout -b feature/your-feature-name
   ```
2. Commit your changes with clear, descriptive commit messages.
3. Push to your fork:
   ```bash
   git push origin feature/your-feature-name
   ```
4. Open a Pull Request against the `main` branch with a summary of changes and before/after screenshots if applicable.
