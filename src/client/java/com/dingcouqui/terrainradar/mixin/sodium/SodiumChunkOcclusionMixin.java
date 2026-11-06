package com.dingcouqui.terrainradar.mixin.sodium;

import com.dingcouqui.terrainradar.config.ConfigManager;
import com.dingcouqui.terrainradar.hidden.HiddenBlocks;
import net.minecraft.block.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Sodium 路径的区块遮挡剔除修正。
 *
 * <p>Sodium 的 {@code ChunkBuilderMeshingTask.execute} 在构建区块网格时，
 * 也用 {@code BlockState.isOpaqueFullCube()} 为 {@code ChunkOcclusionDataBuilder} 标记"封闭"位置，
 * 再把结果写进 {@code BuiltSectionInfo} 作为该 section 的可见性数据。</p>
 *
 * <p>隐藏方块仍被当作不透明满方块，会让被隐藏地表所在的 section 在可见性数据里变成"封闭"，
 * 于是后面的 section 不被渲染 → 看到天空色缺口。这里对隐藏方块返回 false，
 * 不把它标成遮挡体。</p>
 */
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask", remap = false)
public class SodiumChunkOcclusionMixin {
    @Redirect(
            method = "execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lnet/caffeinemc/mods/sodium/client/util/task/CancellationToken;)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/class_2680;method_26216()Z"),
            remap = false)
    private boolean terrainradar$hiddenBlocksDoNotOccludeSections(BlockState state) {
        if (ConfigManager.get().enabled && HiddenBlocks.isHidden(state)) {
            return false;
        }
        return state.isOpaqueFullCube();
    }
}
