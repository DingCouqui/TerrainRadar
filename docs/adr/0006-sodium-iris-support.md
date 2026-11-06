# 为 Sodium / Iris 增加一组可选渲染兼容 mixin

## Status

accepted

## 背景

v0.3.0 的隐藏逻辑只覆盖原版渲染器与 Fabric API Indigo：

- 隐藏：`BlockRenderManager.renderBlock`（原版）+ `IndigoTerrainRenderContext.bufferModel`（Indigo）；
- 补洞：`Block.shouldDrawSide`；
- 光照：`WorldRenderer.getLightmapCoordinates` + `AbstractBlockState.getAmbientOcclusionLightLevel`。

安装 Sodium 0.8.x 后，TerrainRadar 的开关只剩“树叶阴影/方块颜色变化”，方块本身完全没被隐藏。
实测日志（MC 1.21.11 + Sodium 0.8.15 + Iris 1.10.8）确认：

- `[Indigo] Different rendering plugin detected; not applying Indigo.` —— Indigo 被 Sodium 取代，`TerrainRenderContext` 通路不存在；
- 地形网格改由 Sodium 的 `ChunkBuilderMeshingTask` 逐方块调用 `BlockRenderer.renderModel(BlockStateModel, BlockState, BlockPos, BlockPos)` 构建；
- Sodium 的面剔除用 `AbstractBlockRenderContext.shouldDrawSide` + `ShapeComparisonCache` 做形状比较，**不调用**原版 `Block.shouldDrawSide`；
- 顶点亮度来自 `LightDataAccess.compute` 打包后的 packed int（bit0-3 方块光、bit4-7 天空光、bit8-11 发光、bit12-27 AO、bit28-31 标记位），**不经过** `WorldRenderer.getLightmapCoordinates`；
- 唯一仍然生效的旧钩子是 `getAmbientOcclusionLightLevel`（Sodium 的 `compute` 会调用它），所以只会改变阴影，这正是用户看到的现象。

## 决策

保留原版/Indigo 路径不动，新增一份只在 Sodium 存在时才真正生效的 mixin 配置
（`terrainradar.sodium.mixins.json`，`required: false`），四个注入点：

1. **隐藏**：`BlockRenderer.renderModel` HEAD 取消 → 隐藏清单命中的方块整个跳过（Sodium 的每方块网格入口）；
2. **补洞**：`AbstractBlockRenderContext.shouldDrawSide` HEAD 直接返回 `true` → 邻居是被隐藏方块时强制渲染该面；
3. **光照穿透**：`LightDataAccess.compute` RETURN 改写 packed 结果的天空光位为 15（复用 `LightCache` 的“上方有隐藏方块”判定），再交给 Sodium 自己的平滑光照插值；
4. **区块遮挡剔除修正（v0.4.2）**：`ChunkBuilderMeshingTask.execute` 里 `isOpaqueFullCube()` 对隐藏方块返回 `false`——否则隐藏地表仍被写进 `ChunkOcclusionData`，把所在 section 标成“封闭”，其下方 section 整段不渲染（原版路径对应 `SectionBuilder.build`，同样修正，见 `SectionBuilderMixin`）。

实现约定：

- mixin 用 `@Mixin(targets = "...", remap = false)` 字符串目标 + `@Inject(method = "...")`，**编译期不依赖 Sodium**；Sodium 不存在时目标类不会被加载，混入自然失效。Refmap 不会改写这些名字（Sodium 成员不在 Yarn 映射里），生产 jar 中已确认注解名保持原样。
- 配置带 `plugin: com.dingcouqui.terrainradar.mixin.SodiumMixinPlugin`：Sodium 不存在时在 mixin 配置加载阶段就过滤掉这些 mixin，避免 Mixin 为每个 `targets` 解析目标时在日志里留下 `@Mixin target ... was not found` 与 `Error loading class` 警告；未装 Sodium 的实例日志保持干净。
- Sodium 的类/方法签名可能随版本变化，所以该配置 `required: false`：未来 Sodium 改签名时降级为“隐藏失效 + 日志告警”，而不是启动崩溃。
- Iris 的 Sodium 兼容层也注入 `BlockRenderer.renderModel` 的 HEAD/TAIL 维护内部 `hasOverride` 标记（HEAD 置位、TAIL 复位）。我们的 HEAD 取消用 `@Inject(order = 0)`（`InjectorOrder.EARLY`）保证先于 Iris 的默认 order 执行，取消时 Iris 的 HEAD/TAIL 都不会执行，不会让标记泄漏到下一个方块。
- 刷新仍沿用 `worldRenderer.reload()`：Sodium 通过自身 mixin 在 reload 时重建 chunk builder 并重建全部区块（实测生效）；没有改造成按区块标脏，因为改动面更小、行为与原版路径一致。

