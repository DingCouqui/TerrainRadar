package com.dingcouqui.terrainradar.mixin;

import com.dingcouqui.terrainradar.config.ConfigManager;
import com.dingcouqui.terrainradar.hidden.HiddenBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 补洞：如果造成遮挡的邻居方块被隐藏，就强制渲染当前面。
 * 只反转"邻居被隐藏"的情况，不要无条件放行（否则玻璃/树叶内面会重复）。
 */
@Mixin(Block.class)
public class BlockMixin {
    @Inject(method = "shouldDrawSide", at = @At("RETURN"), cancellable = true)
    private static void terrainradar$showFaceNextToHiddenBlock(
            BlockState state, BlockState otherState, Direction side, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() || !ConfigManager.get().enabled || HiddenBlocks.isEmpty()) {
            return;
        }
        if (HiddenBlocks.isHidden(otherState)) {
            cir.setReturnValue(true);
        }
    }
}
