package com.dingcouqui.terrainradar.config;

import java.util.LinkedHashSet;
import java.util.Set;

/** 全局配置的 JSON 映射（config/terrainradar.json）。 */
public final class TerrainRadarConfig {
    public int version = 1;
    public boolean enabled = true;
    public boolean lightPassthrough = true;
    public String preset = "默认";
    public Set<String> hidden = new LinkedHashSet<>();

    /** 统一 ID 格式：小写、带命名空间、去重、去掉空项。 */
    public TerrainRadarConfig normalized() {
        if (version <= 0) {
            version = 1;
        }
        Set<String> fixed = new LinkedHashSet<>();
        if (hidden != null) {
            for (String raw : hidden) {
                if (raw == null) {
                    continue;
                }
                String id = raw.trim().toLowerCase(java.util.Locale.ROOT);
                if (id.isEmpty()) {
                    continue;
                }
                if (!id.contains(":")) {
                    id = "minecraft:" + id;
                }
                fixed.add(id);
            }
        }
        hidden = fixed;
        if (preset == null || preset.isEmpty()) {
            preset = "自定义";
        }
        return this;
    }
}
