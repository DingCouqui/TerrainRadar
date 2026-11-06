package com.dingcouqui.terrainradar.mixin;

import com.dingcouqui.terrainradar.config.ConfigManager;
import com.dingcouqui.terrainradar.hidden.HiddenBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.chunk.SectionBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 原版路径的区块遮挡剔除修正：被隐藏的方块不应参与 {@code ChunkOcclusionData}。
 *
 * <p>{@code SectionBuilder.build} 会用 {@code isOpaqueFullCube()} 决定是否
 * {@code ChunkOcclusionDataBuilder.markClosed(pos)}。隐藏方块仍然是"不透明满方块"，
 * 于是整层被隐藏的地表会把所在 section 的可见性数据标成"完全封闭"，
 * 可见性图就不会继续遍历到它下面的 section —— 而下面那个 section 里正是要显示的石头。
 * 结果就是：视线穿过被隐藏的地表层后，后面的区块根本没渲染，看到背景天空色（缺口）。</p>
 *
 * <p>这里对隐藏方块跳过 markClosed，让可见性图把被隐藏方块存在的 section 当作"能看穿"。</p>
 */
@Mixin(SectionBuilder.class)
public class SectionBuilderMixin {
    @Redirect(
            method = "build",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/BlockState;isOpaqueFullCube()Z"))
    private boolean terrainradar$hiddenBlocksDoNotOccludeSections(BlockState state) {
        if (ConfigManager.get().enabled && HiddenBlocks.isHidden(state)) {
            return false;
        }
        return state.isOpaqueFullCube();
    }
}
