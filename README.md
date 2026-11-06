# TerrainRadar

A client-side Fabric mod for Minecraft **1.21.11** that hides the rendering of selected blocks and reveals the real terrain underneath.

## English

### Overview

TerrainRadar is a pure client-side rendering mod. It keeps the selected blocks in the world (collision, hitboxes, drops and server-side logic are untouched) but skips their rendering. Neighbor faces are rendered so hidden solid blocks do not leave see-through holes. The default list focuses on vegetation; any registered block can be added from the GUI.

### Features

- Hide any block from the built-in list, or search and tick any registered block.
- **No holes**: when a hidden block would normally cull a neighbor's face, that face is rendered instead (vanilla renderer, Fabric API Indigo and Sodium 0.8.x are all handled).
- **Light passthrough** (toggle, default on): hidden blocks no longer block skylight, and no ambient-occlusion shadow remains around hidden blocks (vanilla/Indigo and Sodium).
- Block list GUI with search, vanilla-creative-style categories, a scrollable list with scrollbar / mouse wheel / PageUp/PageDown, select all / invert, and presets.
- Three cumulative presets: **Vegetation**, **Surface**, **Deep**.
- Optional key binding for the master switch; **unbound by default**.
- Bilingual UI: English by default; Simplified Chinese for `zh_cn`, `zh_hk`, `zh_tw` and `lzh`.
- Everything is saved to a single global JSON config.

### Requirements

- Minecraft **1.21.11**
- Fabric Loader **0.17.0+** (0.20.1 used for development)
- Fabric API **0.141.6+1.21.11**
- Optional: ModMenu **17.0.1+** for the “Config” button in the mod list
- Optional: Sodium **0.8.x** and/or Iris **1.10.x** — supported with dedicated Sodium render hooks (detected automatically)

### Installation

1. Install Fabric Loader for Minecraft 1.21.11.
2. Put Fabric API and (optionally) ModMenu into the `mods` folder.
3. Put `terrainradar-<version>.jar` into the `mods` folder.
4. Launch the Fabric 1.21.11 profile.

### Usage

#### GUI

Open it from **ModMenu → TerrainRadar → Config**, or run `/terrainradar gui`.

- **Settings tab**: toggle hidden blocks and light passthrough. Click **Apply** to save; **Back** discards unsaved changes.
- **Block List tab**: search by block ID or name, cycle the category, scroll with the mouse wheel / PageUp / PageDown / scrollbar, click a row to tick it. **All** / **Invert** apply to the current filter. Choosing a preset replaces the current selection.

#### Key binding

Go to **Options → Controls → Key Binds → TerrainRadar → Toggle Hidden Blocks** and assign a key. It is **unbound by default**. The key only works in-game while no GUI is open, and shows an action-bar message when toggled.

#### Commands

| Command | Description |
| --- | --- |
| `/terrainradar` | Show current status (enabled, hidden count, light passthrough, preset, config path) |
| `/terrainradar on` / `off` / `toggle` | Switch the master switch |
| `/terrainradar light on` / `off` / `toggle` | Switch light passthrough |
| `/terrainradar add <block>` | Add a block ID to the hidden list |
| `/terrainradar remove <block>` | Remove a block ID from the hidden list |
| `/terrainradar preset <name>` | Apply a preset (`植被`/`表层`/`深层`, or English aliases `vegetation`/`surface`/`deep`) |
| `/terrainradar list` | Print the first 20 hidden block IDs |
| `/terrainradar gui` | Open the settings screen |
| `/terrainradar reload` | Reload the config from disk |

#### Presets

Presets are **cumulative** and replace the current selection. Manual edits afterwards change the preset state to **Custom**.

