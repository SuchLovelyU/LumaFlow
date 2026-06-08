# RGB Controller Project Notes

## Project Goal

This is a phone-only Android app for controlling an RGB LED matrix over Bluetooth.

Current hardware target:

- 2 x 4 LED matrix
- 8 LEDs total
- Each LED is expected to support RGB color, brightness, and dynamic effect parameters
- Future hardware may increase LED count

Current development priority:

- High-quality frontend UI and interaction prototype
- Local mock data only
- No real Bluetooth communication yet
- No backend

## Tech Stack

- Android 16 / API 36
- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- ViewModel
- StateFlow
- MVVM-style separation
- Dynamic color and dark mode support via Compose Material 3 theme

## Current Git Baseline

Initial committed version:

```text
a706033 Initial RGB LED controller Compose app
```

The current project has been reset back to this baseline after a rejected UI refinement attempt. Future UI changes should be more carefully scoped and reviewed against screenshots before committing.

## Current App Structure

Main app entry:

```text
app/src/main/java/com/example/rgbcontrller/MainActivity.kt
```

Main package layout:

```text
com.example.rgbcontrller/
  data/
    bluetooth/
    mock/

  domain/
    engine/
    model/
    repository/

  presentation/
    navigation/
    screens/
    ui/components/

  ui/theme/
```

## Screens Implemented

The app currently contains these screens:

- Dashboard
- Effects
- Effect detail
- Live Control
- Sensor Modes
- Sensor mode detail
- Editor
- Device
- Settings

Bottom navigation includes:

- Home
- Effects
- Live
- Sensors
- Editor

Device and Settings are secondary screens.

## Navigation

Navigation is defined in:

```text
app/src/main/java/com/example/rgbcontrller/presentation/navigation/AppNavigation.kt
```

Routes:

```text
dashboard
effects
effect/{effectId}
live
sensors
sensor/{modeId}
editor
device
settings
```

Screens do not directly own app architecture decisions. They receive navigation callbacks such as:

- `onOpenDevice`
- `onOpenEffect`
- `onOpenMode`
- `onOpenSettings`

## Domain Models

Defined in:

```text
app/src/main/java/com/example/rgbcontrller/domain/model/LightModels.kt
```

Important models:

- `RgbColor`
- `LedPixel`
- `LedMatrix`
- `DeviceInfo`
- `LightEffect`
- `LiveControl`
- `SensorMode`
- `SensorSnapshot`
- `Keyframe`
- `LightSessionState`

The app is currently centered around `LightSessionState`, which contains:

- Current mock device
- Current LED matrix frame
- Active effect
- Live control values
- Playback state

## Mock Data

Mock catalog:

```text
app/src/main/java/com/example/rgbcontrller/data/mock/MockCatalog.kt
```

Mock repositories:

```text
app/src/main/java/com/example/rgbcontrller/data/mock/MockRepositories.kt
```

Mock data includes:

- Device info
- Effect catalog
- Dashboard shortcuts
- Sensor modes
- Editor keyframes

`MockLightRepository` continuously emits animated LED matrix frames through `StateFlow`.

## Light Engine

Local rendering engine:

```text
app/src/main/java/com/example/rgbcontrller/domain/engine/LightEngine.kt
```

Purpose:

- Generate animated `LedMatrix` frames
- Support mock effects such as solid, blink, flow, meteor, music, flame, aurora, etc.
- Keep UI interactive before real Bluetooth protocol exists

Current frame target:

```text
rows = 2
columns = 4
```

Future improvement:

- Make matrix dimensions configurable per device
- Separate effect calculation from frame transport
- Add protocol-ready frame serialization

## Bluetooth Boundary

Bluetooth is not implemented yet.

Reserved interface:

```text
app/src/main/java/com/example/rgbcontrller/data/bluetooth/BluetoothService.kt
```

Current interface shape:

- `scan()`
- `connect(deviceAddress)`
- `disconnect()`
- `sendFrame(matrix)`
- `connectionEvents`

Future BLE work should plug into repositories without rewriting UI screens.

## UI Components

Shared components live in:

```text
app/src/main/java/com/example/rgbcontrller/presentation/ui/components/CoreComponents.kt
```

Important components:

- `AuroraBackground`
- `DeviceStatusHeader`
- `LedMatrixPreview`
- `SceneShortcutCard`
- `EffectMarketCard`
- `MiniEffectPreview`
- `ColorWheelControl`
- `ExpressiveSlider`
- `DirectionSegmentedControl`
- `SensorModeCard`
- `SensorPreview`
- `TimelineEditor`
- `PageTitle`

Current UI is a working first prototype, not a final polished visual system.

## Theme

Theme files:

```text
app/src/main/java/com/example/rgbcontrller/ui/theme/Color.kt
app/src/main/java/com/example/rgbcontrller/ui/theme/Theme.kt
app/src/main/java/com/example/rgbcontrller/ui/theme/Type.kt
```

Current theme supports:

- Material 3 color scheme
- Dynamic color when available
- Light and dark color schemes

## Known UI Issues

From screenshot review, the initial committed UI has several areas needing improvement:

- Bottom navigation uses text placeholders instead of proper icons
- Some card proportions feel oversized
- Some pages have too much empty vertical space
- Some text can crowd or wrap poorly
- LED preview is visually interesting but not yet product-grade
- Effects and sensor cards need stronger hierarchy
- Editor needs a more professional timeline/control surface

Important: A previous broad UI refactor was rejected as a visual regression. Future UI refinement should be incremental and screenshot-driven.

## Recommended UI Refinement Strategy

Use small, reviewable steps:

1. Fix only the bottom navigation icons and content insets.
2. Fix obvious text overflow issues, especially Device info rows.
3. Improve `LedMatrixPreview` while preserving the existing visual direction.
4. Refine Dashboard shortcut cards.
5. Refine Effects list cards.
6. Refine Live Control layout.
7. Refine Editor timeline.

Avoid changing all visual language at once.

## Build Commands

Build debug APK:

```powershell
.\gradlew.bat assembleDebug
```

Stop Gradle daemon:

```powershell
.\gradlew.bat --stop
```

If build fails with:

```text
Unable to delete app/build/outputs/apk/debug/app-debug.apk
```

This usually means Windows has locked the old APK. Close Android Studio APK Analyzer, file preview windows, emulator install tasks, or restart Android Studio.

## Git Notes

Current repository was initialized locally.

Useful commands:

```powershell
git status --short
git log --oneline
git reset --hard a706033
```

Do not commit broad UI changes unless they have been visually reviewed.

## Next Development Priorities

Recommended next steps:

1. Establish visual acceptance criteria using screenshots.
2. Add real icons carefully, without changing layout proportions.
3. Fix Device info row overflow.
4. Add preview screenshots or Compose previews for key components.
5. Add DataStore for Settings.
6. Make LED matrix size configurable.
7. Add Bluetooth repository implementation behind existing interfaces.
8. Add keyframe editing and drag sorting in Editor.

