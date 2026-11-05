package com.dingcouqui.terrainradar.hidden;

import com.dingcouqui.terrainradar.config.ConfigManager;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

/**
 * 光照穿透的采样缓存与"上方是否有隐藏方块"的判定。
 *
 * <p>放在普通类而不是 mixin 里：mixin 类只允许 private 方法，非私有方法会被当作要合并进目标类的成员。
 */
public final class LightCache {
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

    private LightCache() {
    }

    /** 配置变化后调用，让所有线程的扫描缓存失效。 */
    public static void invalidate() {
        epoch++;
    }

    /** 渲染位置正上方（穿过隐藏方块）是否能看到天空；是则把天光补满。 */
    public static boolean shouldBoostSkyLight(BlockRenderView world, BlockPos pos) {
        if (!ConfigManager.get().enabled || !ConfigManager.get().lightPassthrough || HiddenBlocks.isEmpty()) {
            return false;
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
        return covered;
    }

    public static int withFullSky(int lightmapCoordinates) {
        return lightmapCoordinates | SKY_FULL_BITS;
    }

    private static boolean isCoveredByHiddenBlocks(BlockRenderView world, BlockPos pos) {
        // 注意：chunk 网格构建时传入的是 ChunkRendererRegion（3×3×3 个 section），
        // 它只覆盖有限的竖直范围，越界 getBlockState 会抛 ArrayIndexOutOfBoundsException。
        // 这里按世界高度兜底 + 捕获越界，越界位置视为"不提升"（缓存下来避免重复异常）。
        int topY = world.getBottomY() + world.getHeight() - 1;
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (int dy = 1; dy <= SCAN_LIMIT; dy++) {
            int y = pos.getY() + dy;
            if (y > topY) {
                return false;
            }
            mutable.set(pos.getX(), y, pos.getZ());
            BlockState above;
            try {
                above = world.getBlockState(mutable);
            } catch (RuntimeException e) {
                return false;
            }
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
