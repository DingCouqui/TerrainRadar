# TerrainRadar 设计规格（Fabric 客户端模组）

> v0.4.1（2026-11-06）。v0.4.0 完成 **Sodium 0.8.x / Iris 1.10.x 适配**（第三阶段），v0.4.1 修复 `/terrainradar gui` 在聊天框输入时被 `ChatScreen` 立即关闭的问题；v0.3.0 为原版 / Indigo 基线。
> 资源包工程已归档到 `_legacy-pack-export/`（git tag `pack-legacy-v0.2.1`），由项目所有者剪切走。
> 本文件描述 mod 的设计；实现细节以本文件、`GLOSSARY.md`、`docs/adr/` 为准。

## 1. 目标

按可管理的方块清单隐藏指定方块的渲染，显露真实地形。相对资源包，模组必须解决资源包做不到的三件事：

1. **无洞**：隐藏一个实心方块后，邻居朝向它的面照常渲染（资源包时代靠"去剔除覆盖"只能局部缓解）。
2. **光照穿透**：隐藏方块不再遮挡天光/方块光（可开关，默认开）。
3. **即时切换**：运行时改清单立刻生效，不用重新打包资源包。

## 2. 交付形态

- **Fabric 纯客户端模组**（`environment: client`），服务端不装也能用；不改服务端逻辑、不改碰撞与掉落。
- 先支持 **Minecraft 1.21.11**（最后一个 1.x 版本，Java 21）。后续是否跟 26.x 另议。
- 依赖：**Fabric API**；ModMenu 可选（方便打开配置）。
- 可选兼容：**Sodium 0.8.x**（0.8.7 / 0.8.15 实测）与 **Iris 1.10.x**。Sodium 存在时自动启用一组 Sodium 专用渲染钩子（单独 mixin 配置，`required: false`，不构成硬依赖）。

## 3. 隐藏语义

- **核心**：隐藏方块不渲染；被它遮挡的邻居面照常渲染（没有"看穿地面"的洞）。
- **光照穿透**：隐藏方块不参与光照遮挡（开关，默认开）。
- **保持原版**：碰撞箱、命中判定、破坏粒子、声音、掉落物、选中轮廓、物品栏/手持外观。
- **限制**：只支持普通方块；方块实体（箱子、告示牌等）不纳入默认清单，也不保证隐藏渲染。

## 4. 清单、配置、GUI 与命令

- **默认清单**：`src/main/resources/terrainradar/default-blocks.json`（21 个分类、129 个方块 ID，沿用资源包时代的清单）。
- **全局配置**：`config/terrainradar.json`。
- **GUI（已实现）**：自建 Screen「TerrainRadar 设置」，两个标签页：**设置**（隐藏方块、光照穿透）与**方块清单**（搜索、分类、滚动列表 + 滚动条 + PageUp/Down、勾选、全选/反选、预设）；底部「应用 / 返回」，不分页。通过 **ModMenu** 的配置按钮或 `/terrainradar gui` 直接打开。
- **预设（已实现）**：`植被` = 默认清单 + 8 种玩家作物（共 137）；`表层` = 植被 + 泥土/草方块/沙/沙砾/苔藓/雪/黏土/耕地/土径（共 156）；`深层` = 表层 + 自然岩石（石头族、下界岩/玄武岩/黑石、砂岩/红砂岩，共 167）。三者严格累加；数据写死为方块 ID，不含加工变体、矿石、基岩。GUI 点预设整体替换当前勾选，手动改动后状态为「自定义」；旧预设名不再接受，旧配置加载时归一化为「自定义」并写回。
- **命令**：`/terrainradar` 客户端命令（`on` / `off` / `toggle` / `gui` / `add` / `remove` / `preset` / `list` / `reload`）。
- **键位**：提供「切换隐藏方块」按键绑定（分类 `TerrainRadar`），**默认不绑定**；玩家在「选项 → 控制」里自行设置，仅在游戏内（无界面）生效，切换时显示 ActionBar 提示。GUI 仍通过 ModMenu 或 `/terrainradar` 命令打开。
- **语言**：默认英语（`en_us`）；游戏语言为简体中文（`zh_cn`）、繁体中文-香港（`zh_hk`）、繁体中文-台湾（`zh_tw`）、文言（`lzh`）时显示**简体中文**；其它语言回落英语。ModMenu 里的模组描述统一为英语。

