package com.dingcouqui.terrainradar.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** 从 ModMenu 或 /terrainradar gui 打开的中转页。 */
public class TerrainRadarHomeScreen extends Screen {
    private final Screen parent;

    public TerrainRadarHomeScreen(Screen parent) {
        super(Text.literal("TerrainRadar"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int top = this.height / 2 - 48;
        addDrawableChild(ButtonWidget.builder(Text.literal("设置（Cloth Config）"),
                        button -> client.setScreen(TerrainRadarConfigScreen.create(this)))
                .dimensions(centerX - 100, top, 200, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("方块清单（搜索 / 分类 / 勾选）"),
                        button -> client.setScreen(new BlockListScreen(this)))
                .dimensions(centerX - 120, top + 24, 240, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("完成"), button -> close())
                .dimensions(centerX - 100, top + 48, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 84, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("隐藏方块：TerrainRadar | 当前清单见 config/terrainradar.json"),
                width / 2, height / 2 + 84, 0xA0A0A0);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
