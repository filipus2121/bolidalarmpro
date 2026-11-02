package ru.bolidalarmpro;

import java.util.List;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import ru.bolidalarmpro.sim.DeviceKind;

public final class ModItems {
    public static Item programmer;
    public static Item multimeter;
    public static Item shortJumper;
    public static Item eolResistor;
    public static Item terminator;
    public static Item resetKey;
    public static Item battery7;
    public static Item battery45;
    public static Item powderCartridge;

    private ModItems() {}

    public static void init() {
        programmer = register("programmer", 1);
        multimeter = register("multimeter", 1);
        shortJumper = register("short_jumper", 16);
        eolResistor = register("eol_resistor", 16);
        terminator = register("terminator", 16);
        resetKey = register("reset_key", 1);
        battery7 = register("battery_7ah", 8);
        battery45 = register("battery_45ah", 8);
        powderCartridge = register("powder_cartridge", 16);
    }

    private static Item register(String name, int maxStack) {
        ItemBase item = new ItemBase(name, maxStack);
        GameRegistry.registerItem(item, name);
        return item;
    }

    public static boolean isHeld(ItemStack stack, Item item) {
        return stack != null && stack.getItem() == item;
    }

    public static final class ItemBase extends Item {
        private final String name;

        ItemBase(String name, int maxStack) {
            this.name = name;
            setUnlocalizedName(ModInfo.MODID + "." + name);
            setTextureName(ModInfo.RES + name);
            setCreativeTab(ModCreativeTab.TAB);
            setMaxStackSize(maxStack);
        }

        @Override
        public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
            lines.add(StatCollector.translateToLocal("item." + ModInfo.MODID + "." + name + ".desc"));
            if (name.equals("programmer") || name.equals("multimeter")) {
                lines.add("ПКМ по прибору: PProg/UProg; ПКМ по кабелю: канал/диагностика");
            }
        }
    }

    public static String deviceSubName(DeviceKind kind) {
        return "device." + kind.name().toLowerCase();
    }
}