## 考虑过的方案（已否决）

- **改 `LevelSlice.getBlockState` 让隐藏方块对 Sodium 视作空气**：能顺带解决剔除，但会污染 Sodium 的模型/方块实体/光照采样等所有读取方，影响面不可控。
- **只 hook `Block.shouldDrawSide`（v0.3.0 做法）**：Sodium 根本不调用它，补洞无效。
- **hook `LightPipeline.calculate` 改 `QuadLightData.lm`**：方法签名含 Sodium 自有的 `ModelQuadView`/`QuadLightData` 类型，需要额外编译依赖或反射，且粒度更粗；`LightDataAccess.compute` 只有 int 参数，最干净。
- **要求用户关闭 Sodium**：与“适配”目标矛盾。
- **把 Sodium 加进编译期依赖（`modCompileOnly`）**：会让构建依赖外部仓库与特定 Sodium 版本，违背“可选兼容、无硬依赖”的目标。

## 后果

- 兼容范围：Sodium **0.8.15-beta.1** 实测通过，**0.8.7** 的四个钩子签名一致（未单独跑整套测试）；Iris **1.10.8** 实测通过（几何构建阶段不受光影影响）。
- Sodium 下若移除/升级到不兼容版本：隐藏静默失效，日志有 `Mixin apply ... failed` 告警；不会崩溃。
- 方块实体（箱子、告示牌等）不经过 `BlockRenderer.renderModel`，仍不在隐藏范围内（与 v0.3.0 限制一致）。
- 光照穿透仍是视觉近似：Sodium 的 `LightDataAccess` 只在区块构建的 `LevelSlice`（渲染 section ±2 方块）范围内可读，极高树冠跨出该范围时可能漏判；不做真 relight。
- Iris 开启光影时，隐藏与补洞仍在区块网格构建阶段生效；光照穿透是否可见取决于光影包如何使用顶点 lightmap。
- 选择“表层/深层”预设隐藏整个地表层时，会看到隐藏方块后方的洞穴/含水层/空气（这是“隐藏方块”语义的必然结果，与 ADR 0005 的“深层大面积透空是预期”一致）。

### v0.4.2：天空色缺口的真正原因（此前误判为“视线穿透”）

v0.4.0 的“表层/深层”画面里有天空色缺口，最初误判为“掠射视线穿过原地表空腔”的几何现象。用户用定点实验推翻了它：`/tp -3095 78.3` 与 `/tp -3095 78.4` 垂直向下看，角度相同，但 78.4 出现缺口。原因：

- 眼睛高度 = 脚底 + 1.62；78.3 → 79.92（section 64–79），78.4 → 80.02（section 80–95），**恰好跨过 section 边界 y=80**；
- 该处被隐藏的草/泥土在 section 64–79 内覆盖所有列，它们仍被写进 `ChunkOcclusionData`，把该 section 标成“向下封闭”；
- 可见的石头顶面在 section 48–63。相机在 section 64–79 时，石头 section 是相邻的“nearby section”（Sodium `OcclusionCuller.addNearbySections` 的 3×3×3 免剔除范围），仍被渲染；升到 section 80–95 后它不再属于 nearby，可见性图又穿不过 64–79 → 石头 section 整段不渲染，看到背景天空色。
- 该现象在原版（Indigo）路径同样存在（`SectionBuilder` 的 `ChunkOcclusionData` 构建逻辑相同），所以不是 Sodium 专属问题。

修复即上面的第 4 条：让隐藏方块不参与 `ChunkOcclusionData`。实测（0.4.1 → 0.4.2，同一机位、同一 world 副本）：

- Sodium 0.8.15：78.4 从“整片天空+月亮”变为正常石头地面；常规视角的天空色像素从 12,374 降到 24；
- 纯 Indigo：78.3/78.4 的天空色缺口同样消失；
- 修复后“表层/深层”仍会露出真实洞穴/空气，但不再露出背景天空色。副作用：`深层`预设把大片石头从遮挡数据里移除，可见 section 变多，可能增加渲染开销。
