# Changelog

All notable changes to SipRing are documented here.

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
