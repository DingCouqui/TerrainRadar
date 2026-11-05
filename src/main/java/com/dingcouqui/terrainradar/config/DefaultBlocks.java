package com.dingcouqui.terrainradar.config;

import com.google.gson.Gson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 内置默认清单（src/main/resources/terrainradar/default-blocks.json）。 */
public final class DefaultBlocks {
    private static final Gson GSON = new Gson();
    private static List<String> cachedBlocks;
    private static Map<String, List<String>> cachedGroups;

    private DefaultBlocks() {
    }

    private static void load() {
        if (cachedBlocks != null) {
            return;
        }
        List<String> blocks = new ArrayList<>();
        Map<String, List<String>> groups = new LinkedHashMap<>();
        try (InputStream stream = DefaultBlocks.class.getResourceAsStream("/terrainradar/default-blocks.json")) {
            if (stream == null) {
                throw new IllegalStateException("缺少资源 /terrainradar/default-blocks.json");
            }
            Root root = GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), Root.class);
            if (root != null && root.groups != null) {
                for (Group group : root.groups) {
                    if (group == null || group.blocks == null) {
                        continue;
                    }
                    groups.put(group.name == null ? "其它" : group.name, List.copyOf(group.blocks));
                    blocks.addAll(group.blocks);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("解析默认清单失败", e);
        }
        cachedBlocks = List.copyOf(blocks);
        cachedGroups = Map.copyOf(groups);
    }

    public static List<String> all() {
        load();
        return cachedBlocks;
    }

    public static Map<String, List<String>> groups() {
        load();
        return cachedGroups;
    }

    private static final class Root {
        int version;
        List<Group> groups;
    }

    private static final class Group {
        String name;
        List<String> blocks;
    }
}