## 5. 技术基线（均已核实）

| 项目 | 版本 |
| --- | --- |
| Minecraft | 1.21.11 |
| 映射 | Yarn `1.21.11+build.6` |
| Fabric Loader | `0.20.1`（运行时可低至 0.17.x） |
| Fabric API | `0.141.6+1.21.11` |
| ModMenu（可选） | `17.0.1` |
| Loom | `1.19.3`（官方 1.21.11 模板的 `net.fabricmc.fabric-loom-remap`） |
| Gradle | 9.8.2 |
| JDK | 21 |
| Sodium（可选兼容） | `0.8.7` / `0.8.15-beta.1`（1.21.11） |
| Iris（可选兼容） | `1.10.7` / `1.10.8`（1.21.11） |

> v0.4.1 已构建成功（`build/libs/terrainradar-0.4.1.jar`、`build/libs/terrainradar-lite-0.4.1.jar`）。构建命令（本机）：
>
> ```powershell
> $env:JAVA_HOME='D:\Games\Minecraft\OtherClients\McHMR\java25'
> .\gradlew.bat build "-Dorg.gradle.java.installations.paths=C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot"
> ```
>
> Loom 1.19.3 要求构建 JVM 为 Java 25（Loom 自身的类要求 JVM 25），项目用 Java 21 toolchain 编译；`gradlew` 首次会下载 Gradle 9.8.2（约 150MB）。

实现路径（有开源先例，见 `docs/adr/0004`）：

- 隐藏渲染与补洞：mixin `Block.shouldDrawSide`（邻居被隐藏时反转剔除结果，只反转这一种情况）+ `BlockRenderManager.renderBlock` 整块跳过（覆盖 `cull=false` 通路）。实测反编译源码：`BlockModelRenderer.shouldDrawFace` 最终就是调用 `Block.shouldDrawSide`，不需要单独注入。
- 光照：mixin `WorldRenderer.getLightmapCoordinates`，当渲染位置正上方存在隐藏方块时把天光分量补到 15（线程本地缓存，配置变化时失效）；不做真 relight。
- 刷新：配置变化合并到下一 tick，调用 `worldRenderer.reload()`（MVP 采用简单方案；后续可换 `scheduleChunkRender*` 精确标脏）。
- 配置：`FabricLoader.getInstance().getConfigDir()` + Gson。

**Sodium / Iris 适配（v0.4.0，见 `docs/adr/0006`）**：

Sodium 用自己的区块渲染管线，原版与 Indigo 的钩子全部失效。新增 `terrainradar.sodium.mixins.json`（`@Mixin(targets = "...", remap = false)`，编译期不依赖 Sodium，`required: false`；`plugin` 在 Sodium 缺失时提前过滤，未装 Sodium 的实例日志保持干净）：

| 需求 | Sodium 注入点 | 说明 |
| --- | --- | --- |
| 隐藏方块 | `BlockRenderer.renderModel` HEAD 取消 | Sodium 的每方块网格入口；`@Inject(order = 0)` 先于 Iris 的 HEAD/TAIL，避免其 `hasOverride` 标记泄漏 |
| 补洞 | `AbstractBlockRenderContext.shouldDrawSide` HEAD 返回 `true` | Sodium 用 `ShapeComparisonCache` 自己做剔除，不调用原版 `Block.shouldDrawSide` |
| 光照穿透 | `LightDataAccess.compute` RETURN 把 packed 天空光位（bit4-7）设为 15 | 复用 `LightCache` 的“上方有隐藏方块”判定；`WorldRenderer.getLightmapCoordinates` 在 Sodium 下不参与地形 |

Sodium 不存在时目标类不会加载、钩子自然失效；未来 Sodium 改签名时配置 `required: false` 会降级为日志告警而不是崩溃。刷新仍沿用 `worldRenderer.reload()`（Sodium 会据此重建全部区块，实测生效）。

## 6. MVP 范围与验收

**MVP（第一阶段）**：隐藏渲染 + 补洞 + 光照穿透 + 默认清单 + JSON 配置 + 客户端命令；不含 GUI、不含默认键位。

验收标准：

- 安装后默认清单里的方块在世界中不可见，邻居面正常显示（无洞）；
- 光照穿透开关生效（开：树叶下地形变亮）；
- `/terrainradar` 命令能开关、增删方块、保存配置；
- 重进世界配置保持；
- 无明显掉帧、无崩溃、日志无 mixin 失败；
- 1.21.11 纯净 Fabric 环境通过。
- **2026-11-05 已在 1.21.11 + Fabric API 0.141.6（Indigo）纯净实例实测通过：隐藏、补洞（无洞）、光照穿透差异均符合预期。**

