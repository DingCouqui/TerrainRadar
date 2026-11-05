package com.dingcouqui.terrainradar.gui;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import com.dingcouqui.terrainradar.TerrainRadarClient;
import com.dingcouqui.terrainradar.config.ConfigManager;
import com.dingcouqui.terrainradar.config.TerrainRadarConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Cloth Config 设置页：总开关、光照穿透。 */
public final class TerrainRadarConfigScreen {
    private TerrainRadarConfigScreen() {
    }

    public static Screen create(Screen parent) {
        TerrainRadarConfig config = ConfigManager.get();
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("TerrainRadar 设置"))
                .setSavingRunnable(TerrainRadarClient::applyChange);
        ConfigEntryBuilder entries = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(Text.literal("常规"));
        general.addEntry(entries.startBooleanToggle(Text.literal("隐藏方块"), config.enabled)
                .setDefaultValue(true)
                .setSaveConsumer(value -> config.enabled = value)
                .setTooltip(Text.literal("关闭后所有方块恢复可见，清单保持不变"))
                .build());
        general.addEntry(entries.startBooleanToggle(Text.literal("光照穿透"), config.lightPassthrough)
                .setDefaultValue(true)
                .setSaveConsumer(value -> config.lightPassthrough = value)
                .setTooltip(Text.literal("隐藏方块不再遮挡天光、也不再产生 AO 阴影"))
                .build());

        ConfigCategory list = builder.getOrCreateCategory(Text.literal("方块清单"));
        list.addEntry(entries.startTextDescription(Text.literal(
                "方块清单在上一页的「方块清单」里编辑：支持搜索、分类、勾选、全选/反选和预设。")).build());

        return builder.build();
    }
}
