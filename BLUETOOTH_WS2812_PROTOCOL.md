# Bluetooth WS2812 Control Protocol

本文档用于手机 App 通过蓝牙串口控制 FPGA 上的 WS2812 彩灯。

对应 FPGA 顶层模块：

```verilog
ws2812_bluetooth_controller
```

## 1. 串口参数

蓝牙模块使用 UART 透明传输模式。

| 参数 | 值 |
| --- | --- |
| 波特率 | 115200 |
| 数据位 | 8 bit |
| 停止位 | 1 bit |
| 校验位 | 无 |
| 格式 | 8N1 |
| 流控 | 无 |

App 端发送的是二进制字节流，不是 ASCII 字符串。

例如 `AA 55 01 00 00 00 FF FF 01` 应发送 9 个 byte：

```text
0xAA, 0x55, 0x01, 0x00, 0x00, 0x00, 0xFF, 0xFF, 0x01
```

不要发送字符串 `"AA5501000000FFFF01"`。

## 2. 灯号和颜色格式

当前 FPGA 代码默认控制 8 颗 WS2812。

| 灯号 | 说明 |
| --- | --- |
| 0 | 第 1 颗灯，最靠近 FPGA 数据输出端 |
| 1 | 第 2 颗灯 |
| 2 | 第 3 颗灯 |
| 3 | 第 4 颗灯 |
| 4 | 第 5 颗灯 |
| 5 | 第 6 颗灯 |
| 6 | 第 7 颗灯 |
| 7 | 第 8 颗灯 |

App 端使用 RGB 顺序发送颜色：

```text
R G B
```

每个颜色通道范围：

```text
0x00 ~ 0xFF
```

亮度 `BRIGHT` 也是 8 bit：

```text
0x00 = 熄灭
0xFF = 最高亮度
```

FPGA 内部会把 RGB 自动转换成 WS2812 需要的 GRB 发送顺序，App 不需要处理 GRB。

## 3. 通用帧结构

所有命令都以固定帧头开始：

```text
AA 55
```

通用结构：

```text
AA 55 CMD PAYLOAD CS
```

| 字段 | 长度 | 说明 |
| --- | --- | --- |
| `AA 55` | 2 bytes | 帧头 |
| `CMD` | 1 byte | 命令字 |
| `PAYLOAD` | N bytes | 命令数据 |
| `CS` | 1 byte | 异或校验 |

## 4. 校验算法

`CS` 是从 `CMD` 开始，到最后一个 payload 字节为止的逐字节异或。

不包含帧头 `AA 55`。

公式：

```text
CS = CMD ^ PAYLOAD[0] ^ PAYLOAD[1] ^ ... ^ PAYLOAD[N-1]
```

伪代码：

```c
uint8_t checksum(uint8_t cmd, uint8_t *payload, int len) {
    uint8_t cs = cmd;
    for (int i = 0; i < len; i++) {
        cs ^= payload[i];
    }
    return cs;
}
```

Kotlin 示例：

```kotlin
fun checksum(cmd: Int, payload: IntArray): Int {
    var cs = cmd and 0xFF
    for (b in payload) {
        cs = cs xor (b and 0xFF)
    }
    return cs and 0xFF
}
```

## 5. FPGA 返回值

FPGA 收到命令后，会通过 UART TX 返回 1 个 byte。

| 返回值 | 含义 |
| --- | --- |
| `0x06` | ACK，命令接收成功 |
| `0x15` | NAK，命令错误 |

可能返回 `NAK` 的情况：

| 原因 | 说明 |
| --- | --- |
| 校验错误 | `CS` 不正确 |
| 命令字错误 | `CMD` 不是支持的命令 |
| 灯号错误 | 设置单灯时 `ID > 7` |
| UART 帧错误 | 串口停止位异常 |

建议 App 每发送一帧后等待 ACK，再发送下一帧。呼吸灯等连续动画也可以不等待每帧 ACK，但调试阶段建议先等待 ACK。

## 6. 命令 0x01：设置单颗灯

设置某一颗灯的 RGB 和亮度。

帧格式：

```text
AA 55 01 ID R G B BRIGHT CS
```

