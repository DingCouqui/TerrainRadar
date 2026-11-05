package com.dingcouqui.terrainradar.gui;

import com.dingcouqui.terrainradar.TerrainRadarClient;
import com.dingcouqui.terrainradar.config.ConfigManager;
import com.dingcouqui.terrainradar.config.DefaultBlocks;
import com.dingcouqui.terrainradar.config.Presets;
import net.minecraft.block.Block;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** TerrainRadar 设置界面：两个标签页（设置 / 方块清单），底部「应用 / 返回」。 */
public class TerrainRadarScreen extends Screen {
    private static final int TAB_SETTINGS = 0;
    private static final int TAB_BLOCKS = 1;
    private static final int ROW_HEIGHT = 20;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final List<String> CATEGORIES = new ArrayList<>();
    private static final Map<String, String> GROUP_OF = new HashMap<>();

    static {
        CATEGORIES.add("全部");
        CATEGORIES.addAll(DefaultBlocks.groups().keySet());
        CATEGORIES.add("其它");
        DefaultBlocks.groups().forEach((group, ids) -> ids.forEach(id -> GROUP_OF.put(id, group)));
    }

    private final Screen parent;
    private final Set<String> working = new LinkedHashSet<>(ConfigManager.get().hidden);
    private final List<Block> allBlocks = new ArrayList<>();

    private boolean workingEnabled = ConfigManager.get().enabled;
    private boolean workingLight = ConfigManager.get().lightPassthrough;
    private int tab = TAB_SETTINGS;

    private String search = "";
    private int categoryIndex;
    private int scroll;
    private TextFieldWidget searchField;
    private boolean appliedFlash;
    private boolean draggingScrollbar;

    public TerrainRadarScreen(Screen parent) {
        super(Text.literal("TerrainRadar 设置"));
        this.parent = parent;
        for (Block block : Registries.BLOCK) {
            if (block.asItem() != Items.AIR) {
                allBlocks.add(block);
            }
        }
        allBlocks.sort(Comparator.comparing(block -> Registries.BLOCK.getId(block).toString()));
    }

    private int panelLeft() {
        return width / 2 - 240;
    }

    private int panelWidth() {
        return Math.min(480, width - 40);
    }

    private int contentTop() {
        return 64;
    }

    private int listTop() {
        return contentTop() + 76;
    }

    private int listBottom() {
        return height - 66;
    }

    private int listHeight() {
        return Math.max(ROW_HEIGHT, listBottom() - listTop());
    }

    private List<Block> filtered() {
        String query = search.trim().toLowerCase(Locale.ROOT);
        String category = CATEGORIES.get(categoryIndex);
        List<Block> result = new ArrayList<>();
        for (Block block : allBlocks) {
            String id = Registries.BLOCK.getId(block).toString();
            String group = GROUP_OF.getOrDefault(id, "其它");
            if (!category.equals("全部") && !category.equals(group)) {
                continue;
            }
            if (!query.isEmpty()) {
                String name = block.getName().getString().toLowerCase(Locale.ROOT);
                if (!id.toLowerCase(Locale.ROOT).contains(query) && !name.contains(query)) {
                    continue;
                }
            }
            result.add(block);
        }
        return result;
    }

    private int maxScroll() {
        int rows = filtered().size();
        int visible = listHeight() / ROW_HEIGHT;
        return Math.max(0, rows - visible);
    }

    private void clampScroll() {
        scroll = Math.max(0, Math.min(maxScroll(), scroll));
    }

    private int scrollbarX() {
        return panelLeft() + panelWidth() - SCROLLBAR_WIDTH;
    }

    private int scrollbarThumbHeight() {
        int visible = listHeight() / ROW_HEIGHT;
        int total = filtered().size();
        if (total <= visible) {
            return listHeight();
        }
        return Math.max(20, listHeight() * visible / total);
    }

    private int scrollbarThumbY() {
        int max = maxScroll();
        if (max <= 0) {
            return listTop();
        }
        int travel = listHeight() - scrollbarThumbHeight();
        return listTop() + (int) Math.round((double) scroll / max * travel);
    }

    @Override
    protected void init() {
        int left = panelLeft();
        int w = panelWidth();
        scroll = Math.min(scroll, maxScroll());

        // 标签页
        addDrawableChild(ButtonWidget.builder(Text.literal(tab == TAB_SETTINGS ? "» 设置" : "设置"),
                        button -> switchTab(TAB_SETTINGS))
                .dimensions(left, 30, 110, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(tab == TAB_BLOCKS ? "» 方块清单" : "方块清单"),
                        button -> switchTab(TAB_BLOCKS))
                .dimensions(left + 114, 30, 130, 20).build());

        // 底部按钮
        int bottom = height - 34;
        addDrawableChild(ButtonWidget.builder(Text.literal("应用"), button -> applyAndKeepOpen())
                .dimensions(left + w / 2 - 105, bottom, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("返回"), button -> close())
                .dimensions(left + w / 2 + 5, bottom, 100, 20).build());

        if (tab == TAB_SETTINGS) {
            initSettings(left, w);
        } else {
            initBlocks(left, w);
        }
    }

