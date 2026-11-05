package com.dingcouqui.terrainradar.command;

import com.dingcouqui.terrainradar.Refresh;
import com.dingcouqui.terrainradar.TerrainRadarClient;
import com.dingcouqui.terrainradar.config.ConfigManager;
import com.dingcouqui.terrainradar.config.DefaultBlocks;
import com.dingcouqui.terrainradar.hidden.HiddenBlocks;
import com.dingcouqui.terrainradar.hidden.LightCache;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** /terrainradar 客户端命令（MVP 阶段替代 GUI）。 */
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
                                        .executes(ctx -> preset(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                        .then(ClientCommandManager.literal("list").executes(ctx -> list(ctx.getSource())))
                        .then(ClientCommandManager.literal("reload").executes(ctx -> reload(ctx.getSource())))
        ));
    }

    private static int status(FabricClientCommandSource source) {
        int count = ConfigManager.get().hidden.size();
        source.sendFeedback(Text.literal(String.format(Locale.ROOT,
                "TerrainRadar: %s | 隐藏 %d 个方块 | 光照穿透 %s | 预设 %s",
                ConfigManager.get().enabled ? "开启" : "关闭",
                count,
                ConfigManager.get().lightPassthrough ? "开" : "关",
                ConfigManager.get().preset)));
        if (!HiddenBlocks.missingIds().isEmpty()) {
            source.sendFeedback(Text.literal("无法解析的 ID：" + HiddenBlocks.missingIds()));
        }
        source.sendFeedback(Text.literal("配置：" + ConfigManager.path()));
        return 1;
    }

    private static int setEnabled(FabricClientCommandSource source, boolean enabled) {
        ConfigManager.get().enabled = enabled;
        apply();
        source.sendFeedback(Text.literal("TerrainRadar 已" + (enabled ? "开启" : "关闭")));
        return 1;
    }

    private static int setLight(FabricClientCommandSource source, boolean enabled) {
        ConfigManager.get().lightPassthrough = enabled;
        apply();
        source.sendFeedback(Text.literal("光照穿透已" + (enabled ? "开启" : "关闭")));
        return 1;
    }

    private static int add(FabricClientCommandSource source, String rawId) {
        Identifier id = Identifier.tryParse(rawId.toLowerCase(Locale.ROOT));
        if (id == null || Registries.BLOCK.get(id) == null) {
            source.sendError(Text.literal("找不到方块：" + rawId));
            return 0;
        }
        ConfigManager.get().hidden.add(id.toString());
        ConfigManager.get().preset = "自定义";
        apply();
        source.sendFeedback(Text.literal("已加入隐藏清单：" + id));
        return 1;
    }

    private static int remove(FabricClientCommandSource source, String rawId) {
        Identifier id = Identifier.tryParse(rawId.toLowerCase(Locale.ROOT));
        if (id == null) {
            source.sendError(Text.literal("无效的方块 ID：" + rawId));
            return 0;
        }
        boolean removed = ConfigManager.get().hidden.remove(id.toString());
        if (!removed) {
            source.sendError(Text.literal("清单里没有：" + id));
            return 0;
        }
        ConfigManager.get().preset = "自定义";
        apply();
        source.sendFeedback(Text.literal("已移出隐藏清单：" + id));
        return 1;
    }

    private static int preset(FabricClientCommandSource source, String rawName) {
        String name = rawName.trim();
        Set<String> ids = resolvePreset(name);
        if (ids == null) {
            source.sendError(Text.literal("未知预设：" + name + "（可用：默认 / 仅树叶 / 仅植物）"));
            return 0;
        }
        ConfigManager.get().hidden = new LinkedHashSet<>(ids);
        ConfigManager.get().preset = name;
        apply();
        source.sendFeedback(Text.literal("已应用预设「" + name + "」，共 " + ids.size() + " 个方块"));
        return 1;
    }

    private static int list(FabricClientCommandSource source) {
        source.sendFeedback(Text.literal("共 " + ConfigManager.get().hidden.size() + " 个隐藏方块："));
        int index = 0;
        for (String id : ConfigManager.get().hidden) {
            if (index++ >= 20) {
                source.sendFeedback(Text.literal("……（完整清单见 " + ConfigManager.path() + "）"));
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
        source.sendFeedback(Text.literal("已重新读取配置"));
        return 1;
    }

    private static void apply() {
        TerrainRadarClient.applyChange();
    }

    private static Set<String> resolvePreset(String name) {
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
