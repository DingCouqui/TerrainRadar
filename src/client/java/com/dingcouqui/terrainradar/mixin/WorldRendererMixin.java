package com.dingcouqui.terrainradar.mixin;

import com.dingcouqui.terrainradar.config.ConfigManager;
import com.dingcouqui.terrainradar.hidden.HiddenBlocks;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 光照穿透（MVP 的视觉近似）：如果渲染位置的正上方存在被隐藏的方块（即原本被树叶遮住），
 * 就把天光分量补到 15，让下方地形像没有遮挡一样被照亮。
 *
 * <p>不做真 relight：客户端 relight 结果可能被服务端 light 包覆盖，而且开销大；
 * 这里只改顶点采样的亮度，属于"观感提亮"，在多人服务器同样有效。
 */
@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    private static final int SKY_FULL_BITS = 0xF00000;
    private static final int SCAN_LIMIT = 16;
    private static final int CACHE_LIMIT = 8192;

    private static final ThreadLocal<Long2ByteOpenHashMap> SCAN_CACHE = ThreadLocal.withInitial(() -> {
        Long2ByteOpenHashMap map = new Long2ByteOpenHashMap(1024);
        map.defaultReturnValue((byte) 127);
        return map;
    });
    private static final ThreadLocal<Integer> SCAN_EPOCH = ThreadLocal.withInitial(() -> -1);
    private static volatile int epoch;

    /** 配置变化后调用，让所有线程的扫描缓存失效。 */
    public static void invalidate() {
        epoch++;
    }

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
        if (!ConfigManager.get().enabled || !ConfigManager.get().lightPassthrough || HiddenBlocks.isEmpty()) {
            return;
        }
        Long2ByteOpenHashMap cache = SCAN_CACHE.get();
        if (SCAN_EPOCH.get() != epoch) {
            cache.clear();
            SCAN_EPOCH.set(epoch);
        }
        long key = pos.asLong();
        byte cached = cache.get(key);
        boolean covered;
        if (cached == 127) {
            covered = isCoveredByHiddenBlocks(world, pos);
            if (cache.size() >= CACHE_LIMIT) {
                cache.clear();
            }
            cache.put(key, (byte) (covered ? 1 : 0));
        } else {
            covered = cached == 1;
        }
        if (covered) {
            cir.setReturnValue(cir.getReturnValue() | SKY_FULL_BITS);
        }
    }

    private static boolean isCoveredByHiddenBlocks(BlockRenderView world, BlockPos pos) {
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (int dy = 1; dy <= SCAN_LIMIT; dy++) {
            mutable.set(pos.getX(), pos.getY() + dy, pos.getZ());
            BlockState above = world.getBlockState(mutable);
            if (above.isAir()) {
                continue;
            }
            if (HiddenBlocks.isHidden(above)) {
                return true;
            }
            if (above.getOpacity() > 0) {
                return false;
            }
        }
        return false;
    }
}
