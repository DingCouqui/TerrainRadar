package com.dingcouqui.terrainradar.mixin.sodium;

import com.dingcouqui.terrainradar.hidden.LightCache;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sodium 的顶点亮度由 {@code LightDataAccess.compute} 采样天空光/方块光后打包成顶点数据，
 * 原版路径用的 {@code WorldRenderer.getLightmapCoordinates} 钩子在 Sodium 下不会被调用。
 *
 * <p>这里在打包结果返回前把天空光分量补到 15（与原版路径"渲染位置正上方有隐藏方块就补满天光"等价），
 * 再交给 Sodium 自己的平滑光照插值。打包格式（与 Sodium 一致）：bit0-3 方块光、bit4-7 天空光、
 * bit8-11 发光、bit12-27 AO、bit28-31 标记位。</p>
 */
@Mixin(targets = "net.caffeinemc.mods.sodium.client.model.light.data.LightDataAccess", remap = false)
public abstract class SodiumLightDataAccessMixin {
    private static final int SKY_LIGHT_MASK = 0xF0;
    private static final int SKY_LIGHT_FULL = 0xF0;

    @Shadow
    protected BlockRenderView level;

    @Inject(method = "compute", at = @At("RETURN"), cancellable = true)
    private void terrainradar$boostSkyLightUnderHiddenBlocks(
            int x, int y, int z, CallbackInfoReturnable<Integer> cir) {
        int packed = cir.getReturnValue();
        if (this.level == null || (packed & SKY_LIGHT_MASK) == SKY_LIGHT_FULL) {
            return;
        }
        if (LightCache.shouldBoostSkyLight(this.level, x, y, z)) {
            cir.setReturnValue((packed & ~SKY_LIGHT_MASK) | SKY_LIGHT_FULL);
        }
    }
}
