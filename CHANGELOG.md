# Changelog

All notable changes to SipRing are documented here.

## [0.0.4] — 2026-10-19

Long-interval countdown display fix. `versionCode 4`.

### Fixed

- 修复自定义间隔大于 100 分钟时，如果下一次提醒被顺延到下一个提醒时段，圆环显示成 `13:15:00` 一类超长倒计时、看起来像“小时/分钟异常”的问题。
- 当提醒被顺延到下一个时段时，圆环改为显示 `下次提醒 · 明天 10:00`，并标注「已避开提醒时段」，不再显示误导性的十几个小时倒计时。
- 正常时段内的 100+ 分钟倒计时仍显示 `2:00:00` 这类完整时:分:秒。
- 倒计时文本会根据圆环内径自动缩放，避免大字体/系统字体放大时数字被裁切。
- 长时间等待时，进度环的总时长改为实际等待时长，圆环会正常从满环开始消退，而不是一直卡在满环。

### Tests

- 通过 Robolectric 原生图形渲染实际检查了 120 分钟和跨时段 13 小时两种圆环显示。
- 现有 14 个启动/主题/对话框/渲染测试继续通过，覆盖 API 26 / 29 / 31 / 33 / 34。

## [0.0.3] — 2026-10-19

Appearance and settings release. `versionCode 3`.

### Added

- 右上角版本号改为设置按钮，进入独立设置页。
- 显示模式：跟随系统 / 浅色 / 深色，切换后立即生效。
- 主题色：青碧 / 海蓝 / 雾紫 / 暖金 / 玫瑰；圆环、按钮、卡片强调色、通知颜色同步变化。
- 浅色主题完整配色：背景、卡片、输入框、权限状态、状态栏/导航栏图标。
- 关于页：应用名称、版本号、versionCode、用户隐私声明。
- 隐私声明明确：只在本地运行、不申请网络权限、不上传数据；卸载/清除数据即删除本地设置。
- 主题选择持久化到 SharedPreferences，返回主界面后自动应用。
- 新增浅色 + 海蓝主题的设置页预览截图。

### Changed

- 主界面顶部不再显示版本号；版本号移动到「设置 → 关于」。
- 通知强调色跟随主题色。
- 设置页和对话框使用各自的浅色/深色主题变体。

### Tests

- 新增浅色/海蓝、深色/雾紫两套主题的 `MainActivity` + `SettingsActivity` 启动测试。
- 全部 13 个 Robolectric 测试通过，覆盖 API 26 / 29 / 31 / 33 / 34。

## [0.0.2] — 2026-10-19

Crash-fix release. `versionCode 2`.

### Fixed

- 移除了启动路径上对 Android 10 / 11 / 12 新 API 的直接引用（`WindowInsetsController`、`android.graphics.Insets`、`WindowInsets.Type`、`setDecorFitsSystemWindows`、`AlarmManager.canScheduleExactAlarms`）。这些引用在低版本系统上可能在类校验阶段导致点击图标即闪退。
- `onServiceConnected` 现在会校验 `IBinder` 类型并处理空值，避免服务绑定异常时 `NullPointerException`。
- 前台服务启动、`startForeground` 类型、精准闹钟查询、通知前台行为全部改为兼容分支 + 反射调用，避免 API 26–33 设备直接解析不存在的方法。
- 通知渠道、计时服务、Activity 启动均补了回归测试。
- 通知渠道或 WakeLock 初始化失败时只记录日志，不再让服务创建过程拖垮启动。
- 启动阶段如果仍遇到平台/厂商异常，会显示「SipRing 启动失败」错误页，可一键复制完整堆栈，不再直接闪退。

### Added

- 基于 Robolectric 的启动回归测试，覆盖 Android 8 / 10 / 12 / 13 / 14（API 26 / 29 / 31 / 33 / 34）。
- 测试会同时创建 `MainActivity` 和 `TimerService`，验证资源加载、主题、通知渠道和绑定路径。

### Notes

- 0.0.1 的旧 APK 可以直接覆盖安装 0.0.2；两者使用同一开发签名。
- 如果 0.0.2 仍然闪退，需要真机 `logcat` 才能定位厂商定制问题；请抓取 `AndroidRuntime` / `com.sipring.app` 相关日志。

## [0.0.1] — 2026-10-19

First installable release. `versionCode 1`.

### Added

- 圆形主按钮：开始 / 暂停 / 继续，渐变进度环、水波水位和实时倒计时。
- 可自定义提醒间隔（5–180 分钟），带 15 / 30 / 45 / 60 / 90 分钟快捷选项。
- 可自定义每日提醒时段，支持跨午夜时段和全天模式。
- 前台服务常驻通知，锁屏可查看倒计时；通知内可暂停 / 继续 / 结束。
- 高优先级喝水通知，带声音、震动、「我喝了」和「暂停」操作。
- `AlarmManager.setExactAndAllowWhileIdle()` 精准闹钟兜底；权限不足时自动退化。
- 一键申请忽略电池优化，并提供通知、精准闹钟的权限状态和跳转。
- 从最近任务划掉后彻底停止：取消计时、闹钟和通知；无开机自启。
- 本地保存设置和计时状态，无网络权限，不收集数据。
- 自适应启动图标、Android 13 单色主题图标、启动页深色主题。
- 390 × 844 的 Web 互动预览和 README 截图。
- Gradle Wrapper、签名 Release 构建、GitHub Actions 构建 Artifact。
- 仓库内开发签名密钥，保证后续版本可覆盖安装。

### Known limitations

- 未在真机 / 模拟器上完成端到端测试（构建环境没有 Android 设备）。
- 强省电 ROM 仍可能延迟或拦截提醒，需要用户允许自启动 / 后台运行。
- 重启手机后不会自动恢复提醒（刻意不注册 BOOT_COMPLETED）。
