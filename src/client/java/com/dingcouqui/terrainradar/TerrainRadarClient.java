package com.dingcouqui.terrainradar;

import com.dingcouqui.terrainradar.command.TerrainRadarCommand;
import com.dingcouqui.terrainradar.config.ConfigManager;
import com.dingcouqui.terrainradar.hidden.HiddenBlocks;
import com.dingcouqui.terrainradar.hidden.LightCache;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
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
        HiddenBlocks.setHideGate(() -> MultiplayerFlag.isEnabledInMultiplayer
                || !isMultiplayerSession(MinecraftClient.getInstance()));
        TerrainRadarCommand.register();
        ToggleKeyBinding.register();
        ClientTickEvents.END_CLIENT_TICK.register(Refresh::tick);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            HiddenBlocks.reload(LOGGER);
            LightCache.invalidate();
        });
        LOGGER.info("TerrainRadar loaded: {} hidden block ids", HiddenBlocks.ids().size());
        logRendererCompat();
    }

    /** 记录实际生效的渲染管线，方便用户反馈兼容性问题。 */
    private static void logRendererCompat() {
        try {
            FabricLoader loader = FabricLoader.getInstance();
            if (loader.isModLoaded("sodium")) {
                String version = loader.getModContainer("sodium")
                        .map(container -> container.getMetadata().getVersion().getFriendlyString())
                        .orElse("unknown");
                LOGGER.info("TerrainRadar: Sodium {} detected, Sodium chunk render hooks enabled", version);
            }
            if (loader.isModLoaded("iris")) {
                String version = loader.getModContainer("iris")
                        .map(container -> container.getMetadata().getVersion().getFriendlyString())
                        .orElse("unknown");
                LOGGER.info("TerrainRadar: Iris {} detected", version);
            }
        } catch (RuntimeException e) {
            LOGGER.debug("读取渲染模组信息失败", e);
        }
    }

    /** 配置或隐藏集合变化后统一调用：保存、重解析、让缓冲区重建。 */
    public static void applyChange() {
        ConfigManager.save(LOGGER);
        HiddenBlocks.reload(LOGGER);
        LightCache.invalidate();
        Refresh.request();
    }

    /** 是否处于多人会话：远程服务器，或已开放到局域网的本地世界。 */
    private static boolean isMultiplayerSession(MinecraftClient client) {
        if (client.getCurrentServerEntry() != null) {
            return true;
        }
        return client.getServer() != null && client.getServer().getServerPort() > 0;
    }
}
