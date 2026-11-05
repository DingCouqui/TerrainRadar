package com.dingcouqui.terrainradar.gui;

import com.dingcouqui.terrainradar.TerrainRadarClient;
import com.dingcouqui.terrainradar.config.ConfigManager;
import com.dingcouqui.terrainradar.config.Presets;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
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
    private static final int CHECKBOX_SIZE = 17;
    private static final Identifier CHECKBOX_TEXTURE = Identifier.ofVanilla("widget/checkbox");
    private static final Identifier CHECKBOX_HIGHLIGHTED = Identifier.ofVanilla("widget/checkbox_highlighted");
    private static final Identifier CHECKBOX_SELECTED = Identifier.ofVanilla("widget/checkbox_selected");
    private static final Identifier CHECKBOX_SELECTED_HIGHLIGHTED = Identifier.ofVanilla("widget/checkbox_selected_highlighted");
    private static final List<String> CATEGORIES = List.of(
            "全部", "建筑方块", "染色方块", "自然方块", "功能方块", "红石方块", "其它");
    private static final List<RegistryKey<ItemGroup>> CREATIVE_GROUPS = List.of(
            ItemGroups.BUILDING_BLOCKS, ItemGroups.COLORED_BLOCKS, ItemGroups.NATURAL,
            ItemGroups.FUNCTIONAL, ItemGroups.REDSTONE);
    private static final Set<String> RESOURCE_BLOCKS = Set.of(
            "iron_block", "gold_block", "copper_block", "diamond_block", "emerald_block",
            "lapis_block", "redstone_block", "coal_block", "netherite_block", "quartz_block",
            "amethyst_block", "raw_iron_block", "raw_gold_block", "raw_copper_block",
            "ancient_debris", "resin_block");
    private static final Set<String> DYE_COLORS = Set.of(
            "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black");
    private static final String[] COLORED_SUFFIXES = {
            "_wool", "_carpet", "_concrete", "_concrete_powder", "_terracotta", "_glazed_terracotta",
            "_stained_glass", "_stained_glass_pane", "_candle", "_bed", "_banner", "_shulker_box"};
    private static final String[] REDSTONE_KEYWORDS = {
            "redstone", "piston", "observer", "repeater", "comparator", "dispenser", "dropper",
            "hopper", "lever", "button", "pressure_plate", "tripwire", "target", "daylight_detector",
            "note_block", "redstone_lamp", "rail", "sculk_sensor", "copper_bulb", "lightning_rod", "tnt"};
    private static final String[] FUNCTIONAL_KEYWORDS = {
            "crafting_table", "enchanting_table", "brewing_stand", "cauldron", "barrel", "beehive",
            "bee_nest", "respawn_anchor", "lodestone", "bell", "grindstone", "smithing_table",
            "stonecutter", "loom", "cartography_table", "fletching_table", "composter", "command_block",
            "structure_block", "jigsaw", "barrier", "light", "spawner", "portal", "chest", "furnace",
            "smoker", "lectern", "campfire", "beacon", "conduit", "sign", "bed", "banner",
            "shulker_box", "anvil", "bookshelf", "decorated_pot", "flower_pot", "jukebox"};
    private static boolean creativeTabsReady;

    private static boolean ensureCreativeTabs() {
        if (creativeTabsReady) {
            return true;
        }
        try {
            ItemGroups.updateDisplayContext(FeatureFlags.DEFAULT_ENABLED_FEATURES, false,
                    DynamicRegistryManager.of(Registries.REGISTRIES));
            ItemGroup natural = Registries.ITEM_GROUP.get(ItemGroups.NATURAL);
            creativeTabsReady = natural != null && !natural.getDisplayStacks().isEmpty();
            return creativeTabsReady;
        } catch (Throwable ignored) {
            // 资源/世界未就绪时无法填充创造模式标签页，退回按 tag + ID 分类
            return false;
        }
    }

    /**
     * 优先按原版创造模式标签页分类（建筑/染色/自然/功能/红石），
     * 未出现在原版标签页里的方块（如模组方块）再用 tag + ID 兜底。
     * 1 = 建筑方块，2 = 染色方块，3 = 自然方块，4 = 功能方块，5 = 红石方块，0 = 其它。
     */
    private static int computeCategory(Block block) {
        Item item = block.asItem();
        if (creativeTabsReady && item != Items.AIR) {
            ItemStack stack = new ItemStack(item);
            for (int i = 0; i < CREATIVE_GROUPS.size(); i++) {
                ItemGroup group = Registries.ITEM_GROUP.get(CREATIVE_GROUPS.get(i));
                if (group != null && group.contains(stack)) {
                    return i + 1;
                }
            }
        }
        return fallbackClassify(block);
    }

    private int categoryOf(Block block) {
        return categoryCache.computeIfAbsent(block, TerrainRadarScreen::computeCategory);
    }

    private static int fallbackClassify(Block block) {
        BlockState state = block.getDefaultState();
        String path = Registries.BLOCK.getId(block).getPath();

        if (isColored(path)) {
            return 2;
        }
        for (String keyword : REDSTONE_KEYWORDS) {
            if (path.contains(keyword)) {
                return 5;
            }
        }
        if (state.isIn(BlockTags.SIGNS)) {
            return 4;
        }
        for (String keyword : FUNCTIONAL_KEYWORDS) {
            if (path.contains(keyword)) {
                return 4;
            }
        }
        if (state.isIn(BlockTags.LEAVES) || state.isIn(BlockTags.LOGS) || state.isIn(BlockTags.FLOWERS)
                || state.isIn(BlockTags.SAPLINGS) || state.isIn(BlockTags.CROPS)
                || path.contains("leaves") || path.endsWith("_log") || path.endsWith("_wood")
                || path.endsWith("_hyphae") || path.startsWith("stripped_")
                || path.contains("_ore") || RESOURCE_BLOCKS.contains(path)
                || path.matches(".*(grass|fern|flower|sapling|mushroom|fungus|roots|sprout|vine|kelp|"
                + "seagrass|dripleaf|blossom|lichen|bush|cactus|chorus|lily_pad|petal|litter|moss|"
                + "bamboo|sugar_cane|nether_wart|wheat|carrot|potato|beetroot|cocoa|azalea|berry|"
                + "dirt|sand|gravel|stone|snow|ice|clay|mud).*")) {
            return 3;
        }
        if (state.isIn(BlockTags.STAIRS) || state.isIn(BlockTags.SLABS) || state.isIn(BlockTags.WALLS)
                || state.isIn(BlockTags.FENCES) || state.isIn(BlockTags.FENCE_GATES)
                || state.isIn(BlockTags.DOORS) || state.isIn(BlockTags.TRAPDOORS)
                || state.isIn(BlockTags.PLANKS) || state.isIn(BlockTags.WOOL)
                || state.isIn(BlockTags.WOOL_CARPETS) || state.isIn(BlockTags.TERRACOTTA)
                || state.isIn(BlockTags.CONCRETE_POWDER)
                || path.endsWith("_concrete") || path.contains("glass")
                || path.endsWith("_bricks") || path.endsWith("_brick") || path.endsWith("_tiles")
                || state.isOpaqueFullCube()) {
            return 1;
        }
        return 0;
    }

    private static boolean isColored(String path) {
        for (String color : DYE_COLORS) {
            for (String suffix : COLORED_SUFFIXES) {
                if (path.equals(color + suffix)) {
                    return true;
                }
            }
        }
        return false;
    }

    private final Screen parent;
    private final Set<String> working = new LinkedHashSet<>(ConfigManager.get().hidden);
    private final List<Block> allBlocks = new ArrayList<>();
    private final Map<Block, Integer> categoryCache = new HashMap<>();

    private boolean workingEnabled = ConfigManager.get().enabled;
    private boolean workingLight = ConfigManager.get().lightPassthrough;
    private int tab = TAB_SETTINGS;

    private String search = "";
    private int categoryIndex;
    private int scroll;
    private TextFieldWidget searchField;
    private boolean appliedFlash;
    private boolean draggingScrollbar;
    private ButtonWidget enabledButton;
    private ButtonWidget lightButton;

    public TerrainRadarScreen(Screen parent) {
        super(Text.literal("TerrainRadar 设置"));
        this.parent = parent;
        for (Block block : Registries.BLOCK) {
            if (block.asItem() != Items.AIR) {
                allBlocks.add(block);
            }
        }
        allBlocks.sort(Comparator.comparing(block -> Registries.BLOCK.getId(block).toString()));
        ensureCreativeTabs();
    }

    // ---- 布局 ----

    private int panelWidth() {
        return Math.min(460, width - 40);
    }

    private int panelLeft() {
        return width / 2 - panelWidth() / 2;
    }

    private int contentTop() {
        return 44;
    }

    private int listTop() {
        return contentTop() + 52;
    }

    private int listBottom() {
        return height - 52;
    }

    private int listHeight() {
        return Math.max(ROW_HEIGHT, listBottom() - listTop());
    }

    private int settingsRowY() {
        return height / 2 - 44;
    }

    // ---- 数据 ----

    private List<Block> filtered() {
        String query = search.trim().toLowerCase(Locale.ROOT);
        List<Block> result = new ArrayList<>();
        for (Block block : allBlocks) {
            String id = Registries.BLOCK.getId(block).toString();
            int wanted = categoryIndex == 6 ? 0 : categoryIndex;
            if (categoryIndex != 0 && categoryOf(block) != wanted) {
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
        return Math.max(24, listHeight() * visible / total);
    }

    private int scrollbarThumbY() {
        int max = maxScroll();
        if (max <= 0) {
            return listTop();
        }
        int travel = listHeight() - scrollbarThumbHeight();
        return listTop() + (int) Math.round((double) scroll / max * travel);
    }

    // ---- 初始化 ----

    @Override
    protected void init() {
        int left = panelLeft();
        int w = panelWidth();
        scroll = Math.min(scroll, maxScroll());

        // 标签按钮：沿用上一版的居中大按钮样式
        int tabW1 = 240;
        int tabW2 = 200;
        int gap = 8;
        int tabX = width / 2 - (tabW1 + gap + tabW2) / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal(tab == TAB_SETTINGS ? "» TerrainRadar 设置" : "TerrainRadar 设置"),
                        button -> switchTab(TAB_SETTINGS))
                .dimensions(tabX, 10, tabW1, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(tab == TAB_BLOCKS ? "» 方块清单" : "方块清单"),
                        button -> switchTab(TAB_BLOCKS))
                .dimensions(tabX + tabW1 + gap, 10, tabW2, 20).build());

        // 底部按钮
        int bottom = height - 30;
        addDrawableChild(ButtonWidget.builder(Text.literal("应用"), button -> applyAndKeepOpen())
                .dimensions(width / 2 - 105, bottom, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("返回"), button -> close())
                .dimensions(width / 2 + 5, bottom, 100, 20).build());

        if (tab == TAB_SETTINGS) {
            initSettings(left, w);
        } else {
            initBlocks(left, w);
        }
    }

    private void initSettings(int left, int w) {
        int rowY = settingsRowY();
        enabledButton = ButtonWidget.builder(toggleText(workingEnabled), button -> {
                    workingEnabled = !workingEnabled;
                    button.setMessage(toggleText(workingEnabled));
                })
                .dimensions(left + w - 80, rowY, 70, 20).build();
        lightButton = ButtonWidget.builder(toggleText(workingLight), button -> {
                    workingLight = !workingLight;
                    button.setMessage(toggleText(workingLight));
                })
                .dimensions(left + w - 80, rowY + 34, 70, 20).build();
        addDrawableChild(enabledButton);
        addDrawableChild(lightButton);
    }

    private Text toggleText(boolean on) {
        return Text.literal(on ? "开" : "关");
    }

    private void initBlocks(int left, int w) {
        int top = contentTop();
        searchField = new TextFieldWidget(textRenderer, left, top, Math.max(80, w - 170), 20,
                Text.literal("搜索方块 ID / 名称"));
        searchField.setText(search);
        searchField.setChangedListener(value -> {
            search = value == null ? "" : value;
            scroll = 0;
        });
        addDrawableChild(searchField);

        addDrawableChild(ButtonWidget.builder(Text.literal("分类：" + CATEGORIES.get(categoryIndex)),
                        button -> {
                            categoryIndex = (categoryIndex + 1) % CATEGORIES.size();
                            scroll = 0;
                            rebuild();
                        })
                .dimensions(left + w - 160, top, 160, 20).build());

        int rowY = top + 26;
        for (int i = 0; i < Presets.NAMES.size(); i++) {
            String name = Presets.NAMES.get(i);
            addDrawableChild(ButtonWidget.builder(Text.literal("预设：" + name), button -> {
                        Set<String> preset = Presets.resolve(name);
                        if (preset != null) {
                            working.clear();
                            working.addAll(preset);
                        }
                    })
                    .dimensions(left + i * 104, rowY, 100, 18).build());
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("全选"), button -> {
                    for (Block block : filtered()) {
                        working.add(Registries.BLOCK.getId(block).toString());
                    }
                })
                .dimensions(left + w - 128, rowY, 60, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("反选"), button -> {
                    for (Block block : filtered()) {
                        String id = Registries.BLOCK.getId(block).toString();
                        if (!working.remove(id)) {
                            working.add(id);
                        }
                    }
                })
                .dimensions(left + w - 64, rowY, 60, 18).build());
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

    // ---- 渲染 ----

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 1.21.11 的 renderWithTooltip 已经调用了 renderBackground（含 blur），这里不要再调。
        super.render(context, mouseX, mouseY, delta);
        int left = panelLeft();
        int w = panelWidth();

        if (tab == TAB_SETTINGS) {
            int rowY = settingsRowY();
            context.drawTextWithShadow(textRenderer, Text.literal("隐藏方块（总开关）"), left + 10, rowY + 6, 0xFFFFFFFF);
            context.drawTextWithShadow(textRenderer, Text.literal("光照穿透（去除天光遮挡与 AO 阴影）"),
                    left + 10, rowY + 40, 0xFFFFFFFF);
            context.drawTextWithShadow(textRenderer,
                    Text.literal("修改完点「应用」生效，点「返回」放弃修改。当前清单：" + working.size() + " 个方块"),
                    left + 10, rowY + 86, 0xFFA0A0A0);
        } else {
            renderBlockList(context, left, w, mouseX, mouseY);
        }

        if (appliedFlash) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("已应用"), width / 2, height - 50, 0xFF55FF55);
        }
    }

    private void renderBlockList(DrawContext context, int left, int w, int mouseX, int mouseY) {
        clampScroll();
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
                        + " ｜ 滚轮、PageUp/Down、拖动滚动条"),
                left + 4, bottom + 4, 0xFFA0A0A0);

        // 滚动条
        int max = maxScroll();
        if (max > 0) {
            int sx = scrollbarX();
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
        Identifier texture;
        if (checked) {
            texture = hover ? CHECKBOX_SELECTED_HIGHLIGHTED : CHECKBOX_SELECTED;
        } else {
            texture = hover ? CHECKBOX_HIGHLIGHTED : CHECKBOX_TEXTURE;
        }
        int boxY = y + (ROW_HEIGHT - CHECKBOX_SIZE) / 2;
        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, texture, x, boxY, CHECKBOX_SIZE, CHECKBOX_SIZE);
        context.drawTextWithShadow(textRenderer, Text.literal(label), x + CHECKBOX_SIZE + 4, y + 6, 0xFFFFFFFF);
    }

    // ---- 输入 ----

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
