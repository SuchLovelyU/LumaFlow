# LumaFlow

LumaFlow 是一个用于通过 Bluetooth Low Energy 控制 2 x 4 WS2812 RGB 灯板的 Android 应用。应用内置实时渲染引擎、手机端灯板预览、传感器互动效果和关键帧编辑器，方便在手机上调试灯效并把最终帧发送到硬件。

当前项目仍以 8 颗 LED 的 2 x 4 矩阵为主要目标硬件，协议说明见 [BLUETOOTH_WS2812_PROTOCOL.md](BLUETOOTH_WS2812_PROTOCOL.md)。

## 功能特性

- 2 x 4 LED 矩阵实时预览，预览只展示当前效果本身的亮度变化，不叠加全局亮度限制。
- BLE 扫描、连接、断开、整帧发送和全灯颜色命令。
- Home 页面集成效果选择、传感器模式和设备状态。
- Live Control 支持颜色盘、RGB 调节、亮度和速度控制。
- Editor 支持关键帧播放、添加、复制、移动、删除、时长、颜色和亮度编辑。
- Editor 支持保存历史预设、选择预设、删除预设，以及导入/导出关键帧配置文件。
- 颜色编辑使用弹窗式取色器，保持和应用 Material 3 风格一致。
- 全局最大亮度限制只作用于硬件输出，适合高亮 LED 灯板的实际使用。
- 动画速度滑动时保持连续时钟推进，避免当前颜色突然跳变。
- 呼吸灯使用非线性亮度曲线，让亮度在 255 到 0 再回到 255 的过程更自然。
- 重力模式使用连续采样计算中间亮度，并针对实际 2 x 4 灯板做物理 LED 顺序映射。

## 主要效果

- Static：固定颜色和亮度。
- Breath：非线性呼吸亮度变化。
- Runner / Wave / Sparkle：带速度控制的动态灯效。
- Music Pulse：根据麦克风 RMS 音量触发灯光。
- Gravity Fluid：根据手机倾斜方向模拟连续液面。
- Gyro Follow：根据陀螺仪方向变化生成动态响应。
- Shake Burst：根据晃动强度触发亮度爆发。

## 硬件与权限

目标硬件：

- 8 颗 WS2812 RGB LED。
- 物理布局为 2 行 4 列。
- App 通过 BLE GATT 向蓝牙串口模块发送二进制帧。
- FPGA 或下位机侧按 `AA 55 CMD PAYLOAD CS` 协议解析命令。

Android 权限：

- `BLUETOOTH`、`BLUETOOTH_ADMIN`、`ACCESS_FINE_LOCATION`：兼容较旧 Android 版本。
- `BLUETOOTH_SCAN`、`BLUETOOTH_CONNECT`：Android 12 及以上蓝牙权限。
- `RECORD_AUDIO`：Music Pulse 音乐模式。

硬件特性：

- Bluetooth LE 必需。
- 麦克风可选。
- 加速度计和陀螺仪可选。

## 技术栈

- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- ViewModel
- StateFlow
- Android Sensor APIs
- AudioRecord
- Bluetooth LE GATT

项目配置：

- `applicationId`: `com.example.rgbcontrller`
- `minSdk`: 26
- `targetSdk`: 36
- `versionName`: `1.0`

## 项目结构

```text
app/src/main/java/com/example/rgbcontrller/
  MainActivity.kt
  data/
    bluetooth/       BLE 服务和 WS2812 协议打包
    mock/            仓库实现、传感器采样、动画时钟和本地状态
  domain/
    engine/          灯效渲染引擎
    model/           UI、灯效、设备、传感器和关键帧模型
    repository/      仓库接口
  presentation/
    navigation/      Compose 导航
    screens/         Home、Live、Editor、Device、Settings 页面
    ui/components/   通用 Compose 组件
  ui/theme/          主题、颜色和字体
```

关键文件：

```text
app/src/main/java/com/example/rgbcontrller/domain/engine/LightEngine.kt
app/src/main/java/com/example/rgbcontrller/data/mock/MockRepositories.kt
app/src/main/java/com/example/rgbcontrller/data/bluetooth/Ws2812Protocol.kt
app/src/main/java/com/example/rgbcontrller/data/bluetooth/AndroidBluetoothService.kt
app/src/main/java/com/example/rgbcontrller/presentation/screens/dashboard/DashboardScreen.kt
app/src/main/java/com/example/rgbcontrller/presentation/screens/live/LiveControlScreen.kt
app/src/main/java/com/example/rgbcontrller/presentation/screens/editor/EditorScreen.kt
```

## 渲染与输出策略

应用内部先由 `LightEngine` 生成完整的 `LedMatrix`，再分别用于手机预览和 BLE 发送。

- 手机预览：显示效果原始亮度，方便观察呼吸、重力等细微变化。
- 硬件输出：发送前应用全局最大亮度限制，避免实体灯板刺眼。
- BLE 帧：对每颗灯的 RGB 做亮度预乘，兼容忽略独立亮度字节的下位机实现。
- 重力模式：手机预览和硬件输出使用同一逻辑帧，硬件发送前再做 2 x 4 灯板物理顺序映射。

## 构建

运行单元测试：

```powershell
.\gradlew.bat test
```

构建 Debug APK：

```powershell
.\gradlew.bat assembleDebug
```

APK 输出位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

如果 Windows 仍显示旧 APK，可以先删除输出文件再重新构建：

```powershell
Remove-Item -LiteralPath "app\build\outputs\apk\debug\app-debug.apk" -Force
.\gradlew.bat assembleDebug
```

停止 Gradle daemon：

```powershell
.\gradlew.bat --stop
```

## 测试

当前单元测试覆盖：

- WS2812 协议帧和校验。
- 2 x 4 灯板物理映射。
- 全局亮度限制和硬件 RGB 预乘。
- 呼吸灯和动态效果亮度行为。
- 重力模式连续亮度变化。
- 编辑器关键帧、预设、导入和导出。
- 设置仓库和传感器相关状态。

## Git 常用命令

```powershell
git status --short
git log --oneline
```

提交前建议至少运行：

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
```

## 后续方向

- 增加真实设备连接诊断和错误提示。
- 为关键页面补充 Compose Preview 或截图测试。
- 支持不同尺寸和不同物理布线方式的灯板配置。
- 优化 Editor 时间轴的拖拽排序和批量编辑。
- 为硬件协议文档补充实际 BLE 服务 UUID、特征 UUID 和调试流程截图。
