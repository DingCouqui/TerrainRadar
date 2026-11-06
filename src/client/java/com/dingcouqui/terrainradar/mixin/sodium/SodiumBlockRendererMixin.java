package com.dingcouqui.terrainradar.mixin.sodium;

import com.dingcouqui.terrainradar.config.ConfigManager;
import com.dingcouqui.terrainradar.hidden.HiddenBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sodium 0.8 的区块网格构建入口：{@code BlockRenderer.renderModel} 负责把单个方块的模型几何写进区块缓冲区。
 *
 * <p>装了 Sodium 后，地形渲染完全绕过原版 {@code BlockRenderManager} 和 Fabric Indigo，
 * 原来的隐藏钩子（{@code BlockRenderManagerMixin} / {@code IndigoTerrainRenderContextMixin}）都不会被调用，
 * 所以这里在 Sodium 的每方块入口再拦一次：命中隐藏清单就整块跳过。</p>
 *
 * <p>用 {@code targets} 字符串而不是类引用，这样编译期不需要依赖 Sodium，
 * 运行时 Sodium 不存在时目标类也不会被加载，混入自然不生效。{@code remap = false} 是因为
 * Sodium 的成员名不在 Yarn 映射里，保持原样即可。</p>
 *
 * <p>{@code order = 0}（EARLY）确保本注入先于 Iris 的 {@code renderModel} HEAD/TAIL 注入执行：
 * Iris 在 HEAD 置位、TAIL 复位一个内部标记，如果我们在它之后取消，会跳过 TAIL 导致标记泄漏。</p>
 */
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer", remap = false)
public class SodiumBlockRendererMixin {
    @Inject(method = "renderModel", at = @At("HEAD"), cancellable = true, order = 0)
    private void terrainradar$skipHiddenBlock(
            BlockStateModel model, BlockState state, BlockPos pos, BlockPos origin, CallbackInfo ci) {
        if (ConfigManager.get().enabled && HiddenBlocks.isHidden(state)) {
            ci.cancel();
        }
    }
}
