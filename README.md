# LumaFlow

Android app for controlling a small RGB LED matrix over Bluetooth Low Energy.

The current hardware target is a 2 x 4 RGB LED matrix. The app is still built around a local rendering engine so UI, effects, sensors, and editor behavior can be developed and tested before every Bluetooth transport detail is finalized.

## Current State

- Kotlin Android app using Jetpack Compose and Material 3.
- App display name: `LumaFlow`.
- Bottom navigation contains Home, Live, and Editor.
- Home combines the former effects and sensor-mode surfaces into one control screen.
- Device and Settings are secondary screens.
- Debug APK output is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Main Features

- Real-time LED matrix preview for an 8 LED, 2 x 4 layout.
- Effect catalog with animated preview cards.
- Sensor modes integrated into Home:
  - Music Pulse uses microphone RMS level and a configurable threshold. LEDs remain off below the threshold.
  - Gravity Fluid uses gravity or accelerometer data to simulate a continuous liquid surface.
  - Gyro Follow uses gyroscope motion for directional behavior.
  - Shake Burst reacts to detected movement intensity.
- Live Control with direct HSV color-wheel picking, RGB sliders, brightness, speed, and direction controls.
- Editor with keyframe playback, add, duplicate, move, delete, duration, color, and brightness controls.
- Device-level max brightness limit. Home effects, Live Control, Editor playback, and direct test commands are capped by this global limit.
- BLE scanning, connection, frame sending, and all-LED color commands behind `BluetoothService`.
- Light theme with subtle tinted background, white card surfaces, rounded corners, shadows, and restrained selected states.

## Brand

`LumaFlow` combines light and motion. The launcher icon uses a soft blue-violet adaptive background and a minimal flowing light mark, keeping the brand abstract instead of drawing the hardware literally.

## Permissions And Sensors

Declared Android permissions include:

- `BLUETOOTH`, `BLUETOOTH_ADMIN`, and `ACCESS_FINE_LOCATION` for older Android versions.
- `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT` for newer Android versions.
- `RECORD_AUDIO` for Music Pulse.

Declared hardware features:

- Bluetooth LE is required.
- Microphone is optional.
- Accelerometer is optional.

At runtime, `MainActivity` requests Bluetooth and microphone permissions where required by the Android version.

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- ViewModel
- StateFlow
- Android sensors
- AudioRecord
- Bluetooth LE GATT

Project config:

- `applicationId`: `com.example.rgbcontrller`
- `minSdk`: 26
- `versionName`: `1.0`

## Project Layout

Main entry point:

```text
app/src/main/java/com/example/rgbcontrller/MainActivity.kt
```

Package layout:

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

Important files:

```text
app/src/main/java/com/example/rgbcontrller/presentation/navigation/AppNavigation.kt
app/src/main/java/com/example/rgbcontrller/presentation/screens/dashboard/DashboardScreen.kt
app/src/main/java/com/example/rgbcontrller/presentation/screens/live/LiveControlScreen.kt
app/src/main/java/com/example/rgbcontrller/presentation/screens/editor/EditorScreen.kt
app/src/main/java/com/example/rgbcontrller/presentation/ui/components/CoreComponents.kt
app/src/main/java/com/example/rgbcontrller/domain/engine/LightEngine.kt
app/src/main/java/com/example/rgbcontrller/data/mock/MockRepositories.kt
app/src/main/java/com/example/rgbcontrller/data/bluetooth/AndroidBluetoothService.kt
```

## Screens

Current navigation routes:

```text
dashboard
live
editor
device
settings
```

Home is the primary control surface. It owns the LED preview, device header, sensor modes, effect cards, and contextual controls for Music Pulse and Gravity Fluid.

## Rendering Engine

The local light engine lives in:

```text
app/src/main/java/com/example/rgbcontrller/domain/engine/LightEngine.kt
```

It renders `LedMatrix` frames for the 2 x 4 target and supports static, dynamic, music, gravity, shake, direction, and ambient-style effects. Rendered frames are capped by the device-level max brightness limit before preview and BLE output.

The active page owns the active render mode. Switching between Home, Live, and Editor applies only that page's selected/default effect so effects do not stack across tabs.

## Bluetooth

Bluetooth transport is represented by:

```text
app/src/main/java/com/example/rgbcontrller/data/bluetooth/BluetoothService.kt
app/src/main/java/com/example/rgbcontrller/data/bluetooth/AndroidBluetoothService.kt
```

The service supports:

- BLE scan
- Connect and disconnect
- Discovered device list
- Connection events
- Matrix frame sending
- Send-all color and brightness commands

UI and rendering logic talk through repository interfaces so the Bluetooth implementation can continue to evolve without rewriting screens.

## Build

Run unit tests:

```powershell
.\gradlew.bat test
```

Build debug APK:

```powershell
.\gradlew.bat assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

If Windows keeps serving an old APK, delete the existing output first and rebuild:

```powershell
Remove-Item -LiteralPath "app\build\outputs\apk\debug\app-debug.apk" -Force
.\gradlew.bat assembleDebug
```

Stop Gradle daemon:

```powershell
.\gradlew.bat --stop
```

## Git

Useful commands:

```powershell
git status --short
git log --oneline
```

Before committing UI work, build the debug APK and visually review the installed app because many changes are interaction and visual-quality sensitive.

## Next Priorities

- Validate BLE frame format against real hardware.
- Add stronger diagnostics for sensor availability and permission denial.
- Add Compose previews or screenshot tests for Home cards and editor controls.
- Make matrix dimensions configurable per device.
- Improve editor timeline manipulation with drag sorting.
