package ru.bolidalarmpro.sim;

/** Charge/discharge model for РИП-12 (7 А·ч) and С2000-АСПТ (2 x 4,5 А·ч). */
public final class BatteryModel {
    private BatteryModel() {}

    public static void updateRip12(DeviceRuntime r, SimDevice d, boolean mains,
                                   double loadA, double dt, boolean test24V) {
        double capacity = 7.0;
        r.batteryCapacityAh = capacity;
        boolean battery = d.hasBattery7;
        if (battery && !r.batteryPresentLast) r.batteryChargeAh = capacity;
        if (!battery) r.batteryChargeAh = 0.0;
        r.batteryPresentLast = battery;
        r.hasBattery = battery;
        r.mainsPresent = mains;

        if (mains) {
            r.outputShutdown = false;
            if (battery) r.batteryChargeAh = Math.min(capacity, r.batteryChargeAh + 0.7 * dt / 3600.0);
            r.batteryVoltage = 13.6;
            r.outputVoltage = test24V ? 24.0 : 13.6;
        } else if (battery) {
            double discharge = (loadA + 0.040) * dt / 3600.0;
            r.batteryChargeAh = Math.max(0.0, r.batteryChargeAh - discharge);
            double soc = capacity == 0.0 ? 0.0 : r.batteryChargeAh / capacity;
            r.batteryVoltage = 10.5 + 3.1 * soc;
            if (r.batteryVoltage <= 10.2 || r.batteryChargeAh <= 0.0) {
                r.outputVoltage = 0.0;
                r.outputShutdown = true;
            } else {
                r.outputVoltage = r.batteryVoltage;
            }
        } else {
            r.outputVoltage = 0.0;
            r.batteryVoltage = 0.0;
            r.outputShutdown = true;
        }
        r.batterySoc = capacity == 0.0 ? 0.0 : Math.max(0.0, Math.min(1.0, r.batteryChargeAh / capacity));
        r.powered = mains || (battery && r.outputVoltage > 0.0 && !r.outputShutdown);
    }

    public static void updateAspt(DeviceRuntime r, SimDevice d, boolean mains,
                                  double loadA, double dt) {
        double capacity = 9.0; // two 12 V / 4.5 Ah batteries in series
        r.batteryCapacityAh = capacity;
        boolean battery = d.hasBattery45x2;
        if (battery && !r.batteryPresentLast) r.batteryChargeAh = capacity;
        if (!battery) r.batteryChargeAh = 0.0;
        r.batteryPresentLast = battery;
        r.hasBattery = battery;
        r.mainsPresent = mains;

        if (mains) {
            r.outputShutdown = false;
            if (battery) r.batteryChargeAh = Math.min(capacity, r.batteryChargeAh + 0.8 * dt / 3600.0);
            r.batteryVoltage = 26.4;
            r.outputVoltage = 24.0;
        } else if (battery) {
            double discharge = (loadA + 0.060) * dt / 3600.0;
            r.batteryChargeAh = Math.max(0.0, r.batteryChargeAh - discharge);
            double soc = capacity == 0.0 ? 0.0 : r.batteryChargeAh / capacity;
            r.batteryVoltage = 21.0 + 3.0 * soc;
            if (r.batteryVoltage <= 20.4 || r.batteryChargeAh <= 0.0) {
                r.outputVoltage = 0.0;
                r.outputShutdown = true;
            } else {
                r.outputVoltage = r.batteryVoltage;
            }
        } else {
            r.outputVoltage = 0.0;
            r.batteryVoltage = 0.0;
            r.outputShutdown = true;
        }
        r.batterySoc = capacity == 0.0 ? 0.0 : Math.max(0.0, Math.min(1.0, r.batteryChargeAh / capacity));
        r.poweredInternal = mains || (battery && r.outputVoltage >= 20.4 && !r.outputShutdown);
        r.powered = r.poweredInternal;
    }
}
