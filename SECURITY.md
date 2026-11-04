# Security Policy 🛡️

## Supported Versions
Security updates are provided for the latest version on the `main` branch.

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| < 1.0   | :x:                |

## Privacy & Offline Architecture
StorageLens is architected to be **completely offline**:
- Zero network permissions (`android.permission.INTERNET` is omitted from `AndroidManifest.xml`).
- Zero external SDKs, analytics, or trackers.
- All file hashing (SHA-256) is executed locally on-device in streaming 64 KB memory buffers.

## Reporting a Vulnerability
If you discover a potential security or privacy vulnerability in StorageLens:
1. Please report it privately by opening a [Security Advisory](https://github.com/rehaan212006/StorageLens/security/advisories/new) or contacting the maintainer directly.
2. Provide details on how to reproduce the issue and the potential impact.
3. We will respond promptly and issue a fix in the next release.
