package ru.bolidalarmpro;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ru.bolidalarmpro.client.RenderDevice;
import ru.bolidalarmpro.device.DeviceTileEntity;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {
    @Override
    public void init() {
        ClientRegistry.bindTileEntitySpecialRenderer(DeviceTileEntity.class, new RenderDevice());
    }
}
