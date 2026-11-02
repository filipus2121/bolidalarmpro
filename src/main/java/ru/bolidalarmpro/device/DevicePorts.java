package ru.bolidalarmpro.device;

import java.util.ArrayList;
import java.util.List;

import ru.bolidalarmpro.sim.DeviceKind;
import ru.bolidalarmpro.sim.Pos;
import ru.bolidalarmpro.sim.SimDevice;
import ru.bolidalarmpro.sim.WireKind;

public final class DevicePorts {
    private DevicePorts() {}

    public static List<String> roles(DeviceKind kind) {
        SimDevice proto = SimDevice.createDefault("ports", kind, new Pos(0, 0, 0));
        return new ArrayList<String>(proto.ports.keySet());
    }

    public static String roleLabel(String role) {
        if ("POWER12_OUT".equals(role)) return "Выход +12 В";
        if ("POWER12_IN".equals(role)) return "Питание +12 В";
        if ("RS485_1".equals(role)) return "RS-485 №1 (A/B)";
        if ("RS485_2".equals(role)) return "RS-485 №2 (A/B)";
        if ("DPL_A".equals(role)) return "ДПЛС клеммы A";
        if ("DPL_B".equals(role)) return "ДПЛС клеммы B";
        if ("DPL".equals(role)) return "ДПЛС";
        if ("LOOP1".equals(role)) return "ШС 1";
        if ("LOOP2".equals(role)) return "ШС 2";
        if ("LOOP3".equals(role)) return "ШС 3";
        if ("DOOR".equals(role)) return "Датчик двери";
        if ("OUT_SO1".equals(role)) return "Выход СО1";
        if ("OUT_SO2".equals(role)) return "Выход СО2";
        if ("OUT_ZO".equals(role)) return "Выход ЗО / сирена";
        if ("OUT_AUX".equals(role)) return "Доп. выход";
        if ("OUT_START".equals(role)) return "Цепь запуска АУП";
        if ("START24_IN".equals(role)) return "Пуск 24 В";
        if ("COIL24".equals(role)) return "Катушка реле 24 В";
        if ("POWER12_COM".equals(role)) return "Контакт COM (+12 В)";
        if ("POWER12_NO1".equals(role)) return "Контакт NO1";
        if ("POWER12_NO2".equals(role)) return "Контакт NO2";
        if ("LOOP".equals(role)) return "Шлейф ШС";
        return role;
    }

    public static String wireLabel(WireKind kind) {
        return kind.label();
    }
}