| Preset | Contents | Blocks |
| --- | --- | --- |
| **Vegetation** | Default list (129) + player crops: wheat, carrots, potatoes, beetroots, torchflower crop, pitcher crop, nether wart, cocoa | 137 |
| **Surface** | Vegetation + dirt/grass/podzol/coarse dirt/mycelium/rooted dirt/moss blocks, sand/red sand/suspicious sand, gravel/suspicious gravel, snow block/powder snow, farmland, dirt path, clay | 156 |
| **Deep** | Surface + natural rocks: stone family (stone, granite, diorite, andesite, tuff, deepslate), netherrack/basalt/blackstone, sandstone/red sandstone | 167 |

Deep deliberately excludes crafted variants (cobblestone, stone bricks, polished/cut/smooth blocks), ores and bedrock.

#### Categories

The block list follows the vanilla creative tabs: **All**, **Building**, **Colored**, **Natural**, **Functional**, **Redstone**, **Other**. A block can appear in more than one category; **Other** only contains blocks that match none of them. Blocks added by mods fall back to tag/ID-based classification.

### Configuration

- Path: `config/terrainradar.json` (global, applies to all worlds).
- Fields: `enabled`, `lightPassthrough`, `preset`, `hidden`.
- `preset` stores the canonical Chinese preset name (`植被`/`表层`/`深层`/`自定义`); the GUI displays it in the current language.
- Old preset names (`默认`/`仅树叶`/`仅植物`) are rejected; loading a config with one normalizes it to `自定义` without touching the hidden block list.

### Language

- Default: **English** (`en_us`).
- Simplified Chinese for `zh_cn`, `zh_hk`, `zh_tw` and `lzh` (all four display Simplified Chinese).
- Any other language falls back to English.

### Compatibility and limitations

- **Client-side only**: the server does not need this mod; server-side behavior is unchanged.
- Renderer support: vanilla renderer, Fabric API Indigo, **Sodium 0.8.x** and **Iris 1.10.x** (Sodium 0.8.7 / 0.8.15 and Iris 1.10.7 / 1.10.8 tested on 1.21.11). Embeddium is not supported.
- With Sodium installed, a dedicated `<code>terrainradar.sodium.mixins.json</code>` is enabled automatically; without Sodium it is inert, and the vanilla/Indigo path is unchanged. The Sodium hooks target three Sodium 0.8 members — if a future Sodium version changes them, hiding degrades gracefully (a warning is logged, no crash).
- Only normal blocks are guaranteed. Block entities (chests, signs, etc.) are not part of the default list and their rendering is not guaranteed.
- Light passthrough is a visual approximation using vertex brightness; it does not perform a real relight. On Sodium it uses the chunk-build light slice (render-section ±2 blocks), so extremely tall canopies may be missed.
- Hiding a whole surface layer (Surface/Deep presets) intentionally reveals whatever is behind it. v0.4.2 fixed a section-occlusion bug that could show the background sky colour through hidden ground layers; remaining openings now show real caves/aquifers/air (not a false hole). Deep presets may render more sections and cost some performance.
- No per-world configuration is provided.

### Building from source

Requirements:

- JDK 25 for the Gradle build JVM (required by Loom 1.19.3)
- JDK 21 toolchain installed
- Gradle wrapper (downloads Gradle 9.8.2 automatically)

```powershell
$env:JAVA_HOME = '<path-to-jdk-25>'
.\gradlew.bat build "-Dorg.gradle.java.installations.paths=<path-to-jdk-21>"
```

The output jar is `build/libs/terrainradar-<version>.jar`.

### Documentation

- [DESIGN.md](DESIGN.md) — design notes
- [docs/TESTING.md](docs/TESTING.md) — manual test checklist
- [GLOSSARY.md](GLOSSARY.md) — project terminology
- [docs/adr](docs/adr/) — architecture decision records

### License

This project is licensed under the **GNU General Public License v3.0**. See [LICENSE](LICENSE) for the full text.

## 中文

### 概述

TerrainRadar 是一个纯客户端渲染模组，适用于 Minecraft **1.21.11**：它隐藏指定方块的渲染，露出下面的真实地形。

