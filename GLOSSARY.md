# TerrainRadar

按方块清单隐藏方块渲染、显露真实地形的 Minecraft Fabric 客户端模组项目。

## Language

**客户端隐藏（Client-side Hide）**：
只改变本机客户端的渲染：方块在世界里仍然存在（碰撞、掉落、服务端判定不变），只是看不见。

**隐藏清单（Hidden Set）**：
本项目在世界中不渲染的方块集合。默认清单来自 `data/default-blocks.json`，用户可以在 GUI 里增删。

**剔除（Face Culling）**：
相邻方块之间的面不渲染的机制。隐藏实心方块会让邻居朝向它的面被剔除，形成可透视的洞；模组通过修改剔除判定消除洞。

**光照穿透（Light Passthrough）**：
隐藏方块不再参与光照遮挡，使被隐藏植被下方的地形变亮。可开关，默认开。

**按键绑定（Key Binding）**：
把「隐藏方块」总开关映射到键盘按键的机制；默认不绑定，玩家在「选项 → 控制」中自行设置，仅游戏内无界面时触发。

**默认清单（Default List）**：
模组内置、首次运行时载入的基础隐藏清单（21 个分类、129 个方块），是三份预设的共同基础。

**作物（Crops）**：
玩家可种植并收获的方块：`#minecraft:crops` 的六种（小麦、胡萝卜、马铃薯、甜菜根、火把花作物、瓶子草作物）加下界疣、可可果。

**植被预设（Vegetation Preset）**：
默认清单 + 作物的完整隐藏清单（137 个方块）。三份预设中最小的一份，仍包含树叶、原木、花草。

**表层预设（Surface Preset）**：
植被预设 + 主世界软地表（泥土/草方块及其变体、沙子、沙砾、苔藓、雪、黏土、耕地、土径）的完整隐藏清单（156 个方块）。

**深层预设（Deep Preset）**：
表层预设 + 自然岩石（石头/花岗岩/闪长岩/安山岩/凝灰岩/深板岩、下界岩/玄武岩/黑石、砂岩/红砂岩）的完整隐藏清单（167 个方块）；不含加工变体、矿石、基岩。

**预设（Preset）**：
对隐藏清单的命名组合：植被、表层、深层，三者严格累加。GUI 一键整体替换当前清单；手动改动后状态变为「自定义」。
_Avoid_: 默认、仅树叶、仅植物（旧名，已废弃）

**本地化（Localization）**：
界面文本默认英语；游戏语言为 zh_CN / zh_HK / zh_TW / lzh 时显示简体中文，其余语言回落英语。预设名在配置中保持中文规范名。

**全局配置（Global Config）**：
`config/terrainradar.json` 中的配置，对所有世界生效（本项目不做每世界配置）。

**参考实现（Reference Implementation）**：
已归档的资源包工程（`pack-legacy-v0.2.1`）与 `REFERENCE/` 下的 Xray Ultimate。只作为隐藏语义与视觉效果的参照，不再是交付形态。

**Sodium 渲染管线（Sodium Render Pipeline）**：
Sodium 用自己的区块网格构建器（`ChunkBuilderMeshingTask` → `BlockRenderer.renderModel`）与面剔除/光照采样替代原版地形渲染，并取代 Fabric Indigo。装 Sodium 时 Indigo 会输出 `Different rendering plugin detected; not applying Indigo.` 并退出，原版/Indigo 的隐藏钩子全部失效。

**Sodium 适配层（Sodium Compatibility Layer）**：
`terrainradar.sodium.mixins.json` 中的注入（隐藏、补洞、光照穿透、区块遮挡剔除），只在 Sodium 存在时生效，编译期不依赖 Sodium；`required: false`，Sodium 改版时优雅降级。
_Avoid_: 直接说“Sodium 兼容补丁”（容易和资源包/其他模组的补丁混淆）

**区块遮挡数据（ChunkOcclusionData）**：
每个 16³ section 的可见性数据，由构建网格时用 `isOpaqueFullCube()` 标记的“封闭”方块算出。隐藏方块若仍被标记封闭，其下方 section 会被整段剔除，表现为“透过隐藏地表看到背景天空色”。v0.4.2 起隐藏方块不参与该数据（原版 `SectionBuilder` 与 Sodium `ChunkBuilderMeshingTask` 两处）。
