package com.dingcouqui.terrainradar.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

/** 全局配置：config/terrainradar.json，读取失败时回落到默认清单。 */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("terrainradar.json");

    private static TerrainRadarConfig config = new TerrainRadarConfig();

    private ConfigManager() {
    }

    public static TerrainRadarConfig get() {
        return config;
    }

    /** 载入配置；文件不存在时用默认清单初始化并写盘。 */
    public static void load(Logger logger) {
        if (Files.isRegularFile(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH, StandardCharsets.UTF_8);
                TerrainRadarConfig parsed = GSON.fromJson(json, TerrainRadarConfig.class);
                if (parsed != null) {
                    config = parsed.normalized();
                }
            } catch (JsonParseException | IOException e) {
                logger.warn("读取 {} 失败，使用默认清单", CONFIG_PATH, e);
                config = defaultConfig();
            }
        } else {
            config = defaultConfig();
        }
        save(logger);
    }

    private static TerrainRadarConfig defaultConfig() {
        TerrainRadarConfig fresh = new TerrainRadarConfig();
        fresh.hidden = new LinkedHashSet<>(Presets.resolve("植被"));
        fresh.enabled = true;
        fresh.lightPassthrough = true;
        fresh.preset = "植被";
        return fresh;
    }

    public static void save(Logger logger) {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(config) + "\n", StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("写入 {} 失败", CONFIG_PATH, e);
        }
    }

    public static void setHidden(Set<String> hidden, Logger logger) {
        config.hidden = new LinkedHashSet<>(hidden);
        save(logger);
    }

    public static Path path() {
        return CONFIG_PATH;
    }
}
