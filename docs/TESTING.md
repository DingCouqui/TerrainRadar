# TerrainRadar MVP 手动测试清单（v0.1.0）

> 分工：实现方不操作游戏目录。你把 jar 放进实例的 `mods/`，按下面步骤验证。
> 依赖：Minecraft **1.21.11**、Fabric Loader（≥0.17）、**Fabric API**（0.141.6+1.21.11）。暂时不需要 Cloth Config（GUI 在第二阶段加）。

## 0. 交付物

- `build/libs/terrainradar-0.1.0.jar`
- 建议测试实例：`D:\Games\Minecraft\Game\.minecraft\versions\1.21.11-Build`（已有 fabric-api 0.141.6 + cloth + modmenu）或新建纯净 1.21.11 Fabric 实例
- 不要用 `1.21.11-Multi-Version`（168 个 mod，干扰判断）

## 1. 安装

1. 把 `terrainradar-0.1.0.jar` 复制到 `versions\<实例>\mods\`
2. 确认 `mods\` 里有 `fabric-api-0.141.6+1.21.11.jar`，且没有其它 xray/透视类模组
3. HMCL 启动该实例（Java 21），进入一个创造世界
4. 主菜单 → Mods（如果装了 ModMenu）确认 TerrainRadar 已加载；否则看日志 `logs/latest.log`

## 2. 检查项

### A. 加载

- [ ] 启动不崩溃，`logs/latest.log` 里能搜到 `TerrainRadar loaded: ... hidden block ids`
- [ ] 没有 mixin 报错（关键词：`Mixin apply failed`、`Injection failure`、`No refmap`、`Method not found`）

### B. 隐藏与补洞（核心）

- [ ] 默认清单里的树叶/花草/原木/雪层/竹子/甘蔗/南瓜等不可见（创造模式飞过森林/平原/雪原）
- [ ] **没有洞**：树干根部、雪地地表、蘑菇方块/疣块/菌光体接触面没有被"看穿"的缺口
- [ ] 树冠隐形后能直接看到下方地形
- [ ] 去掉原木后，相邻地形面正常显示（不是黑洞/天空色）

### C. 光照穿透（默认开）

- [ ] 森林下的地面明显变亮（与关闭时对比）
- [ ] 树根/树干周围**没有 AO 暗影**（`light on` 时根部与周围地面亮度一致，不再有一圈黑影）
- [ ] 用 `/terrainradar light off` 关闭后，地面恢复原来的暗度
- [ ] 夜晚/洞穴不会异常全亮（只影响"上方有隐藏方块"的位置）

### C2. GUI（v0.2.0）

依赖：**Cloth Config 21.11.153**（必需）、**ModMenu 17.0.1**（可选，装了才有模组列表里的配置按钮）。

- [ ] ModMenu 模组列表里 TerrainRadar 有「配置」按钮，点开进入中转页
- [ ] 中转页 →「设置（Cloth Config）」：隐藏方块、光照穿透两个开关，保存后立即生效
- [ ] 中转页 →「方块清单」：搜索框能过滤（按 ID 或名称）
- [ ] 分类按钮循环切换分类（全部 / 树叶 / 植物 / 原木 / 其它…），列表随分类变化
- [ ] 上一页 / 下一页可用，页码与匹配数显示正确
- [ ] 勾选/取消方块；「全选（当前筛选）」「反选」有效
- [ ] 预设「默认 / 仅树叶 / 仅植物」能替换当前勾选
- [ ] 「应用」后世界立即生效，并写回 `config/terrainradar.json`；「取消」不保存
- [ ] `/terrainradar gui` 能打开同一个中转页
- [ ] 没有设置默认键位、没有按键冲突

### D. 命令与配置

- [ ] `/terrainradar` 显示当前状态（开关、数量、光照、预设、配置路径）
- [ ] `/terrainradar off` 后所有隐藏恢复可见；`on` 后重新隐藏
- [ ] `/terrainradar toggle` 切换
- [ ] `/terrainradar add minecraft:stone` 后石头也隐形；`remove minecraft:stone` 恢复
- [ ] `/terrainradar preset 仅树叶` 后只有树叶隐形；`preset 默认` 恢复
- [ ] `/terrainradar list` 列出前 20 个方块
- [ ] `/terrainradar reload` 重新读配置
- [ ] 退出重进世界后配置保持（`config/terrainradar.json`）

### E. 回归（不应被破坏）

- [ ] 物品栏图标、手持方块、破坏粒子、声音、掉落物正常
- [ ] 碰撞仍在：走不进隐形树叶、会被隐形原木挡住
- [ ] 选中轮廓仍在（准星对着隐形方块会显示黑框）
- [ ] 地形本体（草方块/泥土/石头/雪块等）始终可见
- [ ] 水、岩浆、玻璃、箱子等其它方块正常
- [ ] 隐藏方块大量出现时帧率没有明显下降

### F. 可选：兼容性冒烟

MVP 的隐藏逻辑适配了**原版渲染器**和 Fabric API 默认的 **Indigo**（两者都已在客户端注入）。如果实例里装了 **Sodium/Embeddium**，地形渲染会绕过这两条路径，隐藏大概率不生效（待后续兼容），请以纯净实例的结果为准。

- [ ] 在装了 Sodium/Iris 的实例里启动不崩溃（效果可能不完整，记录现象即可）

## 3. 回报格式

每条写「通过 / 失败」；失败附截图 + `logs/latest.log` 片段（含 mixin 报错全文）。另外请记录：

1. 测试实例名、Fabric API 版本
2. 是否有洞、洞的位置（如果有）
3. 光照开/关的地面亮度对比（截图）
4. FPS 大致变化（主世界森林/雪地各一次）
5. 任何异常（紫黑方块、闪烁、区块整段消失、崩溃）

## 4. 测试记录

| 日期 | 环境 | 结果 |
| --- | --- | --- |
| 2026-11-05 | 1.21.11 + Fabric API 0.141.6（Indigo），纯净实例 | 通过：树叶/花草/原木/雪层/甘蔗等正确隐形；树干根部、雪地、蘑菇方块接触面**无洞**；`light on` 与 `light off` 森林地面亮度差明显 |

已通过项：A（加载）、B（隐藏与补洞）、C（光照差异）。

仍建议补测：D（命令 add/remove/preset/list/reload 与配置持久）、E（物品/碰撞/轮廓回归）、FPS 是否明显下降、Sodium/Iris 冒烟。
