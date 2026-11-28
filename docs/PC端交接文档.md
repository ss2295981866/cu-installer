# CU Installer 电脑端（Windows）交接文档

> 交接对象：负责开发 Windows 桌面版的同学
> 来源：Android 版 CU Installer（本仓库），作者 若白令狐
> 目标：做一个「傻瓜式」的 Windows 版《Casualties Unknown》安装/模组管理工具，框架参考 PCL（Plain Craft Launcher）

---

## 0. 一页速览

- **要做什么**：Windows 上帮玩家把游戏目录、BepInEx、模组、语言包、存档管起来，尽量一步到位。
- **不用做什么**：不需要内置游戏本体，不需要账号系统，不需要联网服务端。
- **技术路线未定**，需要先在三件事上做选择（见第 3 节）：独立 exe 还是挂在 PCL 里、是否复用 PCL 的实例/下载组件、仓库怎么放。
- **Android 版里最值钱、可直接复用的东西**：BepInEx 安装流程、模组扫描规则、存档备份规则、日志解析与中文翻译表（第 6 节）。
- **许可证红线**：Android 版代码是 MIT；「CU Installer」名称与图标归作者，衍生分发需许可；BepInEx 是 LGPL-2.1，随包分发要保留其许可与来源说明；PCL 的许可证是自定义的（GitHub 显示 `Other`），二次开发前必须先读它的 LICENSE/协议（第 8 节）。

---

## 1. 项目背景

Android 版解决的核心痛点：玩家拿到游戏压缩包后不会装 BepInEx、不知道模组放哪、存档怕丢、出错了看不懂日志。

Android 版的做法：
1. 用户自己解压游戏（1.8 起不再代替解压）
2. 选择游戏目录
3. 一键装/更新 BepInEx（内置 5.4.23.6）
4. 模组一键开关（改名 `.dll.disabled`，不删文件）
5. 语言包导入、存档备份/恢复、运行日志解析并翻译成中文

PC 端会简单很多：没有 SAF、没有 root、不需要申请权限，文件直接读写即可。**但「傻瓜式」的设计目标不变**：默认值要合理、按钮要少、出错要给人话。

---

## 2. 目标用户与场景

- 拿到游戏但不知道 BepInEx 是什么的玩家
- 装了一堆模组，某个不生效，需要看日志但看不懂英文的玩家
- 想备份存档、换设备迁移的玩家

关键体验目标：**解压 → 选目录 → 一键装 BepInEx → 开玩**，全程不需要看文档。

---

## 3. 开工前必须定下来的三件事

| 决策 | 选项 A | 选项 B | 影响 |
|---|---|---|---|
| 形态 | 独立 exe（WPF/WinForms） | 集成进 PCL（自定义主页/插件） | A 自由度高、要自己做下载/解压/更新；B 依赖 PCL 本体、跟随其更新、受其协议约束 |
| 目录管理 | 只支持单个游戏目录 | 复用 PCL 的「版本/实例」概念做多实例 | 多实例更贴近 PCL 用户习惯，但复杂度上升 |
| 代码仓库 | 和 Android 版同仓库（分 `android/` `windows/`） | 新开一个仓库 | 同仓库便于统一发版与 README；新仓库便于不同技术栈协作 |

建议（仅供参考，最终由作者决定）：
- 如果目标是「玩家用起来像 PCL 一样顺手」，选 **PCL 自定义主页/插件式**；
- 如果目标是「独立小工具、更新节奏自己控」，选 **独立 exe + 自己实现 MVP**，把 PCL 当交互参考而不是依赖。

> 注意：PCL 是 VB.NET + WPF 项目（.NET Framework 4.8 系），不是 C#。接手人需要能读写 VB.NET，否则「复用 PCL 源码」的成本会很高。

---

## 4. 领域知识（PC 端照这个来）

### 4.1 游戏目录长什么样