| 字段 | 说明 |
| --- | --- |
| `01` | 命令字，设置单颗灯 |
| `ID` | 灯号，`0x00 ~ 0x07` |
| `R` | 红色通道，`0x00 ~ 0xFF` |
| `G` | 绿色通道，`0x00 ~ 0xFF` |
| `B` | 蓝色通道，`0x00 ~ 0xFF` |
| `BRIGHT` | 亮度，`0x00 ~ 0xFF` |
| `CS` | `01 ^ ID ^ R ^ G ^ B ^ BRIGHT` |

示例：设置 LED0 为蓝色，亮度 255。

```text
AA 55 01 00 00 00 FF FF 01
```

校验计算：

```text
01 ^ 00 ^ 00 ^ 00 ^ FF ^ FF = 01
```

示例：设置 LED3 为紫色，亮度 128。

```text
AA 55 01 03 80 00 FF 80 FD
```

校验计算：

```text
01 ^ 03 ^ 80 ^ 00 ^ FF ^ 80 = FD
```

## 7. 命令 0x02：设置全部灯为同一颜色

一次把 8 颗灯全部设置成相同 RGB 和亮度。

帧格式：

```text
AA 55 02 R G B BRIGHT CS
```

| 字段 | 说明 |
| --- | --- |
| `02` | 命令字，设置全部灯 |
| `R` | 红色通道 |
| `G` | 绿色通道 |
| `B` | 蓝色通道 |
| `BRIGHT` | 亮度 |
| `CS` | `02 ^ R ^ G ^ B ^ BRIGHT` |

示例：全部灯设置为红色，亮度 128。

```text
AA 55 02 FF 00 00 80 7D
```

校验计算：

```text
02 ^ FF ^ 00 ^ 00 ^ 80 = 7D
```

示例：全部灯设置为白色，亮度 32。

```text
AA 55 02 FF FF FF 20 22
```

校验计算：

```text
02 ^ FF ^ FF ^ FF ^ 20 = 22
```

## 8. 命令 0x10：一次设置 8 颗灯

一次发送 8 颗灯各自的 RGB 和亮度。适合做渐变、流水、呼吸等效果。

帧格式：

```text
AA 55 10 R0 G0 B0 BR0 R1 G1 B1 BR1 ... R7 G7 B7 BR7 CS
```

Payload 共 32 bytes：

```text
LED0: R0 G0 B0 BR0
LED1: R1 G1 B1 BR1
LED2: R2 G2 B2 BR2
LED3: R3 G3 B3 BR3
LED4: R4 G4 B4 BR4
LED5: R5 G5 B5 BR5
LED6: R6 G6 B6 BR6
LED7: R7 G7 B7 BR7
```

总帧长度：

```text
2 bytes 帧头 + 1 byte CMD + 32 bytes payload + 1 byte CS = 36 bytes
```

校验：

```text
CS = 10 ^ R0 ^ G0 ^ B0 ^ BR0 ^ ... ^ R7 ^ G7 ^ B7 ^ BR7
```

示例：8 颗灯分别显示红、绿、蓝、黄、青、紫、白、灭，全部亮度 255。

```text
AA 55 10
FF 00 00 FF
00 FF 00 FF
00 00 FF FF
FF FF 00 FF
00 FF FF FF
80 00 FF FF
FF FF FF FF
00 00 00 00
90
```

实际发送时是一串连续 byte：

```text
AA 55 10 FF 00 00 FF 00 FF 00 FF 00 00 FF FF FF FF 00 FF 00 FF FF FF 80 00 FF FF FF FF FF FF 00 00 00 00 90
```

## 9. App 组包建议

建议 App 内部封装 3 个函数：

```text
setOneLed(id, r, g, b, bright)
setAllLed(r, g, b, bright)
setFrame8(colors[8], brightness[8])
```

Kotlin 组包示例：

```kotlin
fun buildFrame(cmd: Int, payload: IntArray): ByteArray {
    val out = ByteArray(2 + 1 + payload.size + 1)
    out[0] = 0xAA.toByte()
    out[1] = 0x55.toByte()
    out[2] = cmd.toByte()

    var cs = cmd and 0xFF
    for (i in payload.indices) {
        val b = payload[i] and 0xFF
        out[3 + i] = b.toByte()
        cs = cs xor b
    }

    out[out.lastIndex] = cs.toByte()
    return out
}

fun setOneLed(id: Int, r: Int, g: Int, b: Int, bright: Int): ByteArray {
    return buildFrame(
        0x01,
        intArrayOf(id, r, g, b, bright)
    )
}

fun setAllLed(r: Int, g: Int, b: Int, bright: Int): ByteArray {
    return buildFrame(
        0x02,
        intArrayOf(r, g, b, bright)
    )
}
```