    private void initSettings(int left, int w) {
        addDrawableChild(CheckboxWidget.builder(Text.literal("隐藏方块（总开关）"), textRenderer)
                .pos(left, contentTop() + 10)
                .checked(workingEnabled)
                .callback((widget, checked) -> workingEnabled = checked)
                .build());
        addDrawableChild(CheckboxWidget.builder(Text.literal("光照穿透（去除天光遮挡与 AO 阴影）"), textRenderer)
                .pos(left, contentTop() + 40)
                .checked(workingLight)
                .callback((widget, checked) -> workingLight = checked)
                .build());
    }

    private void initBlocks(int left, int w) {
        int top = contentTop();
        searchField = new TextFieldWidget(textRenderer, left, top + 10, w - 170, 20, Text.literal("搜索方块 ID / 名称"));
        searchField.setText(search);
        searchField.setChangedListener(value -> {
            search = value == null ? "" : value;
            scroll = 0;
            rebuild();
        });
        addDrawableChild(searchField);

        addDrawableChild(ButtonWidget.builder(Text.literal("分类：" + CATEGORIES.get(categoryIndex)),
                        button -> {
                            categoryIndex = (categoryIndex + 1) % CATEGORIES.size();
                            scroll = 0;
                            rebuild();
                        })
                .dimensions(left + w - 160, top + 10, 160, 20).build());

        int rowY = top + 38;
        for (int i = 0; i < Presets.NAMES.size(); i++) {
            String name = Presets.NAMES.get(i);
            addDrawableChild(ButtonWidget.builder(Text.literal("预设：" + name), button -> {
                        Set<String> preset = Presets.resolve(name);
                        if (preset != null) {
                            working.clear();
                            working.addAll(preset);
                        }
                    })
                    .dimensions(left + i * 122, rowY, 116, 18).build());
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("全选"), button -> {
                    for (Block block : filtered()) {
                        working.add(Registries.BLOCK.getId(block).toString());
                    }
                })
                .dimensions(left + w - 170, rowY, 80, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("反选"), button -> {
                    for (Block block : filtered()) {
                        String id = Registries.BLOCK.getId(block).toString();
                        if (!working.remove(id)) {
                            working.add(id);
                        }
                    }
                })
                .dimensions(left + w - 86, rowY, 80, 18).build());
    }

    private void switchTab(int newTab) {
        if (tab == newTab) {
            return;
        }
        tab = newTab;
        rebuild();
    }

    private void rebuild() {
        this.client.execute(this::clearAndInit);
    }

    private void applyAndKeepOpen() {
        ConfigManager.get().enabled = workingEnabled;
        ConfigManager.get().lightPassthrough = workingLight;
        ConfigManager.get().hidden = new LinkedHashSet<>(working);
        ConfigManager.get().preset = "自定义";
        TerrainRadarClient.applyChange();
        appliedFlash = true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 1.21.11 的 renderWithTooltip 已经调用了 renderBackground（含 blur），这里不要再调。
        super.render(context, mouseX, mouseY, delta);
        int left = panelLeft();
        int w = panelWidth();
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFF);

        if (tab == TAB_SETTINGS) {
            context.drawTextWithShadow(textRenderer,
                    Text.literal("勾选后会立即影响渲染；修改完点「应用」，点「返回」放弃修改。"),
                    left, contentTop() + 90, 0xA0A0A0);
            context.drawTextWithShadow(textRenderer,
                    Text.literal("方块清单：" + working.size() + " 个方块（切换上方「方块清单」标签页编辑）"),
                    left, contentTop() + 106, 0xA0A0A0);
        } else {
            renderBlockList(context, left, w, mouseX, mouseY);
        }

        if (appliedFlash) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("已应用"), width / 2, height - 52, 0x55FF55);
        }
    }

    private void renderBlockList(DrawContext context, int left, int w, int mouseX, int mouseY) {
        List<Block> blocks = filtered();
        int top = listTop();
        int bottom = listBottom();
        context.fill(left, top, left + w, bottom, 0x66000000);
        context.drawHorizontalLine(left, left + w, top, 0xFF808080);
        context.drawHorizontalLine(left, left + w, bottom, 0xFF808080);

        int visible = listHeight() / ROW_HEIGHT;
        int end = Math.min(blocks.size(), scroll + visible);
        for (int i = scroll; i < end; i++) {
            int rowY = top + (i - scroll) * ROW_HEIGHT;
            Block block = blocks.get(i);
            String id = Registries.BLOCK.getId(block).toString();
            boolean selected = working.contains(id);
            drawRow(context, left + 6, rowY, selected, id + " · " + block.getName().getString(), mouseX, mouseY);
        }
        context.drawTextWithShadow(textRenderer,
                Text.literal("已选 " + working.size() + " / 匹配 " + blocks.size()
                        + " ｜ 滚轮/PageUp/Down 滚动 ｜ 点击行切换"),
                left + 4, bottom + 6, 0xA0A0A0);

        // 滚动条
        int max = maxScroll();
        if (max > 0) {
            int sx = scrollbarX();
            int track = listHeight();
            context.fill(sx, top, sx + SCROLLBAR_WIDTH, bottom, 0x55000000);
            int thumbH = scrollbarThumbHeight();
            int thumbY = scrollbarThumbY();
            context.fill(sx, thumbY, sx + SCROLLBAR_WIDTH, thumbY + thumbH, 0xFFAAAAAA);
        }
    }

    private void drawRow(DrawContext context, int x, int y, boolean checked, String label, int mouseX, int mouseY) {
        boolean hover = mouseX >= x && mouseX <= x + panelWidth() && mouseY >= y && mouseY < y + ROW_HEIGHT;
        if (hover) {
            context.fill(x - 2, y, x + panelWidth() - 10, y + ROW_HEIGHT - 2, 0x33FFFFFF);
        }
        context.fill(x, y + 3, x + 12, y + 15, 0xFF000000);
        context.fill(x + 1, y + 4, x + 11, y + 14, checked ? 0xFF55FF55 : 0xFF303030);
        if (checked) {
            context.fill(x + 4, y + 7, x + 9, y + 12, 0xFF103010);
        }
        context.drawTextWithShadow(textRenderer, Text.literal(label), x + 18, y + 6, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        if (tab != TAB_BLOCKS || click.button() != 0) {
            return false;
        }
        int top = listTop();
        int bottom = listBottom();
        int sx = scrollbarX();

        // 滚动条：拖拽或点击跳转
        if (click.x() >= sx && click.x() <= sx + SCROLLBAR_WIDTH && click.y() >= top && click.y() <= bottom) {
            int max = maxScroll();
            if (max > 0) {
                int thumbY = scrollbarThumbY();
                int thumbH = scrollbarThumbHeight();
                if (click.y() >= thumbY && click.y() <= thumbY + thumbH) {
                    draggingScrollbar = true;
                } else {
                    int travel = listHeight() - thumbH;
                    if (travel > 0) {
                        scroll = (int) Math.round((click.y() - top - thumbH / 2.0) / travel * max);
                        clampScroll();
                    }
                }
            }
            return true;
        }

        // 行点击：切换勾选
        int left = panelLeft() + 6;
        if (click.x() >= left && click.x() <= left + panelWidth()
                && click.y() >= top && click.y() < bottom) {
            int row = scroll + (int) ((click.y() - top) / ROW_HEIGHT);
            List<Block> blocks = filtered();
            if (row >= 0 && row < blocks.size()) {
                String id = Registries.BLOCK.getId(blocks.get(row)).toString();
                if (!working.remove(id)) {
                    working.add(id);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (draggingScrollbar && click.button() == 0) {
            int max = maxScroll();
            int thumbH = scrollbarThumbHeight();
            int travel = listHeight() - thumbH;
            if (max > 0 && travel > 0) {
                scroll = (int) Math.round((click.y() - listTop() - thumbH / 2.0) / travel * max);
                clampScroll();
            }
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        draggingScrollbar = false;
        return super.mouseReleased(click);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (tab == TAB_BLOCKS && (input.key() == GLFW.GLFW_KEY_PAGE_UP || input.key() == GLFW.GLFW_KEY_PAGE_DOWN)) {
            int page = Math.max(1, listHeight() / ROW_HEIGHT - 1);
            scroll += input.key() == GLFW.GLFW_KEY_PAGE_UP ? -page : page;
            clampScroll();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            return true;
        }
        if (tab == TAB_BLOCKS && mouseX >= panelLeft() && mouseX <= panelLeft() + panelWidth()
                && mouseY >= listTop() && mouseY <= listBottom()) {
            scroll -= (int) Math.signum(verticalAmount) * 3;
            clampScroll();
            return true;
        }
        return false;
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
