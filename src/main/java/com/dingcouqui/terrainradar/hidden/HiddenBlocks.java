package com.dingcouqui.terrainradar.hidden;

import com.dingcouqui.terrainradar.config.ConfigManager;
import com.dingcouqui.terrainradar.config.TerrainRadarConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** 运行时隐藏集合：把配置里的方块 ID 解析成 Block，渲染热路径只查 Block 集合。 */
public final class HiddenBlocks {
    private static volatile Set<Block> blocks = Collections.emptySet();
    private static volatile Set<String> missingIds = Collections.emptySet();
    private static volatile Set<String> ids = Collections.emptySet();

    private HiddenBlocks() {
    }

    public static void reload(Logger logger) {
        TerrainRadarConfig config = ConfigManager.get().normalized();
        Set<Block> resolved = new LinkedHashSet<>();
        Set<String> unresolved = new LinkedHashSet<>();
        Set<String> keptIds = new LinkedHashSet<>();
        for (String id : config.hidden) {
            Identifier identifier = Identifier.tryParse(id);
            if (identifier == null) {
                unresolved.add(id);
                continue;
            }
            Block block = Registries.BLOCK.get(identifier);
            if (block == null) {
                unresolved.add(id);
                continue;
            }
            resolved.add(block);
            keptIds.add(id);
        }
        blocks = Collections.unmodifiableSet(resolved);
        ids = Collections.unmodifiableSet(keptIds);
        missingIds = Collections.unmodifiableSet(unresolved);
        if (!unresolved.isEmpty()) {
            logger.warn("有 {} 个隐藏方块 ID 无法解析（可能未安装对应模组）：{}", unresolved.size(), unresolved);
        }
    }

    public static boolean isHidden(BlockState state) {
        return isHidden(state.getBlock());
    }

    public static boolean isHidden(Block block) {
        return !blocks.isEmpty() && blocks.contains(block);
    }

    public static boolean isEmpty() {
        return blocks.isEmpty();
    }

    public static Set<String> ids() {
        return ids;
    }

    public static Set<String> missingIds() {
        return missingIds;
    }
}
