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

**默认清单（Default List）**：
模组内置、首次运行时载入的隐藏清单（21 个分类、129 个方块），沿用资源包时代的清单。

**预设（Preset）**：
对隐藏清单的命名组合，例如默认清单、仅树叶、仅植物。GUI 里一键应用。

**全局配置（Global Config）**：
`config/terrainradar.json` 中的配置，对所有世界生效（本项目不做每世界配置）。

**参考实现（Reference Implementation）**：
已归档的资源包工程（`pack-legacy-v0.2.1`）与 `REFERENCE/` 下的 Xray Ultimate。只作为隐藏语义与视觉效果的参照，不再是交付形态。