方块在世界中仍然存在（碰撞、命中判定、掉落物和服务端逻辑都不变），模组只跳过它们的渲染；同时会渲染相邻方块朝向隐藏方块的面，避免出现可透视的洞。默认清单以植被为主，任意已注册方块都可以在 GUI 里搜索并勾选。

### 功能

- 隐藏内置清单中的方块，或在 GUI 中搜索并勾选任意已注册方块。
- **无洞补面**：当隐藏方块原本会剔除邻居面时，改为正常渲染该面（原版渲染器、Fabric API Indigo 与 Sodium 0.8.x 都已处理）。
- **光照穿透**（可开关，默认开）：隐藏方块不再遮挡天光，隐藏方块周围也不会残留 AO 阴影（原版/Indigo 与 Sodium 均支持）。
- 方块清单 GUI：搜索、原版创造模式风格分类、可滚动列表（滚动条 / 鼠标滚轮 / PageUp/PageDown）、全选 / 反选、预设。
- 三份严格累加的预设：**植被**、**表层**、**深层**。
- 总开关的可选按键绑定，**默认不绑定**。
- 双语界面：默认英语；`zh_cn`、`zh_hk`、`zh_tw`、`lzh` 显示简体中文。
- 所有设置保存到一份全局 JSON 配置。

### 环境要求

- Minecraft **1.21.11**
- Fabric Loader **0.17.0+**（开发使用 0.20.1）
- Fabric API **0.141.6+1.21.11**
- 可选：ModMenu **17.0.1+**，用于模组列表里的「配置」按钮
- 可选：Sodium **0.8.x** 和/或 Iris **1.10.x**——已提供 Sodium 专用渲染钩子并自动识别生效

### 安装

1. 为 Minecraft 1.21.11 安装 Fabric Loader。
2. 把 Fabric API、以及（可选）ModMenu 放进 `mods` 文件夹。
3. 把 `terrainradar-<version>.jar` 放进 `mods` 文件夹。
4. 启动 Fabric 1.21.11 配置。

### 使用

#### GUI

从 **ModMenu → TerrainRadar → 配置** 打开，或执行 `/terrainradar gui`。

- **设置标签页**：切换隐藏方块、光照穿透。点「应用」保存；点「返回」放弃未保存的修改。
- **方块清单标签页**：按方块 ID 或名称搜索，循环切换分类，用鼠标滚轮 / PageUp / PageDown / 滚动条滚动，点击行切换勾选。「全选」「反选」作用于当前筛选。选择预设会整体替换当前勾选。

#### 按键绑定

进入 **选项 → 控制 → 按键绑定 → TerrainRadar → 切换隐藏方块**，自行绑定按键。**默认不绑定**。该按键仅在游戏内且没有打开界面时生效，切换时会在 ActionBar 显示提示。

#### 命令

| 命令 | 说明 |
| --- | --- |
| `/terrainradar` | 显示当前状态（开关、隐藏数量、光照穿透、预设、配置路径） |
| `/terrainradar on` / `off` / `toggle` | 切换总开关 |
| `/terrainradar light on` / `off` / `toggle` | 切换光照穿透 |
| `/terrainradar add <block>` | 把方块 ID 加入隐藏清单 |
| `/terrainradar remove <block>` | 把方块 ID 移出隐藏清单 |
| `/terrainradar preset <name>` | 应用预设（`植被`/`表层`/`深层`，或英文别名 `vegetation`/`surface`/`deep`） |
| `/terrainradar list` | 列出前 20 个隐藏方块 ID |
| `/terrainradar gui` | 打开设置界面 |
| `/terrainradar reload` | 从磁盘重新读取配置 |

#### 预设

预设**严格累加**，并整体替换当前勾选；之后手动改动会把状态变为**自定义**。

