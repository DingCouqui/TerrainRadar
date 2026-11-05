# TerrainRadar 设计规格

> v0.2.1（2026-11-05）。实现细节以本文件、`GLOSSARY.md` 与 `docs/adr/` 为准。
> 方块清单见 `docs/BLOCK-LIST.md`（由 `data/blocks.json` 生成）。

## 1. 目标

隐藏自然生成、遮挡视线的**地形覆盖物**，显露**真实地形**。隐藏效果是**完全隐藏**：不保留任何几何、不保留边框线。

原始需求（DESIGN.md 草稿）：

- 设计目的：隐藏地面上遮挡真实地形的方块（树木、树叶、植物、蘑菇、菌柄、疣块、菌光体、紫颂植株、紫颂花、雪片），显露真实地形。
- 隐藏效果：完全隐藏，不保留边框线。
- 隐藏方式参考实现：见 `REFERENCE/` 下的材质包。
- 支持版本：首版支持 26.1 及以下。

## 2. 交付形态

- **Java 版纯资源包**，零依赖：不需要 OptiFine、Iris 或任何 Mod，放入 `resourcepacks/` 即生效。
- 包名 `TerrainRadar`，产物 `dist/TerrainRadar-v0.1.0.zip`；**暂不公开发布**（等使用者指令）。
- 只覆盖 `blockstates` 并新增空模型；**不覆盖**任何原版贴图、方块模型、物品模型、`items/` 定义。

## 3. 隐藏清单

共 **130 个 ID**（129 个方块 + 1 个旧版本别名）。分组见 `docs/BLOCK-LIST.md`：

| 分组 | 数量 |
| --- | --- |
| 树叶 | 11 |
| 草本与枯灌木 | 5 |
| 花卉 | 25 |
| 树苗 | 10 |
| 菌类与下界植物 | 7 |
| 藤蔓与贴面植物 | 13 |
| 水生植物 | 5 |
| 雪层 | 1 |
| 竹子 | 1 |
| 甘蔗 / 甜浆果 / 仙人掌 | 3 |
| 南瓜与西瓜（含茎） | 6 |
| 原木 | 9 |
| 木与菌核（六面树皮） | 11 |
| 菌柄 | 2 |
| 蘑菇方块（含蘑菇柄） | 3 |
| 疣块 | 2 |
| 菌光体 | 1 |
| 紫颂 | 2 |
| 苔藓地毯 / 落叶 / 灌木 / 枯草 | 8 |
| 杜鹃花丛 | 2 |
| 红树根（含沾泥版本） | 2 |

## 4. 不处理（保留可见）

- **地形本体**：草方块、泥土、沙、砂砾、石头、深板岩、下界岩、末地石、雪块、冰、浮冰、蓝冰、细雪、苔藓块、苍白苔藓块、耕地等。
- **玩家加工产物**：全部去皮木/菌柄/菌核、全部木板、竹块、雕刻南瓜、南瓜灯、全部盆栽。
- **玩家作物**：小麦、胡萝卜、马铃薯、甜菜根、下界疣、可可豆、火把花/猪笼草的作物态。
- **珊瑚全系**（珊瑚、珊瑚块、珊瑚扇）与**幽匿全系**：不做处理。

## 5. 实现方式

- 每个隐藏方块生成一个 `assets/minecraft/blockstates/<id>.json`，内容是**无条件 multipart**：

  ```json
  { "multipart": [ { "apply": { "model": "minecraft:block/terrainradar/<id>" } } ] }
  ```

  `Selector.when` 可省略（代码里 `orElse(state -> true)`），因此这一条会作用到该方块的**全部状态**；不需要复制原版 blockstate 结构，也不会因版本属性变化而漏状态。
- 每个隐藏方块生成一个空模型 `assets/minecraft/models/block/terrainradar/<id>.json`：

  ```json
  { "textures": { "particle": "minecraft:block/<原版贴图>" } }
  ```

  没有 `elements`、没有几何（与 26.1 原版 `block/air.json` 同构），破坏粒子沿用原版贴图。
