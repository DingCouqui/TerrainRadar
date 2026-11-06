package com.dingcouqui.terrainradar.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Sodium 兼容 mixin 配置的开关。
 *
 * <p>Sodium 不存在时，mixin 的目标类根本不会被加载；如果不在这里提前过滤，
 * Mixin 仍会为每个 {@code @Mixin(targets = ...)} 尝试解析目标并打出
 * {@code @Mixin target ... was not found} / {@code Error loading class} 警告。
 * 在配置加载阶段直接告诉 Mixin“不要应用这些 mixin”，未装 Sodium 的实例日志就保持干净。</p>
 */
public final class SodiumMixinPlugin implements IMixinConfigPlugin {
    private boolean sodiumLoaded;

    @Override
    public void onLoad(String mixinPackage) {
        sodiumLoaded = FabricLoader.getInstance().isModLoaded("sodium");
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return sodiumLoaded;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        // 无需与其他 mixin 配置协调目标
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // 无前置处理
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // 无后置处理
    }
}
