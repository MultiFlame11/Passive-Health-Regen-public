package com.multiflame.passiveregen.client.gui;

import com.multiflame.passiveregen.PassiveRegenConfig;
import com.multiflame.passiveregen.PassiveRegenMod;
import java.util.Collections;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.config.GuiConfig;

public class PassiveRegenGuiFactory implements IModGuiFactory {
    @Override
    public void initialize(Minecraft minecraftInstance) {
    }

    @Override
    public boolean hasConfigGui() {
        return true;
    }

    @Override
    public GuiScreen createConfigGui(GuiScreen parentScreen) {
        return new GuiConfig(
            parentScreen,
            PassiveRegenMod.MODID,
            false,
            false,
            PassiveRegenMod.NAME,
            PassiveRegenConfig.class
        );
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return Collections.emptySet();
    }
}
