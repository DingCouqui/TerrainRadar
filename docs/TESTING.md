# TerrainRadar 手动测试清单（v0.2.0）

> 分工：实现方**不操作游戏目录**。你只需要把 zip 复制进对应实例的 `resourcepacks/`，按下面步骤启动、观察、回报。
> 资源包只影响客户端渲染，不改存档；测试前不用备份世界。

## 0. 交付物

- `dist/TerrainRadar-v0.2.0.zip`
- 游戏目录：`D:\Games\Minecraft\Game\.minecraft`（HMCL 便携版，每版本一个隔离实例）

## 1. 首轮：26.1 纯净实例（必测）

1. 复制 `TerrainRadar-v0.2.0.zip` 到：
   `D:\Games\Minecraft\Game\.minecraft\versions\26.1\resourcepacks\`
2. 启动 HMCL（`D:\Games\Minecraft\Game\HMCL.exe`），选 **26.1** 启动（需要 Java 25；若提示 Java 不对，把 Java 路径指到 `D:\Games\Minecraft\OtherClients\McHMR\java25`）。
3. 主菜单 → 选项 → 资源包：启用 **TerrainRadar**。确认它**没有被标记 incompatible / 不兼容**。
4. 新建世界：单人游戏 → 创建新的世界 → 游戏模式**创造** → 允许作弊**开** → 世界类型默认。
5. 进入世界后按下面的检查项逐条看。

### A. 加载与日志

- [ ] 资源包列表里 TerrainRadar 正常启用、无版本不兼容提示
- [ ] `logs/latest.log` 中搜索以下关键词，**不应**出现（可截图）：
  - `Failed to load blockstate definition`
  - `Failed to load model`
  - `Missing textures in model`

  （`Discovered unknown block state definition` 在旧版本出现属正常：那是新版本才有的方块 ID 文件，被忽略掉。）

### B. 自然世界视觉

用 `/locate biome <群系>` 找到位置后 `/tp` 过去（以下命令可直接用）：

```
/locate biome minecraft:forest
/locate biome minecraft:jungle
/locate biome minecraft:snowy_plains
/locate biome minecraft:swamp
/locate biome minecraft:lush_caves
/execute in minecraft:the_nether run locate biome minecraft:crimson_forest
/execute in minecraft:the_nether run locate biome minecraft:warped_forest
/execute in minecraft:the_end run locate biome minecraft:end_highlands
```

（跨维度传送用 `/execute in <维度> run tp @s <x> <y> <z>`。）

- [ ] 树叶完全消失（橡树/云杉/白桦/丛林/金合欢/深色橡树/红树/樱花/杜鹃/苍白橡树）
- [ ] 高草、蕨、花、树苗、蘑菇（小型）、绯红/诡异菌、下界苗、菌索 消失
- [ ] 藤蔓、洞穴藤蔓（含发光浆果）、发光地衣、垂根、孢子花、垂滴叶 消失
- [ ] 竹子、甘蔗、甜浆果、仙人掌 消失
- [ ] 南瓜、西瓜、南瓜茎、西瓜茎、结果茎 消失
- [ ] 雪层消失（雪块仍在）
- [ ] 海草、高海草、海带、睡莲 消失
- [ ] 原木、木、菌柄、菌核 消失；**去皮版本与木板仍然可见**
- [ ] 蘑菇方块、蘑菇柄、下界疣块、诡异疣块、菌光体 消失
- [ ] 紫颂植物、紫颂花 消失
- [ ] 苔藓地毯、落叶堆、灌木、萤火虫灌木、矮/高枯草、杜鹃花丛、苍白苔藓地毯/垂苔 消失
- [ ] 红树根、沾泥的红树根 消失

### B2. 雪地专项（v0.2.0 新增）

v0.2.0 给 11 个承载积雪的地表方块去掉了 up 面的 `cullface`，隐藏雪层后地面不再出现大片空洞。用下面的命令传送到雪地看：

```
/locate biome minecraft:snowy_plains
/locate biome minecraft:snowy_taiga
/locate biome minecraft:ice_spikes
/locate biome minecraft:jagged_peaks
```

- [ ] 雪地地表**没有大片空洞**（雪层隐形后，草方块/雪块/沙等正常显示）
- [ ] 树干根部没有明显空洞（树干隐形后，地面照常显示）
- [ ] 悬空雪片消失（树叶隐形后，树上的积雪不再漂浮）
- [ ] 帧率没有明显下降（这些方块每个只多渲染 1 个面；明显掉帧请反馈）
- [ ] 仍然可能出现的洞（预期，不算失败）：石头/泥土/深板岩等**未覆盖方块**上的积雪、玩家建筑上的积雪、原木贴着未覆盖地形的侧面

### C. 26.1 / 1.21.x 新方块（用 /setblock 或创造物品栏）

```
/setblock ~ ~ ~2  minecraft:snow[layers=1]
/setblock ~1 ~ ~2 minecraft:snow[layers=8]
/setblock ~2 ~ ~2 minecraft:cave_vines
/setblock ~3 ~ ~2 minecraft:glow_lichen[up=true,north=true]
/setblock ~4 ~ ~2 minecraft:big_dripleaf
/setblock ~5 ~ ~2 minecraft:chorus_plant[up=true,down=true]
/setblock ~6 ~ ~2 minecraft:shroomlight
/setblock ~7 ~ ~2 minecraft:brown_mushroom_block
/setblock ~8 ~ ~2 minecraft:mushroom_stem
/setblock ~9 ~ ~2 minecraft:nether_wart_block
/setblock ~10 ~ ~2 minecraft:pumpkin
/setblock ~11 ~ ~2 minecraft:oak_log
/setblock ~12 ~ ~2 minecraft:stripped_oak_log
/setblock ~ ~ ~3  minecraft:golden_dandelion
/setblock ~1 ~ ~3 minecraft:closed_eyeblossom
/setblock ~2 ~ ~3 minecraft:pale_oak_leaves
/setblock ~3 ~ ~3 minecraft:leaf_litter
/setblock ~4 ~ ~3 minecraft:wildflowers
/setblock ~5 ~ ~3 minecraft:bush
/setblock ~6 ~ ~3 minecraft:firefly_bush
/setblock ~7 ~ ~3 minecraft:short_dry_grass
/setblock ~8 ~ ~3 minecraft:tall_dry_grass
/setblock ~9 ~ ~3 minecraft:cactus_flower
/setblock ~10 ~ ~3 minecraft:pale_moss_carpet[bottom=true]
/setblock ~11 ~ ~3 minecraft:pale_hanging_moss
```

> 注意：`glow_lichen`、`chorus_plant`、`pale_moss_carpet` 用默认状态虽然合法，但显示效果不典型（连接/底部属性为 false），所以命令里显式给了属性；`tall_dry_grass` 在 26.1 没有 `half` 属性，直接写方块名即可。

- [ ] 上述方块全部隐形（`snow[layers=1]`、`cave_vines`、`glow_lichen` 等也隐形）
- [ ] `stripped_oak_log` 仍可见

### D. 物品与交互（保持原版）

- [ ] 创造物品栏里 `oak_leaves`、`oak_log`、`dandelion` 的图标正常（没变透明、没变紫黑）
- [ ] 手持 `oak_log`、`oak_leaves` 时正常显示
- [ ] 打掉一个隐形 `oak_leaves` / `dandelion`：破坏粒子是原版贴图（不是紫黑）
- [ ] 准星对着隐形方块：**选中轮廓（黑框）仍然出现**
- [ ] 碰撞仍在：走不进隐形树叶/花丛，会被隐形原木挡住
- [ ] F3 准星仍然显示正确的方块名

### E. 预期中的"洞"（属正常，不是 bug）

下面这些位置仍可能出现**能看穿地面/墙体的小缺口**，属已知代价（v0.2.0 只修复了雪地地表等常见情况）：

- 积雪压在**石头 / 泥土 / 深板岩 / 其它未覆盖方块**上（草方块、雪块、沙、砂砾等 11 种已修复）
- 积雪压在**玩家建筑**上（木板、楼梯等不在去剔除列表里）
- 原木/菌柄贴着**未覆盖地形**的侧面（如悬崖边）
- 蘑菇方块、疣块、菌光体、南瓜/西瓜、沾泥的红树根与地形接触处（取决于接触方块的类型）
- 下界岩与疣块/菌光体接触处

- [ ] 洞只出现在上述可解释的位置 → 截图记录即可
- [ ] 雪地地表、树干根部**不应**再出现大片空洞

### F. 回归（这些必须保持可见）

- [ ] 草方块、泥土、沙、石头、深板岩、下界岩、末地石 可见
- [ ] 雪块、冰、浮冰、蓝冰、细雪 可见
- [ ] 苔藓块、苍白苔藓块、耕地 可见
- [ ] 去皮原木/木、木板、竹块、雕刻南瓜、南瓜灯、盆栽 可见
- [ ] 小麦、胡萝卜、马铃薯、甜菜根、下界疣、可可豆、火把花/猪笼草作物态 可见
- [ ] 珊瑚、珊瑚块、珊瑚扇、幽匿系列 可见
- [ ] 水、岩浆、玻璃、箱子等其它方块行为正常

### G. 回报格式

每条检查项写「通过 / 失败」，失败请附截图；另外附：

1. `logs/latest.log`（路径：`versions\26.1\logs\latest.log`），或至少搜索 A 段关键词后的截图
2. 若崩溃/报错：`crash-reports` 里的文件
3. 版本号、资源包是否正确启用

## 2. 第二轮：1.20.2 纯净实例（首轮通过后）

1. HMCL → 添加游戏 → 选 **1.20.2** 原版 → 安装（纯净，不装 mod）。
2. 复制 zip 到 `versions\<1.20.2 实例名>\resourcepacks\`，启用。
3. 新建创造世界，检查：

- [ ] 资源包未标记 incompatible
- [ ] 1.20.2 的草（ID `grass`）隐形
- [ ] 树叶、原木、花、雪层、蘑菇方块 隐形
- [ ] 去皮原木、木板、作物 可见
- [ ] 打掉草/树叶：粒子正常（若草显示紫黑粒子，属已知小问题，记录即可）
- [ ] 日志无 `Failed to load blockstate definition`

**注意**：1.20.2 的日志里可能出现 `Discovered unknown block state definition ...`（针对 `short_grass` 等新版本 ID 的文件），这是 debug 级、正常现象。

## 3. 不需要测试的内容

- 26.2 / 26.3（超出首版范围）
- 带 mod 的实例（1.20.4 有 18 个 mod、1.21.8 有 47 个、1.21.11 有 72 个，会干扰判断）
- 服务器/多人公平性（属使用规范，不是功能）
- 性能（130 个空模型 + 覆盖 blockstates 的开销可忽略）
