# CharlzTechTV

Live sports streaming and TV channels for **Android** and **Windows**.

## Apps

| Platform | Package / ID | Version |
|----------|--------------|---------|
| Android | `com.charlztech.charlztechtv` | 1.0.11 |
| Windows desktop | CharlzTechTV | 1.0.11 |

## Features

- Live, upcoming, and ended sports events
- Multi-server playback with event navigation
- HLS, DASH, ClearKey DRM, WebView embed fallback
- Regional M3U channel playlists
- Search, favorites, Material 3 UI
- Android TV / leanback launcher support

## Build — Android

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:bundleRelease
```

Release AAB output: `app/build/outputs/bundle/release/app-release.aab`

## Build — Windows

```powershell
.\gradlew.bat :desktop:packageMsi
# or full installer with bundled VLC + VC++:
.\desktop\packaging\build-windows-installer.ps1
```

## Architecture

- **UI**: Jetpack Compose + Material 3 + MVVM
- **Player (Android)**: Media3 ExoPlayer
- **Player (Desktop)**: VLCJ
- **Network**: OkHttp + Firebase Remote Config + AES-256-CBC
- **Storage**: Room (Android favorites), JSON file (desktop favorites)

## Requirements

- **Android**: API 24+ (targets Android 16 / API 36)
- **Windows**: 64-bit Windows 10+
