package com.dingcouqui.terrainradar.hidden;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;

/**
 * 渲染线程上的邻居方块读取辅助：避免每个面都新建 {@link BlockPos}。
 *
 * <p>混入 Sodium 的剔除判定时，每次 {@code shouldDrawSide} 都要看一个方向上的邻居状态；
 * 用线程本地可变坐标而不是 {@code pos.offset(side)}，在区块构建热路径上不产生垃圾。</p>
 */
public final class RenderProbe {
    private static final ThreadLocal<BlockPos.Mutable> MUTABLE =
            ThreadLocal.withInitial(BlockPos.Mutable::new);

    private RenderProbe() {
    }

    /** 读取 {@code pos} 沿 {@code side} 相邻位置的方块状态。 */
    public static BlockState stateAt(BlockRenderView world, BlockPos pos, Direction side) {
        BlockPos.Mutable mutable = MUTABLE.get();
        mutable.set(pos.getX(), pos.getY(), pos.getZ());
        mutable.move(side);
        return world.getBlockState(mutable);
    }
}
