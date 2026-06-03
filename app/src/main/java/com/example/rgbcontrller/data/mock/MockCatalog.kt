package com.example.rgbcontrller.data.mock

import com.example.rgbcontrller.domain.model.ConnectionStatus
import com.example.rgbcontrller.domain.model.DeviceInfo
import com.example.rgbcontrller.domain.model.EffectCategory
import com.example.rgbcontrller.domain.model.Keyframe
import com.example.rgbcontrller.domain.model.LightEffect
import com.example.rgbcontrller.domain.model.RgbColor
import com.example.rgbcontrller.domain.model.SensorMode
import com.example.rgbcontrller.domain.model.SensorModeType

object MockCatalog {
    val device = DeviceInfo(
        name = "Aurora Matrix 2x4",
        firmwareVersion = "v0.9.2-dev",
        ledCount = 8,
        batteryPercent = 86,
        isCharging = false,
        connectionStatus = ConnectionStatus.Connected,
    )

    val effects = listOf(
        LightEffect("solid", "常亮", "稳定输出统一颜色，适合环境照明。", EffectCategory.Basic, listOf(RgbColor.White, RgbColor.Cyan)),
        LightEffect("breath", "呼吸", "柔和明暗起伏，适合夜间氛围。", EffectCategory.Basic, listOf(RgbColor.Cyan, RgbColor.Violet)),
        LightEffect("blink", "闪烁", "短促节拍闪动，可用于提示和节奏。", EffectCategory.Basic, listOf(RgbColor.White, RgbColor.Amber)),
        LightEffect("run", "流水", "灯光沿阵列方向连续推进。", EffectCategory.Basic, listOf(RgbColor.Green, RgbColor.Cyan)),
        LightEffect("rainbow", "彩虹", "多色渐变流动，展示完整 RGB 能力。", EffectCategory.Dynamic, listOf(RgbColor.Coral, RgbColor.Amber, RgbColor.Green, RgbColor.Cyan, RgbColor.Violet)),
        LightEffect("wave", "波浪", "柔和色带在矩阵中往复扩散。", EffectCategory.Dynamic, listOf(RgbColor.Cyan, RgbColor.Violet, RgbColor.Pink)),
        LightEffect("meteor", "流星", "高亮拖尾穿过灯阵。", EffectCategory.Dynamic, listOf(RgbColor.White, RgbColor.Cyan)),
        LightEffect("flame", "火焰", "暖色随机跳动，模拟火焰核心。", EffectCategory.Dynamic, listOf(RgbColor.Coral, RgbColor.Amber)),
        LightEffect("aurora", "极光", "低饱和绿蓝紫缓慢漂移。", EffectCategory.Dynamic, listOf(RgbColor.Green, RgbColor.Cyan, RgbColor.Violet)),
        LightEffect("neon", "霓虹", "高对比电光色切换。", EffectCategory.Dynamic, listOf(RgbColor.Pink, RgbColor.Cyan)),
        LightEffect("music", "音乐律动", "根据音量、频谱和鼓点驱动灯光。", EffectCategory.Advanced, listOf(RgbColor.Cyan, RgbColor.Pink, RgbColor.Amber)),
        LightEffect("gravity", "重力模拟", "倾斜手机，颜色像液体一样流向低处。", EffectCategory.Advanced, listOf(RgbColor.Cyan, RgbColor.Green)),
        LightEffect("shake", "摇晃响应", "摇晃触发爆闪、彩虹喷射和粒子爆发。", EffectCategory.Advanced, listOf(RgbColor.White, RgbColor.Pink, RgbColor.Coral)),
        LightEffect("direction", "方向响应", "根据手机朝向改变灯光移动方向。", EffectCategory.Advanced, listOf(RgbColor.Violet, RgbColor.Cyan)),
        LightEffect("ambient", "环境光响应", "根据环境亮度调整色温和强度。", EffectCategory.Advanced, listOf(RgbColor.White, RgbColor.Amber)),
    )

    val shortcuts = listOf(
        effects.first { it.id == "run" },
        effects.first { it.id == "breath" },
        effects.first { it.id == "rainbow" },
        effects.first { it.id == "music" },
        effects.first { it.id == "gravity" },
        effects.first { it.id == "flame" },
        LightEffect("starfield", "星空效果", "随机星点缓慢闪耀。", EffectCategory.Dynamic, listOf(RgbColor.White, RgbColor.Violet)),
        LightEffect("lightning", "闪电效果", "冷白高亮突发闪击。", EffectCategory.Dynamic, listOf(RgbColor.White, RgbColor.Cyan)),
    )

    val sensorModes = listOf(
        SensorMode("music", "音乐律动", "麦克风输入驱动频谱、音量和鼓点灯效。", SensorModeType.Music, listOf("频谱模式", "音量模式", "鼓点模式")),
        SensorMode("gravity", "重力液体", "倾斜手机时，2x4 灯阵中的光像水一样流向低处。", SensorModeType.Gravity, listOf("液体流动", "低处聚集", "颜色惯性")),
        SensorMode("gyro", "旋转跟随", "陀螺仪控制灯光在矩阵中滑动和旋转。", SensorModeType.Gyroscope, listOf("旋转移动", "方向追踪")),
        SensorMode("shake", "摇晃触发", "手机摇晃触发爆闪、彩虹喷射和粒子爆发。", SensorModeType.Shake, listOf("爆闪", "彩虹喷射", "粒子爆发")),
    )

    val keyframes = listOf(
        Keyframe("kf-1", RgbColor.Cyan, 0.8f, 420),
        Keyframe("kf-2", RgbColor.Violet, 1f, 560),
        Keyframe("kf-3", RgbColor.Pink, 0.72f, 380),
        Keyframe("kf-4", RgbColor.Green, 0.9f, 640),
    )
}
