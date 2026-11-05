package com.dingcouqui.terrainradar.mixin;

import com.dingcouqui.terrainradar.config.ConfigManager;
import com.dingcouqui.terrainradar.hidden.HiddenBlocks;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.TerrainRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric API 默认地形渲染器是 Indigo，它绕过原版 BlockRenderManager 直接 emitQuads，
 * 所以隐藏逻辑需要在这里再拦一次（区块网格构建的每方块入口）。
 */
@Mixin(TerrainRenderContext.class)
public class IndigoTerrainRenderContextMixin {
    @Inject(method = "bufferModel", at = @At("HEAD"), cancellable = true)
    private void terrainradar$skipHiddenBlock(
            BlockStateModel model, BlockState state, BlockPos pos, CallbackInfo ci) {
        if (ConfigManager.get().enabled && HiddenBlocks.isHidden(state)) {
            ci.cancel();
        }
    }
}
