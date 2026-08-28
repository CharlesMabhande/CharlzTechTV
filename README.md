# CharlzTechTV
<<<<<<< HEAD

Modern Android live sports streaming app powered by the **CRICFy backend**.

## CRICFy Backend Integration

CharlzTechTV uses the same infrastructure as CRICFy TV:

| Component | Source |
|-----------|--------|
| **API discovery** | Firebase Remote Config (`cric_api1` / `cric_api2`) |
| **Providers** | `{base}/cats.txt` (AES encrypted) |
| **Live events** | `{base}/categories/live-events.txt` (AES encrypted) |
| **Stream servers** | `{base}/channels/{slug}.txt` (AES encrypted) |
| **M3U playlists** | Provider `catLink` URLs (encrypted or plain) |
| **Match cards** | `live-card-png.cricify.workers.dev` |

Current live API base (via Firebase): `https://cfylsvdlshv124.top`

## Features

- Auto-discovers live, upcoming, and ended sports from CRICFy `live-events.txt`
- Full provider catalog from CRICFy `cats.txt`
- Multi-server playback with Previous/Next event navigation
- HLS, DASH, ClearKey DRM, WebView embed fallback
- Pull-to-refresh + background auto-refresh every 15 minutes
- Search, favorites, Material 3 UI

## Build & Run

```powershell
cd c:\Users\CHARLZTECH\AndroidStudioProjects\CharlzTechTV
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
```

Open in Android Studio and run on API 24+ device/emulator.

## Architecture

- **UI**: Jetpack Compose + Material 3 + MVVM
- **Player**: Media3 ExoPlayer
- **Network**: OkHttp + Firebase Remote Config + AES-256-CBC
- **Storage**: Room (favorites)
- **Background**: WorkManager

## Package

`com.charlztech.tv`
=======
Sports streaming and live TV channels
>>>>>>> b71f16d3642d82b2c0269e0b00e2c51ed62d55d5
