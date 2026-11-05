package com.dingcouqui.terrainradar;

import com.dingcouqui.terrainradar.command.TerrainRadarCommand;
import com.dingcouqui.terrainradar.config.ConfigManager;
import com.dingcouqui.terrainradar.hidden.HiddenBlocks;
import com.dingcouqui.terrainradar.hidden.LightCache;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 客户端入口。 */
public final class TerrainRadarClient implements ClientModInitializer {
    public static final String MOD_ID = "terrainradar";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        ConfigManager.load(LOGGER);
        HiddenBlocks.reload(LOGGER);
        TerrainRadarCommand.register();
        ToggleKeyBinding.register();
        ClientTickEvents.END_CLIENT_TICK.register(Refresh::tick);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            HiddenBlocks.reload(LOGGER);
            LightCache.invalidate();
        });
        LOGGER.info("TerrainRadar loaded: {} hidden block ids", HiddenBlocks.ids().size());
    }

    /** 配置或隐藏集合变化后统一调用：保存、重解析、让缓冲区重建。 */
    public static void applyChange() {
        ConfigManager.save(LOGGER);
        HiddenBlocks.reload(LOGGER);
        LightCache.invalidate();
        Refresh.request();
    }
}
