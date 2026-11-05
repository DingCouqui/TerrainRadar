package com.dingcouqui.terrainradar;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 客户端入口。MVP 阶段先只做加载日志，具体功能按设计文档分步实现。 */
public final class TerrainRadarClient implements ClientModInitializer {
    public static final String MOD_ID = "terrainradar";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("TerrainRadar client initialized (skeleton)");
    }
}
