package com.dingcouqui.terrainradar.command;

import com.dingcouqui.terrainradar.Refresh;
import com.dingcouqui.terrainradar.TerrainRadarClient;
import com.dingcouqui.terrainradar.config.ConfigManager;
import com.dingcouqui.terrainradar.config.Presets;
import com.dingcouqui.terrainradar.gui.TerrainRadarScreen;
import com.dingcouqui.terrainradar.hidden.HiddenBlocks;
import com.dingcouqui.terrainradar.hidden.LightCache;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/** /terrainradar 客户端命令。 */
public final class TerrainRadarCommand {
    private TerrainRadarCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommandManager.literal("terrainradar")
                        .executes(ctx -> status(ctx.getSource()))
                        .then(ClientCommandManager.literal("on").executes(ctx -> setEnabled(ctx.getSource(), true)))
                        .then(ClientCommandManager.literal("off").executes(ctx -> setEnabled(ctx.getSource(), false)))
                        .then(ClientCommandManager.literal("toggle").executes(ctx -> setEnabled(
                                ctx.getSource(), !ConfigManager.get().enabled)))
                        .then(ClientCommandManager.literal("light")
                                .then(ClientCommandManager.literal("on").executes(ctx -> setLight(ctx.getSource(), true)))
                                .then(ClientCommandManager.literal("off").executes(ctx -> setLight(ctx.getSource(), false)))
                                .then(ClientCommandManager.literal("toggle").executes(ctx -> setLight(
                                        ctx.getSource(), !ConfigManager.get().lightPassthrough))))
                        .then(ClientCommandManager.literal("add")
                                .then(ClientCommandManager.argument("block", StringArgumentType.string())
                                        .executes(ctx -> add(ctx.getSource(), StringArgumentType.getString(ctx, "block")))))
                        .then(ClientCommandManager.literal("remove")
                                .then(ClientCommandManager.argument("block", StringArgumentType.string())
                                        .executes(ctx -> remove(ctx.getSource(), StringArgumentType.getString(ctx, "block")))))
                        .then(ClientCommandManager.literal("preset")
                                .then(ClientCommandManager.argument("name", StringArgumentType.string())
                                        .suggests((ctx, builder) -> {
                                            for (String name : Presets.NAMES) {
                                                builder.suggest(name);
                                            }
                                            builder.suggest("vegetation");
                                            builder.suggest("surface");
                                            builder.suggest("deep");
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> preset(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                        .then(ClientCommandManager.literal("list").executes(ctx -> list(ctx.getSource())))
                        .then(ClientCommandManager.literal("gui").executes(ctx -> openGui()))
                        .then(ClientCommandManager.literal("reload").executes(ctx -> reload(ctx.getSource())))
        ));
    }

    private static int status(FabricClientCommandSource source) {
        source.sendFeedback(Text.translatable("terrainradar.command.status",
                Text.translatable(ConfigManager.get().enabled ? "terrainradar.state.on" : "terrainradar.state.off"),
                ConfigManager.get().hidden.size(),
                Text.translatable(ConfigManager.get().lightPassthrough
                        ? "terrainradar.state.on" : "terrainradar.state.off"),
                Presets.displayName(ConfigManager.get().preset)));
        if (!HiddenBlocks.missingIds().isEmpty()) {
            source.sendFeedback(Text.translatable("terrainradar.command.missing_ids", HiddenBlocks.missingIds()));
        }
        source.sendFeedback(Text.translatable("terrainradar.command.config_path", ConfigManager.path()));
        return 1;
    }

    private static int setEnabled(FabricClientCommandSource source, boolean enabled) {
        ConfigManager.get().enabled = enabled;
        apply();
        source.sendFeedback(Text.translatable(
                enabled ? "terrainradar.command.enabled" : "terrainradar.command.disabled"));
        return 1;
    }

    private static int setLight(FabricClientCommandSource source, boolean enabled) {
        ConfigManager.get().lightPassthrough = enabled;
        apply();
        source.sendFeedback(Text.translatable(
                enabled ? "terrainradar.command.light_on" : "terrainradar.command.light_off"));
        return 1;
    }

    private static int add(FabricClientCommandSource source, String rawId) {
        Identifier id = Identifier.tryParse(rawId.toLowerCase(Locale.ROOT));
        if (id == null || Registries.BLOCK.get(id) == null) {
            source.sendError(Text.translatable("terrainradar.command.block_not_found", rawId));
            return 0;
        }
        ConfigManager.get().hidden.add(id.toString());
        ConfigManager.get().preset = Presets.CUSTOM;
        apply();
        source.sendFeedback(Text.translatable("terrainradar.command.added", id));
        return 1;
    }

    private static int remove(FabricClientCommandSource source, String rawId) {
        Identifier id = Identifier.tryParse(rawId.toLowerCase(Locale.ROOT));
        if (id == null) {
            source.sendError(Text.translatable("terrainradar.command.invalid_id", rawId));
            return 0;
        }
        boolean removed = ConfigManager.get().hidden.remove(id.toString());
        if (!removed) {
            source.sendError(Text.translatable("terrainradar.command.not_in_list", id));
            return 0;
        }
        ConfigManager.get().preset = Presets.CUSTOM;
        apply();
        source.sendFeedback(Text.translatable("terrainradar.command.removed", id));
        return 1;
    }

    private static int preset(FabricClientCommandSource source, String rawName) {
        String name = rawName.trim();
        Set<String> ids = Presets.resolve(name);
        if (ids == null) {
            source.sendError(Text.translatable("terrainradar.command.unknown_preset", name,
                    Text.translatable("terrainradar.preset.vegetation"),
                    Text.translatable("terrainradar.preset.surface"),
                    Text.translatable("terrainradar.preset.deep")));
            return 0;
        }
        String canonical = Presets.canonicalName(name);
        ConfigManager.get().hidden = new LinkedHashSet<>(ids);
        ConfigManager.get().preset = canonical;
        apply();
        source.sendFeedback(Text.translatable("terrainradar.command.preset_applied",
                Presets.displayName(canonical), ids.size()));
        return 1;
    }

    private static int list(FabricClientCommandSource source) {
        source.sendFeedback(Text.translatable(
                "terrainradar.command.list_header", ConfigManager.get().hidden.size()));
        int index = 0;
        for (String id : ConfigManager.get().hidden) {
            if (index++ >= 20) {
                source.sendFeedback(Text.translatable(
                        "terrainradar.command.list_more", ConfigManager.path()));
                break;
            }
            source.sendFeedback(Text.literal(" - " + id));
        }
        return 1;
    }

    private static int reload(FabricClientCommandSource source) {
        ConfigManager.load(TerrainRadarClient.LOGGER);
        HiddenBlocks.reload(TerrainRadarClient.LOGGER);
        LightCache.invalidate();
        Refresh.request();
        source.sendFeedback(Text.translatable("terrainradar.command.reloaded"));
        return 1;
    }

    private static int openGui() {
        MinecraftClient client = MinecraftClient.getInstance();
        // 命令是玩家在聊天框里输入的：ChatScreen 在 sendMessage 返回后会立刻
        // client.setScreen(null)（见 ChatScreen.keyPressed），如果这里同步打开界面，
        // 会被紧接着的 setScreen(null) 立刻关掉，表现为“命令没反应”。
        // 因此把打开界面延迟到客户端下一 tick 执行。
        client.send(() -> client.setScreen(new TerrainRadarScreen(null)));
        return 1;
    }

    private static void apply() {
        TerrainRadarClient.applyChange();
    }
}
