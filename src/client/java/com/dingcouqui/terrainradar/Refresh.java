package com.dingcouqui.terrainradar;

import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 把"需要重建区块缓冲区"的请求合并到下一次客户端 tick。 */
public final class Refresh {
    private static final Logger LOGGER = LoggerFactory.getLogger(TerrainRadarClient.MOD_ID);
    private static boolean pending;

    private Refresh() {
    }

    public static void request() {
        pending = true;
    }

    public static void tick(MinecraftClient client) {
        if (!pending) {
            return;
        }
        pending = false;
        if (client.world == null || client.worldRenderer == null) {
            return;
        }
        try {
            client.worldRenderer.reload();
        } catch (RuntimeException e) {
            LOGGER.warn("重建区块渲染失败", e);
        }
    }
}
