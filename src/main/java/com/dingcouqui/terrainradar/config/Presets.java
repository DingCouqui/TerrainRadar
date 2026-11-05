package com.dingcouqui.terrainradar.config;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** 预设：默认清单 / 仅树叶 / 仅植物（命令与 GUI 共用）。 */
public final class Presets {
    public static final List<String> NAMES = List.of("默认", "仅树叶", "仅植物");

    private Presets() {
    }

    /** 解析预设名；未知返回 null。 */
    public static Set<String> resolve(String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        Map<String, List<String>> groups = DefaultBlocks.groups();
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "默认", "default" -> new LinkedHashSet<>(DefaultBlocks.all());
            case "仅树叶", "leaves", "leaf" -> new LinkedHashSet<>(
                    groups.getOrDefault("树叶（Leaves）", List.of()));
            case "仅植物", "plants", "plant" -> {
                Set<String> ids = new LinkedHashSet<>();
                groups.forEach((group, blocks) -> {
                    if (!group.contains("原木") && !group.contains("木与菌核") && !group.contains("菌柄")
                            && !group.contains("蘑菇方块") && !group.contains("疣块") && !group.contains("菌光体")) {
                        ids.addAll(blocks);
                    }
                });
                yield ids;
            }
            default -> null;
        };
    }
}
