package ru.bolidalarmpro.network;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import ru.bolidalarmpro.ModInfo;

public final class ModPackets {
    public static SimpleNetworkWrapper INSTANCE;

    public static final int ACT_RESET = 1;
    public static final int ACT_PRESS = 2;
    public static final int ACT_TOGGLE_AUTO = 3;
    public static final int ACT_LOCAL_START = 4;
    public static final int ACT_CANCEL = 5;
    public static final int ACT_TOGGLE_REMOTE_START = 6;
    public static final int ACT_SET_RS_ADDRESS = 7;
    public static final int ACT_SET_DPL_ADDRESS = 8;
    public static final int ACT_SET_CHANNEL = 9;
    public static final int ACT_SET_ASPT_ADDRESS = 10;
    public static final int ACT_TOGGLE_OUTPUT = 11;
    public static final int ACT_TOGGLE_MAINS = 12;
    public static final int ACT_TOGGLE_DOOR = 13;
    public static final int ACT_TOGGLE_SENSOR = 14;
    public static final int ACT_TOGGLE_24V_TEST = 15;
    public static final int ACT_TOGGLE_BATTERY7 = 16;
    public static final int ACT_TOGGLE_BATTERY45 = 17;
    public static final int ACT_TEST_INDICATION = 18;
    public static final int ACT_REPLACE_PYRO = 19;
    public static final int ACT_RESET_DAMAGE = 20;
    public static final int ACT_TOGGLE_REMOTE_PRIORITY = 21;
    public static final int ACT_TOGGLE_AUTO_RESTORE = 22;

    private ModPackets() {}

    public static void register() {
        // FML 1.7.10: NetworkRegistry is an enum singleton and exposes newSimpleChannel,
        // not the newSimpleNetworkWrapper name used by some later tutorials.
        INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(ModInfo.MODID);
        INSTANCE.registerMessage(DeviceActionMessage.Handler.class, DeviceActionMessage.class, 0, Side.SERVER);
        INSTANCE.registerMessage(PowderEffectMessage.Handler.class, PowderEffectMessage.class, 1, Side.CLIENT);
    }

    public static void sendAction(int x, int y, int z, int action, int a, int b) {
        if (INSTANCE != null) INSTANCE.sendToServer(new DeviceActionMessage(x, y, z, action, a, b));
    }

    public static void sendPowderEffect(int x, int y, int z) {
        if (INSTANCE != null) INSTANCE.sendToAll(new PowderEffectMessage(x, y, z));
    }
}
