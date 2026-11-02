package ru.bolidalarmpro;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

public final class ModCreativeTab extends CreativeTabs {
    public static final ModCreativeTab TAB = new ModCreativeTab();

    private ModCreativeTab() {
        super(ModInfo.MODID);
    }

    @Override
    public Item getTabIconItem() {
        return Item.getItemFromBlock(ModBlocks.deviceBlock);
    }

    @Override
    public String getTranslatedTabLabel() {
        return "BOLID / Орион - стенд ОПС";
    }
}
