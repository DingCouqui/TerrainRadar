package com.dingcouqui.terrainradar;

/**
 * lite 变体：多人模式下禁用隐藏方块。打包任务会用本类替换普通构建中的同名类。
 */
public final class MultiplayerFlag {
    public static boolean isEnabledInMultiplayer = false;

    private MultiplayerFlag() {
    }
}
