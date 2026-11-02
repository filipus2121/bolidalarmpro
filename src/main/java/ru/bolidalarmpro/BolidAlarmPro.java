package ru.bolidalarmpro;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import ru.bolidalarmpro.device.DeviceTileEntity;
import ru.bolidalarmpro.network.ModPackets;

@Mod(modid = ModInfo.MODID, name = ModInfo.NAME, version = ModInfo.VERSION,
     dependencies = "required-after:Forge@[10.13.4.1614,)",
     acceptedMinecraftVersions = "[1.7.10]", useMetadata = false)
public final class BolidAlarmPro {
    @Instance(ModInfo.MODID)
    public static BolidAlarmPro instance;

    @SidedProxy(clientSide = "ru.bolidalarmpro.ClientProxy",
                serverSide = "ru.bolidalarmpro.CommonProxy")
    public static CommonProxy proxy;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ModConfig.load(event);
        ModBlocks.init();
        ModItems.init();
        proxy.preInit();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(instance, new ModGuiHandler());
        ModPackets.register();
        FMLCommonHandler.instance().bus().register(new ServerTickHandler());
        GameRegistry.registerTileEntity(DeviceTileEntity.class, "bolid_device");
        proxy.init();
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit();
    }
}
