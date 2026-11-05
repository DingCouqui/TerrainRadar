# TerrainRadar 方块清单

> 由 `tools/generate.py` 从 `data/blocks.json` 生成，请勿手改。版本：v0.2.1。

- 隐藏方块：**129** 个（另有 1 个旧版本 ID 别名）
- 支持版本：Java 1.20.2–26.1（单 zip，资源包格式 18–84）

## 隐藏清单

### 树叶（Leaves）（11）

  `oak_leaves`, `spruce_leaves`, `birch_leaves`, `jungle_leaves`, `acacia_leaves`, `dark_oak_leaves`
  `mangrove_leaves`, `cherry_leaves`, `azalea_leaves`, `flowering_azalea_leaves`, `pale_oak_leaves`

### 草本与枯灌木（Grasses）（5）

  `short_grass`, `tall_grass`, `fern`, `large_fern`, `dead_bush`

### 花卉（Flowers）（25）

  `dandelion`, `poppy`, `blue_orchid`, `allium`, `azure_bluet`, `red_tulip`
  `orange_tulip`, `white_tulip`, `pink_tulip`, `oxeye_daisy`, `cornflower`, `lily_of_the_valley`
  `wither_rose`, `torchflower`, `pitcher_plant`, `closed_eyeblossom`, `open_eyeblossom`, `golden_dandelion`
  `cactus_flower`, `sunflower`, `lilac`, `rose_bush`, `peony`, `pink_petals`
  `wildflowers`

### 树苗（Saplings）（10）

  `oak_sapling`, `spruce_sapling`, `birch_sapling`, `jungle_sapling`, `acacia_sapling`, `dark_oak_sapling`
  `cherry_sapling`, `pale_oak_sapling`, `mangrove_propagule`, `bamboo_sapling`

### 菌类与下界植物（Fungi & Nether Plants）（7）

  `brown_mushroom`, `red_mushroom`, `crimson_fungus`, `warped_fungus`, `nether_sprouts`, `crimson_roots`
  `warped_roots`

### 藤蔓与贴面植物（Vines & Attached Plants）（13）

  `vine`, `glow_lichen`, `hanging_roots`, `cave_vines`, `cave_vines_plant`, `weeping_vines`
  `weeping_vines_plant`, `twisting_vines`, `twisting_vines_plant`, `big_dripleaf`, `big_dripleaf_stem`, `small_dripleaf`
  `spore_blossom`

### 水生植物（Aquatic Plants）（5）

  `seagrass`, `tall_seagrass`, `kelp`, `kelp_plant`, `lily_pad`

### 雪层（Snow Layer）（1）

  `snow`

### 竹子（Bamboo）（1）

  `bamboo`

### 甘蔗 / 甜浆果 / 仙人掌（Farm Plants & Cactus）（3）

  `sugar_cane`, `sweet_berry_bush`, `cactus`

### 南瓜与西瓜（Gourds）（6）

  `pumpkin`, `melon`, `pumpkin_stem`, `melon_stem`, `attached_pumpkin_stem`, `attached_melon_stem`

### 原木（Logs）（9）

  `oak_log`, `spruce_log`, `birch_log`, `jungle_log`, `acacia_log`, `dark_oak_log`
  `mangrove_log`, `cherry_log`, `pale_oak_log`

### 木与菌核（Wood & Hyphae）（11）

  `oak_wood`, `spruce_wood`, `birch_wood`, `jungle_wood`, `acacia_wood`, `dark_oak_wood`
  `mangrove_wood`, `cherry_wood`, `pale_oak_wood`, `crimson_hyphae`, `warped_hyphae`

### 菌柄（Stems）（2）

  `crimson_stem`, `warped_stem`

### 蘑菇方块（Mushroom Blocks）（3）

  `brown_mushroom_block`, `red_mushroom_block`, `mushroom_stem`

### 疣块（Wart Blocks）（2）

  `nether_wart_block`, `warped_wart_block`

### 菌光体（Shroomlight）（1）

  `shroomlight`

### 紫颂（Chorus）（2）

  `chorus_plant`, `chorus_flower`

### 苔藓地毯 / 落叶 / 灌木 / 枯草（Ground Cover）（8）

  `moss_carpet`, `pale_moss_carpet`, `pale_hanging_moss`, `leaf_litter`, `bush`, `firefly_bush`
  `short_dry_grass`, `tall_dry_grass`

### 杜鹃花丛（Azalea Bushes）（2）

  `azalea`, `flowering_azalea`

