# 贡献指南

## 提交 Issue

请尽量包含：

- 设备型号与 Android 版本
- 复现步骤
- 应用内「更新与诊断 → 导出日志」生成的日志文件（`Download/cu-logs/`）

## 提交 PR

1. Fork 本仓库并新建分支
2. 保持代码风格与现有源码一致（Java 8，无 AndroidX 依赖，UI 以代码构建）
3. 确认 `bash build/build-java.sh` 可以成功构建
4. 在 PR 中说明改动内容与测试方式

## 开发环境

- JDK 17
- Android SDK（android.jar，API 30）
- R8（起 d8 作用）
- zipalign / apksigner

## 许可

提交的代码默认以 MIT 许可发布。
