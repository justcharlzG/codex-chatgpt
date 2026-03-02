# 安卓复读机 App（本地音频）

这是一个可安装到安卓手机的 Kotlin + Jetpack Compose 示例项目，支持：

- 选择本地文件夹（Storage Access Framework）
- 浏览文件夹里的音频文件
- 点击文件播放
- 单曲循环
- AB 复读
- 播放速度调节（0.5x - 2.0x）

## 打包与安装

1. 用 Android Studio 打开项目目录。
2. 等待 Gradle 同步完成。
3. 连接安卓手机（开启开发者模式和 USB 调试）或使用模拟器。
4. 点击 **Run app** 安装运行。

也可以在 Android Studio 中使用 **Build > Build Bundle(s) / APK(s) > Build APK(s)** 导出 APK。

## 使用方法

1. 启动 App 后点击“选择本地文件夹”。
2. 选择含音频文件的目录。
3. 在列表中点击音频开始播放。
4. 可开启“循环播放”。
5. 播放时可设置 A 点、B 点并开启 AB 复读。
6. 通过滑块调整播放速度。
