# Android Camera

A WebRTC-based camera streaming application built with Kotlin and Android Jetpack. Stream your camera feed to other devices using peer-to-peer connections and share connection details via QR codes.

## Features

- **Camera Preview**: Live camera preview using CameraX
- **WebRTC Streaming**: Peer-to-peer video streaming capabilities
- **QR Code Scanning**: Scan QR codes to connect to other devices
- **QR Code Generation**: Generate QR codes to share connection details
- **Material Design**: Modern Android UI with Navigation Component

## Requirements

- Android 7.0+ (API level 24)
- Camera permission
- Audio recording permission (for WebRTC streaming)

## Installation

1. Download the latest APK from the [Releases](../../releases) page
2. Enable "Install from unknown sources" in your Android settings
3. Install the APK on your device
4. Grant camera and audio permissions when prompted

## Usage

### Camera Preview
- Open the app to view live camera feed
- Navigate between camera preview and QR code functionality using the bottom navigation

### WebRTC Streaming
1. Go to the QR Code section
2. Tap the "Generate QR Code" button to create a connection QR code
3. Share the QR code with another device
4. The other device can scan the QR code to establish a WebRTC connection

### QR Code Scanning
- Use the QR scanner to scan connection codes from other devices
- The app will automatically attempt to establish a WebRTC connection

## Build from Source

### Prerequisites
- Android Studio Arctic Fox or later
- JDK 11 or later
- Android SDK with API level 36

### Build Commands

```bash
# Clean and build debug APK
./gradlew clean assembleDebug

# Build release APK
./gradlew assembleRelease

# Run tests and lint checks
./gradlew check

# Install on connected device
./gradlew installDebug
```

## Architecture

- **MainActivity**: Main navigation host
- **FirstFragment**: Camera preview using CameraX
- **SecondFragment**: QR code scanning and WebRTC controls
- **QRCodeDialogFragment**: Connection QR code generation
- **WebRTCService**: WebRTC peer connection management

## Technologies Used

- **Kotlin**: Primary development language
- **CameraX**: Camera functionality
- **WebRTC**: Peer-to-peer video streaming
- **ML Kit**: Barcode/QR code scanning
- **ZXing**: QR code generation
- **Material Design Components**: UI framework
- **Navigation Component**: Fragment navigation

## License

This project is open source. See the repository for more details.

## Contributing

Contributions are welcome! Please feel free to submit issues and pull requests.