package com.dingcouqui.terrainradar;

import com.dingcouqui.terrainradar.config.ConfigManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/** 「隐藏方块（总开关）」的按键绑定：默认不绑定，玩家在「选项 → 控制」里自行设置。 */
public final class ToggleKeyBinding {
    public static final String TRANSLATION_KEY = "key.terrainradar.toggle_enabled";
    private static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of(TerrainRadarClient.MOD_ID, "main"));
    private static KeyBinding keyBinding;

    private ToggleKeyBinding() {
    }

    public static void register() {
        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                TRANSLATION_KEY, InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (keyBinding.wasPressed()) {
                // 避免在聊天/搜索框/任意 GUI 中输入时误触发
                if (client.currentScreen == null) {
                    toggle(client);
                }
            }
        });
    }

    private static void toggle(MinecraftClient client) {
        boolean enabled = !ConfigManager.get().enabled;
        ConfigManager.get().enabled = enabled;
        TerrainRadarClient.applyChange();
        client.inGameHud.setOverlayMessage(
                Text.literal("TerrainRadar " + (enabled ? "已开启" : "已关闭")), false);
    }
}
