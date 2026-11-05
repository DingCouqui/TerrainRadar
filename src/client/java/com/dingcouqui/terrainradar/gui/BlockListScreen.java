package com.dingcouqui.terrainradar.gui;

import com.dingcouqui.terrainradar.TerrainRadarClient;
import com.dingcouqui.terrainradar.config.ConfigManager;
import com.dingcouqui.terrainradar.config.DefaultBlocks;
import com.dingcouqui.terrainradar.config.Presets;
import net.minecraft.block.Block;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** 方块清单页：搜索 + 分类 + 勾选 + 全选/反选 + 预设。分页显示，避免一次创建过多控件。 */
public class BlockListScreen extends Screen {
    private static final int PAGE_SIZE = 9;
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

    private String search = "";
    private int categoryIndex;
    private int page;
    private TextFieldWidget searchField;

    public BlockListScreen(Screen parent) {
        super(Text.literal("TerrainRadar · 方块清单"));
        this.parent = parent;
        for (Block block : Registries.BLOCK) {
            if (block.asItem() == Items.AIR) {
                continue;
            }
            allBlocks.add(block);
        }
        allBlocks.sort(Comparator.comparing(block -> Registries.BLOCK.getId(block).toString()));
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

    @Override
    protected void init() {
        List<Block> matches = filtered();
        int total = matches.size();
        int maxPage = Math.max(0, (total - 1) / PAGE_SIZE);
        page = Math.min(Math.max(0, page), maxPage);

        int centerX = width / 2;
        int left = centerX - 210;
        int top = 36;

        searchField = new TextFieldWidget(textRenderer, left, top, 300, 20, Text.literal("搜索方块 ID / 名称"));
        searchField.setText(search);
        searchField.setChangedListener(value -> {
            search = value == null ? "" : value;
            page = 0;
            rebuild();
        });
        addDrawableChild(searchField);

        addDrawableChild(ButtonWidget.builder(Text.literal("分类：" + CATEGORIES.get(categoryIndex)),
                        button -> {
                            categoryIndex = (categoryIndex + 1) % CATEGORIES.size();
                            page = 0;
                            rebuild();
                        })
                .dimensions(left + 308, top, 112, 20).build());

        // 分页
        addDrawableChild(ButtonWidget.builder(Text.literal("< 上一页"), button -> {
                    if (page > 0) {
                        page--;
                        rebuild();
                    }
                })
                .dimensions(left, top + 26, 70, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("下一页 >"), button -> {
                    if (page < maxPage) {
                        page++;
                        rebuild();
                    }
                })
                .dimensions(left + 338, top + 26, 82, 18).build());

        // 预设
        int presetY = top + 26;
        for (int i = 0; i < Presets.NAMES.size(); i++) {
            String name = Presets.NAMES.get(i);
            addDrawableChild(ButtonWidget.builder(Text.literal("预设：" + name), button -> {
                        Set<String> preset = Presets.resolve(name);
                        if (preset != null) {
                            working.clear();
                            working.addAll(preset);
                            rebuild();
                        }
                    })
                    .dimensions(centerX - 90 + (i - 1) * 120, presetY, 110, 18).build());
        }

        // 当前页的方块
        int rowsY = top + 54;
        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int index = start + i;
            if (index >= total) {
                break;
            }
            Block block = matches.get(index);
            String id = Registries.BLOCK.getId(block).toString();
            String label = id + " · " + block.getName().getString();
            CheckboxWidget checkbox = CheckboxWidget.builder(Text.literal(label), textRenderer)
                    .pos(left, rowsY + i * 22)
                    .checked(working.contains(id))
                    .maxWidth(420)
                    .callback((widget, checked) -> {
                        if (checked) {
                            working.add(id);
                        } else {
                            working.remove(id);
                        }
                    })
                    .build();
            addDrawableChild(checkbox);
        }

        // 底部操作
        int bottom = height - 30;
        addDrawableChild(ButtonWidget.builder(Text.literal("全选（当前筛选）"), button -> {
                    for (Block block : filtered()) {
                        working.add(Registries.BLOCK.getId(block).toString());
                    }
                    rebuild();
                })
                .dimensions(left, bottom, 130, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("反选"), button -> {
                    for (Block block : filtered()) {
                        String id = Registries.BLOCK.getId(block).toString();
                        if (!working.remove(id)) {
                            working.add(id);
                        }
                    }
                    rebuild();
                })
                .dimensions(left + 136, bottom, 60, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("应用"), button -> {
                    ConfigManager.get().hidden = new LinkedHashSet<>(working);
                    ConfigManager.get().preset = "自定义";
                    TerrainRadarClient.applyChange();
                    client.setScreen(parent);
                })
                .dimensions(left + 202, bottom, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("取消"), button -> close())
                .dimensions(left + 288, bottom, 80, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        List<Block> matches = filtered();
        int total = matches.size();
        int maxPage = Math.max(0, (total - 1) / PAGE_SIZE);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("第 " + (page + 1) + " / " + (maxPage + 1) + " 页 · 共 " + total + " 个匹配"),
                width / 2, 18, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer,
                Text.literal("已选 " + working.size() + " 个方块（应用后生效）"),
                width / 2 - 210, height - 46, 0xA0A0A0);
    }

    /** 延迟到下一 tick 重建界面，避免在控件回调里直接替换子控件导致并发修改。 */
    private void rebuild() {
        this.client.execute(this::clearAndInit);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