JavaScript/TypeScript 组包示例：

```ts
function buildFrame(cmd: number, payload: number[]): Uint8Array {
  const out = new Uint8Array(2 + 1 + payload.length + 1);
  out[0] = 0xaa;
  out[1] = 0x55;
  out[2] = cmd & 0xff;

  let cs = cmd & 0xff;
  payload.forEach((value, index) => {
    const b = value & 0xff;
    out[3 + index] = b;
    cs ^= b;
  });

  out[out.length - 1] = cs & 0xff;
  return out;
}

function setOneLed(id: number, r: number, g: number, b: number, bright: number) {
  return buildFrame(0x01, [id, r, g, b, bright]);
}

function setAllLed(r: number, g: number, b: number, bright: number) {
  return buildFrame(0x02, [r, g, b, bright]);
}
```

## 10. 呼吸灯开发建议

呼吸灯推荐固定颜色，只连续改变亮度。

例如蓝色呼吸灯：

```text
R = 0
G = 0
B = 255
BRIGHT = 0 -> 255 -> 0
```

可以用命令 `0x02` 控制全部灯一起呼吸：

```text
AA 55 02 00 00 FF BRIGHT CS
```

每次更新只改变 `BRIGHT` 和 `CS`。

建议刷新率：

| 场景 | 建议帧率 |
| --- | --- |
| 普通呼吸 | 30 fps 左右 |
| 流水/渐变 | 30~60 fps |
| 调试阶段 | 10~20 fps，等待 ACK |

115200 波特率下，一帧 `0x10` 命令长度 36 bytes。UART 8N1 每 byte 实际占 10 bit，所以一帧约：

```text
36 * 10 / 115200 = 3.125 ms
```

理论上 60 fps 完全够用。实际 App 建议留出蓝牙链路余量，不要无间隔疯狂发送。

## 11. 呼吸亮度曲线建议

线性亮度变化可以工作，但人眼对亮度感知不是线性的。为了更自然，App 可以使用 gamma 曲线或正弦曲线。

简单正弦呼吸：

```text
bright = (sin(phase) + 1) * 127.5
```

伪代码：

```c
phase += speed;
bright = (sin(phase) + 1.0) * 127.5;
send setAllLed(0, 0, 255, bright);
```

如果想要更平滑，可以预先生成 256 个亮度值查表。

## 12. 调试步骤

1. 先使用 `bluetooth_uart_echo_test` 确认 App 发送的 byte 能被原样回传。
2. 切换 FPGA 顶层为 `ws2812_bluetooth_controller`。
3. 发送 `AA 55 02 FF 00 00 80 7D`，确认全部灯变红且亮度约一半。
4. 检查 App 是否收到 `0x06`。
5. 如果收到 `0x15`，优先检查校验值和是否发送了 ASCII 字符串。
6. 再测试单灯命令和 8 灯整帧命令。

## 13. 常见问题

### 灯不变色，但能收到 ACK

可能原因：

| 原因 | 处理 |
| --- | --- |
| `led_out` 管脚约束不对 | 检查 QSF 管脚 |
| WS2812 供电不足 | 外部供电，FPGA 和灯带共地 |
| LED 顺序理解反了 | LED0 是最靠近数据输入端的第一颗 |

### 一直收到 NAK

可能原因：

| 原因 | 处理 |
| --- | --- |
| 校验值错误 | 重新按 XOR 算法计算 |
| 发送了 ASCII 字符串 | 改为发送 byte array |
| 帧头错误 | 必须以 `0xAA 0x55` 开始 |
| 灯号超过 7 | 单灯命令 ID 必须是 `0x00~0x07` |

### 呼吸灯不够平滑

建议：

| 方法 | 说明 |
| --- | --- |
| 使用 8 bit 亮度 | 当前 FPGA 已支持 `0~255` |
| 使用正弦或 gamma 曲线 | 比线性递增更自然 |
| 提高刷新率 | 推荐 30 fps 左右 |
| 使用 `0x10` 整帧命令 | 多颗灯变化时更同步 |

