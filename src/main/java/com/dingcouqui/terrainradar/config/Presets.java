package com.dingcouqui.terrainradar.config;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 预设：植被 / 表层 / 深层，三份严格累加的完整清单。
 * 数据全部写死为方块 ID（植被 = 默认清单 + 作物；表层 = 植被 + 软地表；深层 = 表层 + 自然岩石）。
 */
public final class Presets {
    public static final String CUSTOM = "自定义";
    public static final List<String> NAMES = List.of("植被", "表层", "深层");

    /** 玩家种植的作物（#minecraft:crops 六种 + 下界疣 + 可可果）。 */
    private static final List<String> CROPS = List.of(
            "wheat", "carrots", "potatoes", "beetroots", "torchflower_crop", "pitcher_crop",
            "nether_wart", "cocoa");

    /** 表层在植被之上增加：泥土/沙子/沙砾/苔藓/雪块及其变体。 */
    private static final List<String> SURFACE = List.of(
            // 泥土族（#minecraft:dirt，含草方块）
            "dirt", "grass_block", "podzol", "coarse_dirt", "mycelium", "rooted_dirt",
            "moss_block", "pale_moss_block", "mud", "muddy_mangrove_roots",
            // 沙子（#minecraft:sand）与沙砾
            "sand", "red_sand", "suspicious_sand", "gravel", "suspicious_gravel",
            // 雪（#minecraft:snow；雪层已在默认清单）
            "snow_block", "powder_snow",
            // 表层土地
            "farmland", "dirt_path", "clay");

    /** 深层在表层之上增加：自然岩石（不含加工变体、不含矿石/基岩）。 */
    private static final List<String> DEEP = List.of(
            // base_stone_overworld
            "stone", "granite", "diorite", "andesite", "tuff", "deepslate",
            // base_stone_nether
            "netherrack", "basalt", "blackstone",
            // 自然砂岩
            "sandstone", "red_sandstone");

    private Presets() {
    }

    /** 是否为当前有效的预设名（不含「自定义」）。 */
    public static boolean isPresetName(String raw) {
        String name = raw == null ? "" : raw.trim();
        return NAMES.contains(name);
    }

    /** 解析预设名；未知返回 null。返回的 ID 均带命名空间。 */
    public static Set<String> resolve(String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        return switch (name) {
            case "植被" -> build(CROPS);
            case "表层" -> build(CROPS, SURFACE);
            case "深层" -> build(CROPS, SURFACE, DEEP);
            default -> null;
        };
    }

    @SafeVarargs
    private static Set<String> build(List<String>... extras) {
        Set<String> ids = new LinkedHashSet<>();
        for (String id : DefaultBlocks.all()) {
            ids.add(namespaced(id));
        }
        for (List<String> extra : extras) {
            for (String id : extra) {
                ids.add(namespaced(id));
            }
        }
        return ids;
    }

    private static String namespaced(String id) {
        String trimmed = id.trim().toLowerCase(Locale.ROOT);
        return trimmed.contains(":") ? trimmed : "minecraft:" + trimmed;
    }
}