```
<游戏目录>/
  CasualtiesUnknown.exe
  CasualtiesUnknown_Data/
    Lang/                 ← 语言包 json 放这里
    Managed/              ← Unity 程序集
  MonoBleedingEdge/
  UnityPlayer.dll
  winhttp.dll             ← BepInEx 的 doorstop（装完才有）
  doorstop_config.ini
  .doorstop_version
  BepInEx/
    core/
    plugins/              ← 模组
    config/
    LogOutput.log         ← 运行日志
```

判断「这是不是一个有效的游戏目录」：根目录存在 `CasualtiesUnknown.exe`。

### 4.2 BepInEx 组成（5.4.23.6）

随包文件（解压到游戏根目录即可）：
- `.doorstop_version`、`doorstop_config.ini`、`winhttp.dll`
- `BepInEx/core/*.dll`（BepInEx.dll、0Harmony.dll、Mono.Cecil 等）

装完后判断是否成功：`BepInEx/core/BepInEx.dll` 存在。

版本判断优先级：
1. 自己写的标记文件 `BepInEx/.cu_version`（安装/更新时写入，内容如 `5.4.23.6`）
2. 没有标记时读 `BepInEx/LogOutput.log` 第一行里的 `BepInEx x.y.z`
3. 版本比较要按 **最多 4 段** 比（`5.4.23.5` 和 `5.4.23.6` 必须能分出大小；Android 版早期只比 3 段，出过 bug）

### 4.3 模组规则

- 模组可以是 `plugins/` 下的一个 `.dll`，也可以是一个文件夹（文件夹里放 dll，BepInEx 会加载）
- 启用/禁用 = 改名，**不删除文件**
  - `Foo.dll` ↔ `Foo.dll.disabled`
  - 文件夹：`Foo` ↔ `Foo.disabled`（或对文件夹内部 dll 统一改名，二者选一，保证可逆）
- 扫描时只认：
  - `*.dll`
  - `*.dll.disabled`
  - 目录（且目录内深度 ≤2 有 dll 才算「已启用」）
- **不要**把 `readme.txt`、`CHANGELOG.txt` 这类文件当模组（Android 版踩过：列表里出现 txt，还能被开关改名成 `xxx.txt.dll.disabled`）
- 名字形如 `CHANGELOG.txt.dll.disabled` 的假 dll 要过滤掉

### 4.4 语言包

- 位置：`<游戏目录>/CasualtiesUnknown_Data/Lang/*.json`
- 导入 = 复制 json 进去；列表 = 列出该目录下的 json
- 建议导入时校验 JSON 合法性，并统计条目数

### 4.5 存档

两个位置都要考虑，按实际存在处理：
- `%LOCALAPPDATA%Low/Orsoniks/CasualtiesUnknown/save.sv`
- BepInEx 存档模组（如 CuSaveManager）的 `BepInEx/plugins/slots/` 目录

备份建议：打包 `slots/` 目录 +（若存在）`save.sv`，命名 `yyyyMMdd_HHmmss.zip`，并允许加备注（Android 版是旁挂一个同名 `.note` 文件）。
恢复 = 解压回原位置，覆盖前提示。

### 4.6 运行日志

格式：`[Level   :   ModName] message`，Level 为 `Info/Warning/Error/Message/Debug`。

统计规则：
- 已加载插件数 = 包含 `] Loading [` 的行数
- 错误/警告 = Level 为 Error/Warning 的行
- 很多行带 `[17:34:03.750]` 这样的时间戳前缀，展示翻译时可以去掉

---

## 5. 功能清单（Android 版现状 → PC 端建议）

