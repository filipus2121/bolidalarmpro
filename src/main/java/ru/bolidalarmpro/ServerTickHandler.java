package ru.bolidalarmpro;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import ru.bolidalarmpro.wiring.SimulationHost;

public final class ServerTickHandler {
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        World[] worlds = DimensionManager.getWorlds();
        if (worlds == null) return;
        for (World world : worlds) {
            if (world == null || world.isRemote) continue;
            SimulationHost.get(world).tick(world, 0.05);
        }
    }
}
