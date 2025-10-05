# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Android Camera is a WebRTC-based camera streaming application built with Kotlin and Android Jetpack. The app enables camera streaming and QR code functionality through two main features: camera preview with WebRTC streaming capabilities and QR code scanning/generation for connection sharing.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean and build debug APK
./gradlew clean assembleDebug

# Run all checks (lint, test)
./gradlew check

# Install debug APK on connected device
./gradlew installDebug

# Grant execute permissions (if needed on Unix systems)
chmod +x gradlew
```

## Architecture

### Navigation Structure
- **MainActivity**: Main activity using Navigation Component with toolbar and FAB
- **FirstFragment**: Camera preview functionality using CameraX
- **SecondFragment**: QR code scanning and WebRTC streaming controls
- **QRCodeDialogFragment**: QR code generation for connection sharing

### Key Components

#### WebRTC Service (`WebRTCService.kt`)
- Manages WebRTC peer connections and video streaming
- Handles camera capture using Camera2 API via WebRTC
- Provides offer/answer SDP creation for peer communication
- Uses Google STUN servers for ICE connectivity

#### Camera Integration
- **FirstFragment**: Basic camera preview using CameraX library
- **SecondFragment**: Camera with ML Kit barcode scanning
- **QRCodeDialogFragment**: WebRTC video capture integration

#### Permission Handling
- Camera permission for both fragments
- Audio recording permission for WebRTC streaming
- Uses `ActivityResultContracts` for modern permission requests

### Dependencies

Key external libraries:
- **CameraX**: `androidx.camera:*` for camera functionality
- **WebRTC**: `com.dafruits:webrtc:123.0.0` for peer-to-peer streaming
- **ML Kit**: `com.google.mlkit:barcode-scanning` for QR code detection
- **ZXing**: `com.journeyapps:zxing-android-embedded` for QR code generation
- **Material Design**: Standard Android UI components

### Build Configuration

- **Target SDK**: 36 (Android 14+)
- **Min SDK**: 24 (Android 7.0+)
- **Compile SDK**: 36
- **Build Tools**: 35.0.0
- **Kotlin**: Latest stable version
- **Java**: Version 11 compatibility
- **View Binding**: Enabled for type-safe view access
- **Application ID**: `br.tmvdl.android.camera`

### CI/CD

GitHub Actions workflow (`.github/workflows/android-ci.yml`):
- Builds on push/PR to main/develop branches
- Creates debug APK
- Uploads APK as GitHub release with commit-based tagging
- Uses JDK 17 for builds

### Package Structure

```
br.tmvdl.android.camera/
├── MainActivity.kt              # Main navigation activity
├── FirstFragment.kt             # Camera preview fragment
├── SecondFragment.kt            # QR scanning + WebRTC controls
├── QRCodeDialogFragment.kt      # Connection QR code dialog
└── WebRTCService.kt             # WebRTC peer connection management
```

### Development Notes

- All fragments use view binding for type-safe view access
- Camera operations run on background executor threads
- WebRTC operations use single-threaded executor
- Proper lifecycle management for camera and WebRTC resources
- Error handling with logging using Android Log class
- No unit tests currently implemented in the project
- Proguard rules available but minification disabled in debug builds