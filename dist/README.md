# 构建产物

- `SipRing-0.0.1.apk` — 已签名的 Release APK，可直接侧载。
- `SipRing-0.0.1.apk.sha256` — SHA-256 校验值。

安装：

```bash
adb install -r SipRing-0.0.1.apk
```

当前使用 `keystore/sipring-dev.jks` 开发签名。后续 0.0.2、0.0.3 请继续使用同一密钥签名，否则 Android 会拒绝覆盖安装。
