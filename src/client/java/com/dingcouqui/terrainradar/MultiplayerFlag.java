package com.dingcouqui.terrainradar;

/**
 * 构建期开关：是否允许在多人服务器上隐藏方块。
 *
 * <p>普通构建保持 {@code true}；lite 构建会在打包时把本类替换为 {@code false}。
 * 该开关不参与任何配置文件、GUI 或文档。</p>
 */
public final class MultiplayerFlag {
    public static boolean isEnabledInMultiplayer = true;

    private MultiplayerFlag() {
    }
}
