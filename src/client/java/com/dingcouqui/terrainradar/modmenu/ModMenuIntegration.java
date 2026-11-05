package com.dingcouqui.terrainradar.modmenu;

import com.dingcouqui.terrainradar.gui.TerrainRadarHomeScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screen.Screen;

/** ModMenu 集成：点 TerrainRadar 的配置按钮进入中转页。 */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return TerrainRadarHomeScreen::new;
    }
}