- **去剔除覆盖（no-cull）**：雪层会剔除它下方方块的上表面，隐藏雪层后雪地会出现大片透视空洞。为此给常承载积雪的地表方块（`data/nocull.json`，默认 19 个，含石头/泥土/深板岩等山体方块）复制原版模型并去掉 `up` 面的 `cullface`，让上表面永远渲染。原理与参考包让被包裹的矿石保持可见相同：**可见方块自己的模型不带 `cullface` 时，它的面不会被邻居剔除**。
- **全量去剔除实验包**：`tools/experiment_nocull_all.py` 可生成 26.1 专用的实验包（所有方块的所有面都不剔除，隐藏清单除外），用于实测 FPS 与视觉，不作为默认产物。
- 物品栏图标、手持、物品展示框、选中轮廓、碰撞箱、光照、方块实体渲染**全部保持原版**。
- 生成流程：`data/blocks.json` + `data/nocull.json`（真源）→ `tools/extract_particles.py`（从 26.1 资产解析破坏粒子）→ `tools/generate.py`（生成 blockstates / 空模型 / 去剔除模型 / `pack.mcmeta` / 清单文档）→ `tools/build.py`（打包）→ `tools/validate.py`（校验）。

## 6. 已知代价

- **洞（部分已缓解）**：隐藏实心方块（32 个，见清单）时，相邻方块朝向它的面会被剔除。剔除依据邻居方块状态的遮挡形状，资源包无法在「隐藏方块」一侧关闭，只能在「要显示的方块」一侧去掉 `cullface`（代价：这些面永远渲染）。当前对 19 个承载积雪的地表方块（含石头/泥土/深板岩/凝灰岩/方解石/安山岩/花岗岩/闪长岩）去掉了 up 面 cullface，雪地空洞已修复；仍可能出现的洞：**玩家建筑上的积雪**、原木与未覆盖地形的侧面接触、下界岩与疣块/菌光体的接触等。彻底方案是全量去剔除实验包（见 §5），代价是明显的渲染开销。
- **光照**：隐藏树叶不改变天光遮挡，森林下地形仍然偏暗（纯资源包无法改光照，lightmap 属 OptiFine 特性）。
- **全局生效**：资源包没有世界上下文，玩家用这些方块搭建的建筑同样会被隐藏。
- **1.20.2 的 `grass`**：旧 ID 的破坏粒子贴图名在 26.1 已改名（`grass` → `short_grass`），旧版本可能显示紫黑粒子，属已知小问题。

## 7. 版本支持

- **1.20.2–26.1，单个 zip**。`pack.mcmeta` 同时声明：

  ```json
  { "pack_format": 18, "supported_formats": [18, 84], "min_format": 18, "max_format": 84 }
  ```

  （1.20.2 起支持 `supported_formats`；1.21.9 起改用 `min_format`/`max_format`，且只有当包声明支持旧格式（min ≤ 64）时才保留 `supported_formats`；本项目 min=18 所以保留，并固定使用数组形式。1.21.6 起严格 JSON，不允许注释与尾逗号。）
- 1.20.3 起 `grass` 改名为 `short_grass`：额外生成 `grass.json` 别名供 1.20.2 使用，1.20.3+ 会忽略它。
- 1.21.4 / 1.21.5 / 26.1 新增的方块在旧版本没有对应方块，blockstate 文件会被忽略（只记 debug 日志）。

## 8. 工程结构

```
DESIGN.md                本规格
GLOSSARY.md              领域词汇
pack.mcmeta              生成物（资源包元数据）
pack.png                 生成物（图标）
data/blocks.json         唯一真源：隐藏/不处理清单、包元数据
data/nocull.json         唯一真源：去剔除覆盖（保持可见的方块与要去的面）
data/particles.json      生成物：每个方块的破坏粒子贴图
assets/minecraft/...     生成物：blockstates、空模型、去剔除模型（block/nocull）
tools/extract_particles.py
tools/generate.py
tools/make_icon.py
tools/build.py
tools/validate.py
docs/BLOCK-LIST.md       生成物：完整清单
docs/TESTING.md          手动测试清单
docs/adr/                架构决策记录
dist/                    打包产物（不进 git）
```

## 9. 测试与交付

- 实现方**不操作游戏目录**；交付 zip 与测试内容，由使用者手动复制、启动、验证。
- 首轮：**26.1 纯净实例**；第二轮：**1.20.2 纯净实例**（需新建）。
- 具体检查项、命令与回报格式见 `docs/TESTING.md`。

## 10. 后续（未做）

- 根据「全量去剔除实验包」的实测 FPS 结果，决定是否把它作为可选子包提供。
- 若仍有未覆盖方块造成空洞：按需把方块加入 `data/nocull.json`（代价是这些方块多渲染一个面）。
- 公开发布（GitHub Releases / Modrinth）、许可证、发布文案。
- 可选子包：线框版（可定位方块）、接受更多洞的"更强隐藏"版。
- 26.2+ 新方块（如 `poplar_leaves`）支持。
- 逐方块的人工粒子贴图校对（自动解析已覆盖全部 130 个 ID）。
