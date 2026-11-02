package ru.bolidalarmpro;

import java.util.HashMap;
import java.util.Map;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import ru.bolidalarmpro.device.DeviceBlock;
import ru.bolidalarmpro.sim.WireKind;
import ru.bolidalarmpro.wiring.BlockCable;

public final class ModBlocks {
    public static Block deviceBlock;
    public static final Map<WireKind, BlockCable> CABLES = new HashMap<WireKind, BlockCable>();

    private ModBlocks() {}

    public static void init() {
        deviceBlock = new DeviceBlock();
        GameRegistry.registerBlock(deviceBlock, ItemBlock.class, "device");
        for (WireKind kind : WireKind.values()) {
            BlockCable cable = new BlockCable(kind);
            CABLES.put(kind, cable);
            GameRegistry.registerBlock(cable, ItemBlock.class, "cable_" + kind.id());
        }
    }

    public static BlockCable getCable(WireKind kind) {
        return CABLES.get(kind);
    }
}
