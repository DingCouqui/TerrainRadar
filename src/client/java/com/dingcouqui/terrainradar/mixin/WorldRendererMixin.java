package com.dingcouqui.terrainradar.mixin;

import com.dingcouqui.terrainradar.hidden.LightCache;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 光照穿透（MVP 的视觉近似）：如果渲染位置正上方存在被隐藏的方块（即原本被树叶遮住），
 * 就把天光分量补到 15，让下方地形像没有遮挡一样被照亮。
 *
 * <p>不做真 relight：客户端 relight 结果可能被服务端 light 包覆盖，而且开销大；
 * 这里只改顶点采样的亮度，属于"观感提亮"，在多人服务器同样有效。
 * 具体判定与缓存在 {@link LightCache}（mixin 类里只能有 private 成员）。
 */
@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Inject(
            method = "getLightmapCoordinates(Lnet/minecraft/client/render/WorldRenderer$BrightnessGetter;Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;)I",
            at = @At("RETURN"),
            cancellable = true)
    private static void terrainradar$boostSkyLightUnderHiddenBlocks(
            WorldRenderer.BrightnessGetter brightnessGetter,
            BlockRenderView world,
            BlockState state,
            BlockPos pos,
            CallbackInfoReturnable<Integer> cir) {
        if (LightCache.shouldBoostSkyLight(world, pos)) {
            cir.setReturnValue(LightCache.withFullSky(cir.getReturnValue()));
        }
    }
}