| 能力 | Android 版 | PC 端建议 |
|---|---|---|
| 游戏目录 | 目录选择器 + 手动输入路径 | 文件夹选择框 + 手动输入（照顾 `\\?\`、网络盘等） |
| 安装游戏 | 1.8 起：不再导入压缩包 | 直接用系统解压，或集成解压（zip/7z），但别默认替用户乱放 |
| BepInEx | 一键安装/更新，内置 5.4.23.6 | 同左；支持「仅当检测到旧版时显示更新」 |
| 模组管理 | 列表 + 开关 + 本地导入 | 同左；建议加搜索框 |
| 语言包 | 导入 + 列表 | 同左；加 JSON 校验 |
| 存档 | 备份/恢复/导入导出/备注 | 同左；PC 端可直接打开文件夹 |
| 运行日志 | 解析 + 中文翻译 + 导出 | 同左，翻译表可直接抄（第 6.3 节） |
| 多实例 | 游戏实例切换 | 可选；若挂 PCL 就用它的版本隔离 |
| 主题/外观 | 深/浅色、背景图、玻璃样式 | 弱化，PC 端跟随系统即可 |
| 更新检查 | 查 GitHub Release | 同左；可自动下载安装包 |
| 崩溃日志 | 写 `Download/cu-logs/crash-*.txt` | 写 `%LOCALAPPDATA%\CUInstaller\logs\` |
| root/权限/SAF | 需要（Android 特有） | **全部不需要** |

---

## 6. 可直接复用的规则与实现细节

### 6.1 BepInEx 安装/更新流程

1. 目标是游戏根目录
2. 解压内置 zip（注意：**解压流不要提前关闭**，Android 版踩过「只解出第一个文件就报 Stream closed」的坑）
3. 写 `BepInEx/.cu_version` = 版本号
4. 更新完成后刷新界面状态，不要等下次启动才生效

### 6.2 模组启用状态判断

```
是 dll → 已启用
是 xxx.dll.disabled → 已禁用
是目录 → 内部深度 ≤2 有 dll 则已启用，否则已禁用（空目录/误导入的空文件夹）
其它 → 直接忽略
```

### 6.3 日志翻译表（Android 版已覆盖，建议原样搬）

| 原文模式 | 中文 |
|---|---|
| `Failed to load plugin X` | 插件加载失败：X |
| `Could not load [X] because it has missing dependencies: Y` | 无法加载 [X]：缺少依赖 Y |
| `Could not load file or assembly X` | 缺少程序集/依赖：X |
| `Missing dependency: X` | 缺少依赖：X |
| `Failed to patch: ... Patching exception in method null` | 补丁失败：目标方法不存在（模组与游戏版本不匹配） |
| `patch skipped X: Patching exception in method null` | 跳过补丁 X：目标方法不存在（模组与游戏版本不匹配） |
| `AccessTools.DeclaredMethod: Could not find method for type A and name B` | 找不到方法：A.B（模组与游戏版本不匹配） |
| `AccessTools.TypeByName: Could not find type named X` | 找不到类型：X（模组与游戏版本不匹配） |
| `Mismatching version detected: TARGET: X - CURRENT: Y` | 版本不匹配：模组需要 X，当前 Y |
| `Discord RPC OnConnectionFailed ...` | Discord RPC 连接失败（不影响游戏） |
| `Lang: Failed to get <包名> translation for "key"` | 缺少翻译条目：key（语言包 <包名> 未覆盖） |
| `"uiXxx" wasnt found` | 缺少 UI 资源：uiXxx（可能是自定义界面未加载） |
| `Called UnregisterAllObjects` | 已重置全部注册对象 |
| `body cache missed X` | 肢体缓存缺失：X |
| `TRANSPILER: A B TRANSPILER SUCCESS` | 补丁注入成功：A.B |
| `TRANSPILER: A B TRANSPILER found ... N` | 补丁定位：A.B 找到目标代码（偏移 N） |
| `DEV: GENERATING TYPE IDS: BASE: X -- N -> Y` | 注册类型 ID：N -> Y（基类 X） |
| `Init failed` / `Steamworks...Init failed` | 初始化失败 / Steamworks 联机组件初始化失败 |
| `deprecated` | 插件已过时，建议更新 |
| `NullReferenceException` | 空引用异常（模组可能不兼容） |

原则：**没匹配到的行原样显示**，不要逐词替换（否则会出现「失败 to patch」这种夹生翻译）。

### 6.4 日志展示细节（体验相关）

- 弹层/面板展示，可滚动，不要一次性把上千条塞进 UI
- 连续重复的行合并为「（重复 N 次）」
- 最多展示 ~160 条，其余提示「用导出查看完整内容」
- 提供「原文 / 翻译」切换、导出为 txt
- 「缺少翻译条目」这类噪音在真实日志里可能占 70%+，建议提供「只看错误/折叠翻译缺失」

---

## 7. 数据与资源

| 资源 | 说明 |
|---|---|
| `bepinex.zip` | BepInEx 5.4.23.6（win_x64），LGPL-2.1；随包分发需保留 BepInEx 的 LICENSE 与来源链接 |
| 图标 | 作者所有，衍生分发需许可 |
| 名称 | `CU Installer`（显示名）、`未知伤亡管理器`（副标题），不要随意改名 |
| 文案 | 中文、简短、口语化；错误提示给「人话 + 建议」 |

---

## 8. 许可证与合规（务必先看）

1. **PCL**：仓库 `Meloong-Git/PCL`，GitHub 上标注的许可是 `Other`（自定义），二次开发/分发前必须读它的协议；不要移除 PCL 作者署名，不要用它做商业分发。
2. **本项目 Android 版**：MIT（代码）+ NOTICE 里的名称/图标声明。
3. **BepInEx**：LGPL-2.1。若在你的安装包里内置它的二进制，需要允许用户替换该组件、并附带其许可证与来源。
4. **不要内置游戏本体**，不要提供下载源。

---

## 9. 仓库与协作约定

- Android 版仓库：<https://github.com/ss2295981866/cu-installer>
- 电脑端建议：
  - 新技术栈就新开仓库，README 里互相链接；或在同仓库建 `windows/` 子目录
  - 许可证建议与 Android 版保持一致（MIT + NOTICE）
  - 版本号独立递增（如 `1.0.0` 起），发版必须附 Release 说明
- 提交规范：一次提交只做一件事；改功能要同步更新 README/CHANGELOG
- **不要提交签名证书、密钥、token**

---

## 10. 已知坑清单（Android 版真实踩过）

1. 解压时 `ZipInputStream` 在循环里被提前 `close()` → 只解出第一个文件，报 `Stream closed`
2. 插件列表排序时 `name == null` → `compareToIgnoreCase` 空指针崩溃（扫描时就要过滤掉无名字条目）
3. 路径含空格时，拼 shell 命令没加引号 → `mkdir: 'Unknown': Read-only file system`
4. 版本比较只取 3 段 → `5.4.23.5` 与 `5.4.23.6` 被判相等，更新按钮不出现
5. 复制文件到别的应用目录后没改属主 → 游戏读不到（PC 端无此问题，但权限/占用排查顺序可以参考：是否存在 → 是否可写 → 是否被占用）
6. 日志噪音淹没重点 → 需要折叠/过滤
7. 向导页不要太多：Android 版删掉了「选择运行环境」整页后，流程明显更顺

---

## 11. PC 端 MVP 验收清单

- [ ] 选择游戏目录，能正确识别 `CasualtiesUnknown.exe`
- [ ] 一键安装 BepInEx，装完 `BepInEx/core/BepInEx.dll` 存在，模组页可用
- [ ] 检测旧版 BepInEx 并显示「更新」，最新则不显示
- [ ] 模组列表只显示 dll/目录，开关可逆，txt 不出现
- [ ] 导入语言包到 `Lang/`，列表可见
- [ ] 备份/恢复存档，带时间戳与备注
- [ ] 运行日志能解析并中文展示，可切原文、可导出
- [ ] 出错时给人话提示，并写日志文件
- [ ] 不内置游戏，不收集数据，署名与许可证正确

---

## 12. 对外沟通口径（可直接用）

> CU Installer 是一个免费的《Casualties Unknown》安装与模组管理工具，不包含游戏本体。
> 使用流程：自己解压游戏 → 打开工具选目录 → 一键装 BepInEx → 玩。
> 模组放 `BepInEx/plugins`，语言包放 `CasualtiesUnknown_Data/Lang`，存档可以在工具里备份。
> 报错看不懂？把「运行日志」导出发给作者，里面有中文翻译。

---

## 13. 联系人

- 作者：若白令狐（Android 版维护者）
- 需求排版、文案风格、图标资源、许可证问题，均以作者回复为准