**第二阶段（已实现）**：单界面标签页（设置 / 方块清单）、可滚动清单（滚动条 / PageUp/Down）、预设（植被 / 表层 / 深层）、ModMenu 集成（不设默认键位）。

**第三阶段（v0.4.0，已实现）：Sodium / Iris 适配**。验收标准：

- Sodium 0.8.x 下，默认“植被”清单里的树叶/花草/原木/雪层等完全不可见；与关闭开关有巨大差异；
- 隐藏方块的邻居面正常显示：树干位置只剩“浅坑 + 下方泥土/草方块”，没有看穿到天空/虚空的洞；
- 光照穿透在 Sodium 下生效（`light on/off` 的地面亮度差异可辨）；
- 运行时 `/terrainradar on|off`、键位、GUI 切换后区块立即重建并生效，不卡死、不崩溃；
- 日志出现 `TerrainRadar: Sodium ... detected, Sodium chunk render hooks enabled`，无 mixin 注入失败；
- Iris 1.10.x 安装（含开启光影包）时不崩溃，隐藏与补洞仍生效；
- 未安装 Sodium 时原版/Indigo 路径行为不变（回归通过）。
- **2026-11-06 已在 1.21.11 + Fabric API 0.141.6 + Sodium 0.8.15-beta.1 + Iris 1.10.8 隔离实例实测通过**（截图对比见 `docs/TESTING.md` 测试记录）。

## 7. 测试与交付

- 我负责：构建 jar、安装/测试步骤、测试清单、问题修复。
- 项目所有者负责：把 jar 放进游戏实例（实现方不操作游戏目录）、按清单验证。
- 测试环境：**纯净 1.21.11 Fabric 实例**（Fabric API；ModMenu 可选）用于原版/Indigo 回归；**1.21.11 + Fabric API + Sodium 0.8.x + Iris 1.10.x** 实例用于兼容验证（v0.4.0 起）。验证使用隔离实例（复制版本目录 + 独立 gameDir），不直接操作项目所有者的游戏目录。

## 8. 已知风险

- **1.21.11 的精确 mixin 签名**需要实机核对（现有开源先例是 1.21.4 或更新的快照命名，不能照抄）。v0.3.0 / v0.4.0 均已实机核对。
- **VisGraph / SectionOcclusionGraph 整段剔除**：可能让隐藏方块所在区块整体不渲染；目前实测植被/表层预设未复现。
- **Sodium 版本漂移**：适配依赖 Sodium 0.8.x 的 `BlockRenderer.renderModel` / `AbstractBlockRenderContext.shouldDrawSide` / `LightDataAccess.compute` 三个成员；签名变化时钩子静默降级（`required: false` + 日志告警），不会崩溃，但隐藏会失效，需要跟版修复。
- **Iris 光影**：隐藏/补洞发生在区块网格构建阶段，与光影无关；光照穿透是顶点 lightmap 近似，光影包是否使用它取决于 shader。
- **Sodium 下的光照穿透覆盖范围**：`LightDataAccess` 只能读到区块构建的 `LevelSlice`（渲染 section ±2 方块），极高树冠跨出该范围时可能漏判“上方有隐藏方块”。
- **客户端 relight**可能被服务端下发的 light 包覆盖，所以不做真 relight。
- 隐藏“表层/深层”预设的整层地表时，会看到其后的洞穴/含水层/空气；这是隐藏语义的预期结果（见 ADR 0005），不是补洞失败。纯 Indigo 路径同样出现（原地表高度留下水平空腔，掠射视线穿出轮廓）；Sodium 下因 section 遮挡剔除仍按真实区块数据计算，缺口后显示天空色空背景，视觉上略明显。

## 9. 文档与历史

- 资源包工程（v0.2.1、ADR 0001–0003、清单与生成脚本）在 `_legacy-pack-export/`，git tag `pack-legacy-v0.2.1` 保留了它的最后状态。
- 转向原因与取舍见 `docs/adr/0004-from-resource-pack-to-fabric-mod.md`。
- Sodium / Iris 适配方案见 `docs/adr/0006-sodium-iris-support.md`。
