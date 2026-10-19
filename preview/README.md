# 互动效果预览

直接用浏览器打开 `index.html` 即可。它是为了在没有 Android 设备时检查视觉和交互而写的 Web 原型，不会被打包进 APK。

- 点击圆环：开始 / 暂停；
- 点击快捷间隔或「自定义」：修改提醒间隔；
- 点击 08:00 / 22:00：修改提醒时段；
- 点击权限行：切换演示状态（仅预览，不会修改系统设置）；
- 停止按钮在运行中出现在圆环下方。

## 截图参数

```text
index.html?shot=1&state=idle
index.html?shot=1&state=running&remaining=1453&interval=30
```

`shot=1` 固定为 390 × 844 无边框画布；`remaining` 单位为秒。

## 生成截图

在仓库根目录使用 Edge headless：

```powershell
& "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe" `
  --headless=new --disable-gpu --hide-scrollbars `
  --force-device-scale-factor=2 --window-size=390,844 `
  --virtual-time-budget=3000 `
  --screenshot="preview/preview-idle.png" `
  "file:///D:/IDEproject/SipRing/preview/index.html?shot=1&state=idle"
```

运行中截图把 `state=idle` 换成 `state=running&remaining=1453&interval=30`。

预览里的权限状态、通知、倒计时均为模拟；真实行为以 APK 为准。
