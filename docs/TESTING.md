# TerrainRadar 手动测试清单（v0.4.0）

> 分工：实现方不操作项目所有者的游戏目录。你把 jar 放进实例的 `mods/`，按下面步骤验证。
> 依赖：Minecraft **1.21.11**、Fabric Loader（≥0.17）、**Fabric API**（0.141.6+1.21.11）；不需要 Cloth Config。
> 可选：**Sodium 0.8.x**（0.8.7 / 0.8.15 实测）、**Iris 1.10.x**（1.10.7 / 1.10.8 实测）、ModMenu。

## 0. 交付物

- `build/libs/terrainradar-0.4.0.jar`（普通构建）
- `build/libs/terrainradar-lite-0.4.0.jar`（lite：多人会话中关闭隐藏与光照穿透）
- 建议测试实例：
  - **Sodium / Iris 兼容**：`D:\Games\Minecraft\Game\.minecraft\versions\1.21.11`（已有 fabric-api 0.141.6 + sodium 0.8.15 + iris 1.10.8 + modmenu；把 terrainradar 换成新 jar）
  - **原版 / Indigo 回归**：任一无 Sodium/Iris 的 1.21.11 Fabric 实例
- 不要用 `1.21.11-Multi-Version`（168 个 mod，干扰判断）

## 1. 安装

