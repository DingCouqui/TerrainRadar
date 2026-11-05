# TerrainRadar 设计规格（Fabric 客户端模组）

> v0.1.0-draft（2026-11-05）。资源包工程已归档到 `_legacy-pack-export/`（git tag `pack-legacy-v0.2.1`），由项目所有者剪切走。
> 本文件描述 mod 的设计；实现细节以本文件、`GLOSSARY.md`、`docs/adr/` 为准。

## 1. 目标

按可管理的方块清单隐藏指定方块的渲染，显露真实地形。相对资源包，模组必须解决资源包做不到的三件事：

1. **无洞**：隐藏一个实心方块后，邻居朝向它的面照常渲染（资源包时代靠"去剔除覆盖"只能局部缓解）。
2. **光照穿透**：隐藏方块不再遮挡天光/方块光（可开关，默认开）。
3. **即时切换**：运行时改清单立刻生效，不用重新打包资源包。

## 2. 交付形态

- **Fabric 纯客户端模组**（`environment: client`），服务端不装也能用；不改服务端逻辑、不改碰撞与掉落。
- 先支持 **Minecraft 1.21.11**（最后一个 1.x 版本，Java 21）。后续是否跟 26.x 另议。
- 依赖：**Fabric API**、**Cloth Config**（设置页）；ModMenu 可选（方便打开配置）。

## 3. 隐藏语义

- **核心**：隐藏方块不渲染；被它遮挡的邻居面照常渲染（没有"看穿地面"的洞）。
- **光照穿透**：隐藏方块不参与光照遮挡（开关，默认开）。
- **保持原版**：碰撞箱、命中判定、破坏粒子、声音、掉落物、选中轮廓、物品栏/手持外观。
- **限制**：只支持普通方块；方块实体（箱子、告示牌等）不纳入默认清单，也不保证隐藏渲染。

## 4. 清单、配置、GUI 与命令

- **默认清单**：`src/main/resources/terrainradar/default-blocks.json`（21 个分类、129 个方块 ID，沿用资源包时代的清单）。
- **全局配置**：`config/terrainradar.json`。
- **GUI（第二阶段）**：Cloth Config 设置页（总开关、光照穿透）+ 自建「方块清单」页（搜索、分类、勾选、全选/反选、预设）；通过 **ModMenu** 打开。
- **命令**：`/terrainradar` 客户端命令（`on` / `off` / `toggle` / `gui` / `add` / `remove` / `preset` / `list` / `reload`）。
- **键位**：**不设置默认键位**（避免冲突）；GUI 通过 ModMenu 或 `/terrainradar` 命令打开。

## 5. 技术基线（均已核实）

| 项目 | 版本 |
| --- | --- |
| Minecraft | 1.21.11 |
| 映射 | Yarn `1.21.11+build.6` |
| Fabric Loader | `0.20.1`（运行时可低至 0.17.x） |
| Fabric API | `0.141.6+1.21.11` |
| Cloth Config | `21.11.153` |
| Loom | `1.19.3`（官方 1.21.11 模板的 `net.fabricmc.fabric-loom-remap`） |
| Gradle | 9.8.2 |
| JDK | 21 |

> MVP 已构建成功（`build/libs/terrainradar-0.1.0.jar`）。构建命令（本机）：
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

## 6. MVP 范围与验收

**MVP（第一阶段）**：隐藏渲染 + 补洞 + 光照穿透 + 默认清单 + JSON 配置 + 客户端命令；不含 GUI、不含默认键位。

验收标准：

- 安装后默认清单里的方块在世界中不可见，邻居面正常显示（无洞）；
- 光照穿透开关生效（开：树叶下地形变亮）；
- `/terrainradar` 命令能开关、增删方块、保存配置；
- 重进世界配置保持；
- 无明显掉帧、无崩溃、日志无 mixin 失败；
- 1.21.11 纯净 Fabric 环境通过。

**完整版（第二阶段）**：Cloth Config 设置页、自建可搜索清单页、预设、ModMenu 集成（不设默认键位）。

## 7. 测试与交付

- 我负责：构建 jar、安装/测试步骤、测试清单、问题修复。
- 项目所有者负责：把 jar 放进游戏实例（实现方不操作游戏目录）、按清单验证。
- 测试环境：**纯净 1.21.11 Fabric 实例**（Fabric API；MVP 不需要 Cloth Config/ModMenu），先不装 Sodium/Iris；兼容性验证放到 MVP 之后。

## 8. 已知风险

- **1.21.11 的精确 mixin 签名**需要实机核对（现有开源先例是 1.21.4 或更新的快照命名，不能照抄）。
- **VisGraph / SectionOcclusionGraph 整段剔除**：可能让隐藏方块所在区块整体不渲染，需试验。
- **Sodium / Iris 共存**：Sodium 需要单独的 `BlockOcclusionCache` 钩子；光影下顶点亮度方案可能失效。
- **客户端 relight**可能被服务端下发的 light 包覆盖，所以首版不做真 relight。

## 9. 文档与历史

- 资源包工程（v0.2.1、ADR 0001–0003、清单与生成脚本）在 `_legacy-pack-export/`，git tag `pack-legacy-v0.2.1` 保留了它的最后状态。
- 转向原因与取舍见 `docs/adr/0004-from-resource-pack-to-fabric-mod.md`。
