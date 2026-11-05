package com.dingcouqui.terrainradar.mixin;

import com.dingcouqui.terrainradar.config.ConfigManager;
import com.dingcouqui.terrainradar.hidden.HiddenBlocks;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 光照穿透的一部分：隐藏方块不应在邻居上产生 AO 阴影。
 * 否则即使天光被补满，树根/树干周围仍会留一圈暗影。
 * 原版 BlockModelRenderer 与 Fabric Indigo 的 AoLuminanceFix 都会调用这个方法。
 */
@Mixin(AbstractBlock.AbstractBlockState.class)
public class BlockStateMixin {
    @Inject(method = "getAmbientOcclusionLightLevel", at = @At("HEAD"), cancellable = true)
    private void terrainradar$noAoShadowFromHiddenBlock(
            BlockView world, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (!ConfigManager.get().enabled || !ConfigManager.get().lightPassthrough) {
            return;
        }
        BlockState state = (BlockState) (Object) this;
        if (HiddenBlocks.isHidden(state)) {
            cir.setReturnValue(1.0F);
        }
    }
}
