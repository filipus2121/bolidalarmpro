package ru.bolidalarmpro;

import cpw.mods.fml.common.network.IGuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import ru.bolidalarmpro.gui.GuiDevice;

public final class ModGuiHandler implements IGuiHandler {
    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        return null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == ModInfo.GUI_DEVICE) return new GuiDevice(player, world, x, y, z);
        return null;
    }
}
