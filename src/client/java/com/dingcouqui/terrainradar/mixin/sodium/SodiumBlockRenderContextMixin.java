package com.dingcouqui.terrainradar.mixin.sodium;

import com.dingcouqui.terrainradar.config.ConfigManager;
import com.dingcouqui.terrainradar.hidden.HiddenBlocks;
import com.dingcouqui.terrainradar.hidden.RenderProbe;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sodium 的面剔除不使用原版 {@code Block.shouldDrawSide}，而是自己做形状比较
 * （见 {@code AbstractBlockRenderContext.shouldDrawSide} → {@code ShapeComparisonCache}），
 * 所以 {@code BlockMixin} 在 Sodium 下对补洞完全无效。
 *
 * <p>这里在 Sodium 的剔除判定入口注入：只要邻居是被隐藏的方块，就强制当前面可见。
 * 只反转"邻居被隐藏"这一种情况，和原版路径的 {@code BlockMixin} 语义一致。</p>
 */
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext", remap = false)
public abstract class SodiumBlockRenderContextMixin {
    @Shadow
    protected BlockRenderView level;

    @Shadow
    protected BlockPos pos;

    @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true)
    private void terrainradar$drawFaceNextToHiddenBlock(Direction side, CallbackInfoReturnable<Boolean> cir) {
        if (!ConfigManager.get().enabled || HiddenBlocks.isEmpty() || this.level == null || this.pos == null) {
            return;
        }
        BlockState neighbor = RenderProbe.stateAt(this.level, this.pos, side);
        if (HiddenBlocks.isHidden(neighbor)) {
            cir.setReturnValue(true);
        }
    }
}
