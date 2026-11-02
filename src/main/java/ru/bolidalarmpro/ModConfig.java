package ru.bolidalarmpro;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

public final class ModConfig {
    public static double timeScale = 1.0;
    public static int powderRadius = 6;
    public static boolean powderDamagesNothing = true;
    public static int syncIntervalTicks = 10;
    public static boolean faultChatMessages = false;

    private ModConfig() {}

    public static void load(FMLPreInitializationEvent event) {
        Configuration c = new Configuration(event.getSuggestedConfigurationFile());
        c.load();
        Property p = c.get("simulation", "timeScale", 1.0,
                "Множитель времени симуляции: 1.0 = реальные 15/30-секундные задержки");
        timeScale = Math.max(0.1, Math.min(20.0, p.getDouble(1.0)));
        p = c.get("simulation", "syncIntervalTicks", 10,
                "Как часто отправлять состояние приборов клиенту (в серверных тиках)");
        syncIntervalTicks = Math.max(1, Math.min(100, p.getInt(10)));
        p = c.get("powder", "radius", 6,
                "Радиус облака порошка в блоках: тушит Blocks.fire внутри куба");
        powderRadius = Math.max(1, Math.min(16, p.getInt(6)));
        p = c.get("powder", "cosmeticOnly", true,
                "Порошок тушит только огонь, не наносит урон сущностям");
        powderDamagesNothing = p.getBoolean(true);
        p = c.get("diagnostics", "faultChatMessages", false,
                "Дублировать неисправности линий в чат");
        faultChatMessages = p.getBoolean(false);
        if (c.hasChanged()) c.save();
    }
}
