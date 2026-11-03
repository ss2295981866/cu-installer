# 未知伤亡管理器 · CU Installer

> 给不想折腾的玩家：把《Casualties Unknown》在手机上跑起来这件事，做成几个按钮。
> 解压游戏、装 BepInEx、管模组、换语言包、备份存档，顺便还能换个主题。

![首页](docs/screenshot-home.jpg)

## 它解决什么问题

PC 游戏想在安卓上跑，得先有 Winlator 类环境，再把游戏目录、BepInEx、语言包、存档
一个个摆对位置。这套工具把「摆文件」的部分自动化了：

- 选个游戏压缩包，自动解压到正确目录
- 一键装 BepInEx，模组开关就是点一下
- 语言包、存档备份/恢复都有现成界面
- 不写死路径，游戏放哪都能找得到

## 功能

| 模块 | 能做什么 |
|---|---|
| **安装游戏** | 自备 zip → 自动解压；也支持从别处选已有目录 |
| **游戏检测** | 扫描 Download / Documents / Games / 根目录 / 外置 SD，认 `CasualtiesUnknown.exe` |
| **BepInEx** | 可选安装；扫描 `plugins/`，点一下启用/禁用 |
| **语言包** | 扫描 `CasualtiesUnknown_Data/Lang/*.json`，显示键数量 |
| **存档** | `save.sv` 备份、恢复、导入导出，支持备注 |
| **外观** | 8 种主题色、自定义背景（填充/拉伸）、7 种玻璃样式 |
| **运行环境** | 检测 Bionic Hub / Winlator CN / 盖世游戏并跳转 |
| **更新与诊断** | 检查 GitHub Release 新版本并一键下载安装；导出设备信息 + 设置 + logcat 到 `Download/cu-logs/` |
| **游戏实例** | 多份游戏目录并存，一键切换；「选择游戏目录」或「安装新游戏」（zip / tar.gz / tgz / rar，带解压进度条；7z 提示不支持） |

![关于](docs/screenshot-about.jpg)

## 系统要求

- Android 8.0（API 28）及以上
- 需要授予「所有文件访问」权限（读写游戏目录用）
- 不内置游戏本体，请自行合法获取

## 下载 / 构建

直接下载：[cuinstaller-1.5.apk](https://github.com/ss2295981866/cu-installer/releases/download/v1.5/cuinstaller-1.5.apk)
（历史版本见 [Releases](https://github.com/ss2295981866/cu-installer/releases)）。
也可以自己构建，见下。

> 注意：不同人签名的 APK 不能互相覆盖安装；换签名需要先卸载旧版。

### 构建

依赖：JDK 17+、R8/d8、`zipalign`、`apksigner`、`python3`，以及 Android SDK 的 `android.jar`
（API 30，用于编译）。

```bash
# 生成自己的签名
keytool -genkeypair -keystore build/release.jks -alias cuinstaller \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass changeit -keypass changeit -dname "CN=CuInstaller"

# 构建（ANDROID_JAR / R8_JAR 指向本地文件）
ANDROID_JAR=$HOME/android-platforms/android-30/android.jar \
R8_JAR=$HOME/r8.jar \
KS=build/release.jks KSPASS=changeit \
bash build/build-java.sh
# 产物 build/out/cuinstaller.apk
```

`targetSdk ≥ 30` 要求 `resources.arsc` 未压缩且 4 字节对齐，脚本里已先 `zipalign` 再签名。

## 源码说明：Java 工程（从 APK 还原）

原 Java 工程在 2026-10-27 的容器重置里丢失，之后一直在 smali 上打补丁。
现在已用 jadx 从发布 APK 反编译出完整 Java 源码，并做了人工修复，**可以直接编译**：

- `src/java/com/rblh/cu/` — 16 个 Java 文件（应用全部代码）
- `libs/` — 第三方依赖：junrar 7.5.5（RAR 解压）、slf4j-api 1.7.36
- `base/` — 原 APK 抽出的资源与清单（含 `INTERNET` 权限，不含 dex / 签名）

反编译产物中修复过的问题（都已在源码里改好）：

- jadx 生成的 `for (final int i ...)`、`AnonymousClassNN.this` 等非法语法
- 被捕获变量的 effectively-final 问题（补了 `final` 副本）
- `importFile` 中丢失的 try/catch（恢复为线程内 try/catch）
- 若干泛型擦除、`Map.Entry` 类型、未初始化返回值等

如果发现行为与发布版不一致，欢迎提 PR。

## 目录

```
base/                 AndroidManifest.xml / resources.arsc / res / assets/bepinex.zip
build/build-java.sh   Java 构建脚本
build/repack.py       资源 + classes.dex 重新打包
docs/                 截图与开发记录
libs/                 junrar / slf4j 依赖 jar
src/java/             应用 Java 源码（16 个文件）
```

## 常见问题

**Q：为什么 APK 里没有游戏？**
A：游戏有版权，且体积 300 MB+。仓库只提供工具，游戏请自备（zip 放进 `Download/`
或点「安装」手动选）。

**Q：装好了但显示「尚未安装」？**
A：确认 `CasualtiesUnknown.exe` 在游戏目录里，且已授予「所有文件访问」；
也可以在安装页点「已安装游戏？」手动指定目录。

**Q：能和作者发布的版本互相覆盖吗？**
A：不能，签名不同。要么卸载重装，要么用同一把密钥签名。

## 许可

代码以 **MIT** 发布，见 [LICENSE](LICENSE)。第三方组件与资源见 [NOTICE.md](NOTICE.md)。

## 致谢

- [BepInEx](https://github.com/BepInEx/BepInEx)（LGPL-2.1）
- Winlator / 盖世游戏 等运行环境
- 原作者：若白令狐
