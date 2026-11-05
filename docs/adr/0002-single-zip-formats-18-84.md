# 单个 zip 覆盖 1.20.2–26.1（资源包格式 18–84）

## Status

accepted

## 背景与决定

目标版本跨度里，资源包元数据规则变过三次：1.20.2 引入 `supported_formats`，1.21.6 起 JSON 严格解析，1.21.9 起改为 `min_format`/`max_format`（`supported_formats` 只接受数组），26.1 的格式号是 84.0。因为本项目**只覆盖 blockstates**、不碰物品模型（1.21.4 的物品模型重构因此与我们无关），内容层面跨版本一致，所以用一个 zip：`pack.mcmeta` 同时写 `pack_format: 18`、`supported_formats: [18, 84]`、`min_format: 18`、`max_format: 84`。

## Consequences

- 不能自定义物品栏/手持外观（物品模型保持原版），这是"单 zip + 只覆盖 blockstates"的代价，也正是物品图标在隐藏方块后仍然正常的原因。
- 1.20.2 会忽略不认识的 `min_format`/`max_format` 字段（未实测，首轮实机验证覆盖 26.1，第二轮覆盖 1.20.2）。
- 1.21.9+ 只接受数组形式的 `supported_formats`，生成器固定输出数组。
- 生成物**不含任何 Mojang 贴图、模型或原版 blockstate 文件**：blockstate 是一份自定义的无条件 `multipart`，空模型只有 `textures.particle`；因此比典型 Xray 类资源包更干净，只引用原版贴图名。