| 预设 | 内容 | 方块数 |
| --- | --- | --- |
| **植被** | 默认清单（129）+ 玩家作物：小麦、胡萝卜、马铃薯、甜菜根、火把花作物、瓶子草作物、下界疣、可可果 | 137 |
| **表层** | 植被 + 泥土/草方块/灰化土/砂土/菌丝体/缠根泥土/苔藓块、沙子/红沙/可疑的沙子、沙砾/可疑的沙砾、雪块/细雪、耕地、土径、黏土 | 156 |
| **深层** | 表层 + 自然岩石：石头族（石头、花岗岩、闪长岩、安山岩、凝灰岩、深板岩）、下界岩/玄武岩/黑石、砂岩/红砂岩 | 167 |

深层刻意不含加工变体（圆石、石砖、磨制/切制/平滑方块）、矿石和基岩。

#### 分类

方块清单跟随原版创造模式标签页：**全部**、**建筑**、**染色**、**自然**、**功能**、**红石**、**其它**。一个方块可以同时属于多个分类；**其它**只包含没有命中任何分类的方块。模组新增方块回落到 tag/ID 分类。

### 配置

- 路径：`config/terrainradar.json`（全局，对所有世界生效）。
- 字段：`enabled`、`lightPassthrough`、`preset`、`hidden`。
- `preset` 保存中文规范预设名（`植被`/`表层`/`深层`/`自定义`）；GUI 按当前语言显示。
- 旧预设名（`默认`/`仅树叶`/`仅植物`）不再接受；加载带旧名的配置时会归一化为 `自定义`，隐藏清单不受影响。

### 语言

- 默认：**英语**（`en_us`）。
- `zh_cn`、`zh_hk`、`zh_tw`、`lzh` 显示简体中文（四种语言均显示简体中文）。
- 其它语言回落英语。

### 兼容性与限制

- **纯客户端**：服务器不需要安装本模组；服务端行为不变。
- 渲染器支持：原版渲染器、Fabric API Indigo、**Sodium 0.8.x** 与 **Iris 1.10.x**（1.21.11 上实测 Sodium 0.8.7 / 0.8.15、Iris 1.10.7 / 1.10.8）。不支持 Embeddium。
- 装了 Sodium 时自动启用 `<code>terrainradar.sodium.mixins.json</code>`；没装 Sodium 时该配置完全惰性，原版/Indigo 路径不变。Sodium 钩子依赖 0.8.x 的三个成员，未来 Sodium 改签名时隐藏会优雅降级（日志告警、不崩溃）。
- 只保证普通方块；方块实体（箱子、告示牌等）不在默认清单内，也不保证其渲染被隐藏。
- 光照穿透是基于顶点亮度的视觉近似，不做真正的 relight；Sodium 下只能读到区块构建时的光照切片（渲染 section ±2 方块），极高树冠可能漏判。
- 选择“表层/深层”预设会露出被隐藏层后面的内容。v0.4.2 修复了区块遮挡剔除的 bug（此前可能透过隐藏地表看到背景天空色）；现在露出的应是真实洞穴/含水层/空气，而不是假洞。“深层”预设会让更多区块参与渲染，可能带来一定性能开销。
- 不提供每个世界单独配置。

### 从源码构建

要求：

- Gradle 构建 JVM 使用 JDK 25（Loom 1.19.3 要求）
- 安装 JDK 21 toolchain
- Gradle wrapper（会自动下载 Gradle 9.8.2）

```powershell
$env:JAVA_HOME = '<path-to-jdk-25>'
.\gradlew.bat build "-Dorg.gradle.java.installations.paths=<path-to-jdk-21>"
```

产物为 `build/libs/terrainradar-<version>.jar`。

### 文档

- [DESIGN.md](DESIGN.md) — 设计说明
- [docs/TESTING.md](docs/TESTING.md) — 手动测试清单
- [GLOSSARY.md](GLOSSARY.md) — 项目术语
- [docs/adr](docs/adr/) — 架构决策记录

### 许可证

本项目使用 **GNU General Public License v3.0** 许可证，完整文本见 [LICENSE](LICENSE)。