### 红树根（Mangrove Roots）（2）

  `mangrove_roots`, `muddy_mangrove_roots`

## 旧版本 ID 别名

- `grass`：1.20.2 的草方块 ID；1.20.3 起改名为 short_grass。该文件在 1.20.3+ 会被忽略。

## 不处理（保留可见）

### 地形本体（Terrain）

  `grass_block`, `dirt`, `coarse_dirt`, `rooted_dirt`
  `podzol`, `mycelium`, `mud`, `sand`
  `red_sand`, `gravel`, `stone`, `deepslate`
  `netherrack`, `end_stone`, `snow_block`, `ice`
  `packed_ice`, `blue_ice`, `powder_snow`, `moss_block`
  `pale_moss_block`, `farmland`

### 玩家加工产物（Crafted Blocks）

  `stripped_*_log`, `stripped_*_wood`, `stripped_crimson_stem`, `stripped_warped_stem`
  `stripped_crimson_hyphae`, `stripped_warped_hyphae`, `*_planks`, `bamboo_block`
  `stripped_bamboo_block`, `carved_pumpkin`, `jack_o_lantern`

### 玩家作物（Crops）

  `wheat`, `carrots`, `potatoes`, `beetroots`
  `nether_wart`, `cocoa`, `torchflower_crop`, `pitcher_crop`

### 珊瑚（Coral，不处理）

  `*_coral`, `*_coral_block`, `*_coral_fan`, `*_coral_wall_fan`

### 幽匿（Sculk，不处理）

  `sculk`, `sculk_vein`, `sculk_catalyst`, `sculk_shrieker`
  `sculk_sensor`, `calibrated_sculk_sensor`

### 盆栽（Potted Plants，不处理）

  `potted_*`, `flower_pot`

## 去剔除覆盖（保持可见的方块）

隐藏实心方块会剔除相邻方块朝向它的面，留下可透视的洞。下面的方块复制了原版模型，
并去掉指定面的 `cullface`，让这些面永远渲染（原理同参考包让矿石保持可见的做法）。

### snow-ground（去掉 up 面的 cullface）

雪层会剔除它下方方块的上表面（遮挡形状的面 = 完整方块）。隐藏雪层后，为常常承载积雪的地表方块去掉 up 面的 cullface，让上表面照常渲染，避免雪地出现大片空洞。含石头/泥土/深板岩等山体方块（代价是这些方块每个多渲染一个面）。

  `grass_block`, `podzol`, `mycelium`, `coarse_dirt`
  `pale_moss_block`, `snow_block`, `blue_ice`, `sand`
  `red_sand`, `gravel`, `farmland`, `stone`
  `dirt`, `deepslate`, `tuff`, `calcite`
  `andesite`, `granite`, `diorite`

## 已知代价：洞（Culling Hole）

隐藏实心方块（原木、木、菌柄、菌核、蘑菇方块、疣块、菌光体、南瓜、西瓜、雪层第 8 层）会在与地形接触处留下可透视的洞，这是资源包机制无法消除的已知代价。

会产生洞的隐藏方块（32）：

  `oak_log`, `spruce_log`, `birch_log`, `jungle_log`, `acacia_log`
  `dark_oak_log`, `mangrove_log`, `cherry_log`, `pale_oak_log`, `oak_wood`
  `spruce_wood`, `birch_wood`, `jungle_wood`, `acacia_wood`, `dark_oak_wood`
  `mangrove_wood`, `cherry_wood`, `pale_oak_wood`, `crimson_hyphae`, `warped_hyphae`
  `crimson_stem`, `warped_stem`, `brown_mushroom_block`, `red_mushroom_block`, `mushroom_stem`
  `nether_wart_block`, `warped_wart_block`, `shroomlight`, `pumpkin`, `melon`
  `muddy_mangrove_roots`, `snow`

## 版本备注

- 1.20.3 起 `grass` 改名为 `short_grass`；本包同时提供 `grass.json` 供 1.20.2 使用（1.20.3+ 自动忽略）。
- 1.21.4 新增 `pale_oak_leaves`、`closed/open_eyeblossom`、`pale_moss_*`；1.21.5 新增 `leaf_litter`、`wildflowers`、`bush`、`firefly_bush`、`short/tall_dry_grass`、`cactus_flower`；26.1 新增 `golden_dandelion`。
- 旧版本会忽略不存在的方块 ID 对应的 blockstate（只记 debug 日志，不报错）。