1. 把 `terrainradar-0.4.0.jar` 复制到 `versions\<实例>\mods\`
2. 确认 `mods\` 里有 `fabric-api-0.141.6+1.21.11.jar`，且没有其它 xray/透视类模组；测 Sodium 兼容时再确认有 `sodium-*.jar`（0.8.x）与（可选）`iris-*.jar`（1.10.x）
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

依赖：**ModMenu 17.0.1**（可选，装了才有模组列表里的配置按钮；没装用 `/terrainradar gui`）。

- [ ] ModMenu 模组列表里 TerrainRadar 的「配置」按钮**直接**打开「TerrainRadar 设置」（不再有中转页）
- [ ] 顶部两个标签页「设置 / 方块清单」可切换
- [ ] 设置页：隐藏方块、光照穿透两个勾选项；改完点「应用」生效，点「返回」不保存
- [ ] 方块清单页：搜索框按 ID / 名称过滤
- [ ] 分类按钮循环切换（全部 / 建筑方块 / 染色方块 / 自然方块 / 功能方块 / 红石方块 / 其它）
- [ ] 方块可同时属于多个分类（按原版创造模式标签页）；「其它」只含没有命中任何分类的方块
- [ ] 列表可**滚动**：鼠标滚轮、PageUp/PageDown、拖动右侧滚动条都有效，**没有分页**
- [ ] 点击行切换勾选；「全选」「反选」作用于当前筛选
- [ ] 预设「植被 / 表层 / 深层」整体替换当前勾选，数量依次为 137 / 156 / 167
- [ ] 植被含 8 种作物（小麦/胡萝卜/马铃薯/甜菜根/火把花作物/瓶子草作物/下界疣/可可果），不含耕地
- [ ] 表层含草方块/泥土/沙/沙砾/苔藓/雪/黏土/耕地/土径；不含石头
- [ ] 深层含自然岩石（石头/花岗岩/闪长岩/安山岩/凝灰岩/深板岩/下界岩/玄武岩/黑石/砂岩/红砂岩）；不含圆石等加工变体、不含矿石/基岩
- [ ] 点预设后点「应用」，`preset` 记录预设名；勾选/全选/反选后应用，`preset` 变「自定义」
- [ ] 旧配置 `preset` 为 默认/仅树叶/仅植物 时，加载后归一化为「自定义」，隐藏清单不变
- [ ] 「应用」后世界立即变化并写回 `config/terrainradar.json`；「返回」放弃未应用的修改
- [ ] `/terrainradar gui` 打开同一界面
- [ ] 默认不绑定任何按键；按键绑定可在「选项 → 控制」中自行设置，无默认冲突

### C3. 按键绑定（v0.2.0）

- [ ] 「选项 → 控制」里有 **TerrainRadar** 分类，内含「切换隐藏方块」
- [ ] 该按键**默认未绑定**（显示「未绑定/Not Bound」）
- [ ] 自行绑定一个按键后，游戏内按一下：隐藏方块总开关切换，ActionBar 显示「TerrainRadar 已开启/已关闭」
- [ ] 打开聊天/任意 GUI/搜索框时按键**不触发**（避免输入冲突）
- [ ] 重启游戏后绑定保留（原版 options 保存）
- [ ] 与 `/terrainradar toggle` 行为一致

### C4. 语言（v0.2.0）

- [ ] 默认英语（`en_us`）：GUI、命令反馈、按键绑定均为英文；ModMenu 里的描述为英文
- [ ] 简体中文（`zh_cn`）：全部界面为简体中文
- [ ] 繁体中文-香港（`zh_hk`）/ 繁体中文-台湾（`zh_tw`）/ 文言（`lzh`）：界面显示简体中文（与 `zh_cn` 相同）
- [ ] 其它语言（如 `ja_jp`）：回落英文
- [ ] 预设显示名随语言变化（Vegetation / Surface / Deep ↔ 植被 / 表层 / 深层），config 中 `preset` 仍为中文规范名
- [ ] 切换语言后界面文本立即切换（资源重载即可，无需重启）

### D. 命令与配置

- [ ] `/terrainradar` 显示当前状态（开关、数量、光照、预设、配置路径）
- [ ] `/terrainradar off` 后所有隐藏恢复可见；`on` 后重新隐藏
- [ ] `/terrainradar toggle` 切换
- [ ] `/terrainradar add minecraft:stone` 后石头也隐形；`remove minecraft:stone` 恢复
- [ ] `/terrainradar preset 植被` / `表层` / `深层` 分别应用三份预设
- [ ] `/terrainradar preset 默认` 报「未知预设」（旧名不再接受）
- [ ] `/terrainradar list` 列出前 20 个方块
- [ ] `/terrainradar reload` 重新读配置
- [ ] 退出重进世界后配置保持（`config/terrainradar.json`）

### E. 回归（不应被破坏）

- [ ] 物品栏图标、手持方块、破坏粒子、声音、掉落物正常
- [ ] 碰撞仍在：走不进隐形树叶、会被隐形原木挡住
- [ ] 选中轮廓仍在（准星对着隐形方块会显示黑框）
- [ ] 植被预设下地形本体（草方块/泥土/石头/雪块等）始终可见；表层/深层预设会按定义隐藏对应地形（预期行为）
- [ ] 水、岩浆、玻璃、箱子等其它方块正常
- [ ] 隐藏方块大量出现时帧率没有明显下降

### F. Sodium / Iris 兼容（v0.4.0）

依赖：Sodium **0.8.x**（1.21.11；0.8.7 / 0.8.15 实测）、Iris **1.10.x**（1.10.7 / 1.10.8 实测，光影包可选）。

- [ ] 启动不崩溃；日志出现 `TerrainRadar: Sodium <version> detected, Sodium chunk render hooks enabled`；装了 Iris 时还有 `TerrainRadar: Iris <version> detected`
- [ ] 日志无 `Mixin apply` / `Injection failure` / `No refmap` 等 TerrainRadar 相关报错（Iris 自身的 refmap 警告可忽略）
- [ ] 默认“植被”预设下，树叶/花草/原木/雪层/甘蔗等完全不可见；与关闭开关相比差异巨大
- [ ] **无洞**：树干位置只剩“浅坑 + 下方泥土/草方块”，其朝向隐藏方块的顶面正常渲染，没有看穿到天空/虚空的缺口
- [ ] 光照穿透生效：`light on` 时树冠下地形明显变亮，`light off` 恢复（Sodium 下由 `LightDataAccess` 补光）
- [ ] 运行时切换：`/terrainradar off` → 3~5s 内植被恢复可见；`/terrainradar on` → 3~5s 内重新隐藏；键位/GUI 应用同样生效；无卡死、无崩溃（Sodium 下会重建区块，日志有 `Stopping worker threads` / `Started 10 worker threads` 属正常）
- [ ] Iris 安装且**启用光影包**时不崩溃、不紫黑/闪烁，隐藏与补洞仍生效；光影下光照穿透是否明显取决于 shader
- [ ] “表层/深层”预设隐藏整层地表时，会看到隐藏方块后方的天空/洞穴/含水层——这是隐藏语义的预期结果，**不算洞**（与 ADR 0005 一致；实测在未装 Sodium 的纯 Indigo 路径下同样出现，机制是原地表高度留下水平空腔、掠射视线穿出到轮廓外；Sodium 的 section 遮挡剔除会让缺口后显示天空色空背景，视觉上更明显一点）。想避免该现象请用“植被”预设
- [ ] 卸载 Sodium/Iris 后，原版/Indigo 路径的隐藏、补洞、光照与“表层预设的地表透空”表现不变；**日志里不应出现 `@Mixin target net/caffeinemc/... was not found` 或 `Error loading class: net/caffeinemc/...`**（`SodiumMixinPlugin` 会在 Sodium 缺失时提前过滤这三个 mixin）

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
| 2026-11-06 | 1.21.11 + Fabric API 0.141.6 + Sodium 0.8.15-beta.1 + Iris 1.10.8，terrainradar **0.3.0** | **失败（复现 bug）**：开关只改变树叶阴影/整体明暗，树叶/原木/花草等照常渲染；日志 `[Indigo] Different rendering plugin detected; not applying Indigo.`，没有 Sodium 专用钩子 |
| 2026-11-06 | 1.21.11 + Fabric API 0.141.6 + Sodium 0.8.15-beta.1 + Iris 1.10.8，terrainradar **0.4.0**（隔离实例，1280×720 游戏内 F2 截图） | 通过：启动无 mixin 报错；日志有 `TerrainRadar: Sodium 0.8.15-beta.1+mc1.21.11 detected, Sodium chunk render hooks enabled` 与 `TerrainRadar: Iris 1.10.8+mc1.21.11 detected`；“植被”预设下植被完全隐藏且**无洞**（树干位置只剩浅坑+泥土底面）；`/terrainradar off/on` 运行时切换 3~5s 内生效、无卡死；`light off` 后画面亮度 106.38 → 102.52（Sodium 补光生效）；`use_block_face_culling=false` 对照画面几乎不变（补洞无漏面）；“表层”预设的地表透空为隐藏语义下露出的天空/洞穴，非渲染洞；Iris + Complementary Reimagined r5.9.3 下隐藏正常、无紫黑/闪烁 |
| 2026-11-06 | 1.21.11 + Fabric API 0.141.6（Indigo，移除 Sodium/Iris），terrainradar **0.4.0** | 通过（回归）：Indigo 正常注册；隐藏/补洞正常，画面与 Sodium 版 mean diff 2.4；`SodiumMixinPlugin` 过滤后日志无 `@Mixin target net/caffeinemc/... was not found` / `Error loading class` 警告 |

已通过项：A（加载）、B（隐藏与补洞）、C（光照差异）、C2（GUI）、C3（按键绑定）、C4（语言）、F（Sodium/Iris 兼容，0.4.0）。

仍建议补测：D（命令 add/remove/preset/list/reload 与配置持久，0.4.0 在 Sodium 环境下已顺带验证大多数命令）、E（物品/碰撞/轮廓回归）、FPS 是否明显下降、Iris 光影包下的长时间稳定性。
