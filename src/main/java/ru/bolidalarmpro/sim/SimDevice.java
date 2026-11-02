package ru.bolidalarmpro.sim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Mutable mirror of one placed device. The Forge side rebuilds these from tile entities. */
public final class SimDevice {
    public final String id;
    public final DeviceKind kind;
    public final Pos pos;
    public final Map<String, PortKey> ports = new HashMap<String, PortKey>();
    public final List<Bridge> bridges = new ArrayList<Bridge>();

    public int rsAddress = 1;
    public int dplAddress = 1;
    public int facing = 2;
    public int configuredAsptAddress = 2;

    public boolean mainsEnabled = true;
    public boolean hasBattery7 = false;
    public boolean hasBattery45x2 = false;
    public boolean autoEnabled = true;
    public boolean remoteStartEnabled = false;
    public boolean remotePriority = false;
    public boolean autoRestore = false;
    public boolean test24V = false;

    public boolean doorOpen = false;
    public boolean sensorTriggered = false;
    public boolean alarmLatched = false;
    public boolean resetRequested = false;
    public boolean localStartRequested = false;
    public boolean cancelRequested = false;
    public boolean autoToggleRequested = false;
    public boolean testIndicationRequested = false;
    public boolean spent = false;
    public boolean damaged = false;
    public boolean coilClosed = false;

    public boolean[] outputEnabled = new boolean[] {true, false, false, false, true};

    public SimDevice(String id, DeviceKind kind, Pos pos) {
        this.id = id;
        this.kind = kind;
        this.pos = pos;
        if (kind == DeviceKind.S2000M_205 || kind == DeviceKind.S2000M_502_02) rsAddress = 127;
        if (kind == DeviceKind.KDL_236) rsAddress = 1;
        if (kind == DeviceKind.ASPT_353) rsAddress = 2;
        if (kind == DeviceKind.UDP_513_3AM) dplAddress = 2;
        if (kind == DeviceKind.S2000M_502_02) remoteStartEnabled = true;
        addDefaultPorts();
    }

    public static SimDevice createDefault(String id, DeviceKind kind, Pos pos) {
        return new SimDevice(id, kind, pos);
    }

    private void port(String role, WireKind kind, int channel) {
        ports.put(role, new PortKey(kind, channel, role));
    }

    private void addDefaultPorts() {
        if (kind == DeviceKind.RIP_12) {
            port("POWER12_OUT", WireKind.POWER12, 0);
        } else if (kind == DeviceKind.S2000M_205) {
            port("POWER12_IN", WireKind.POWER12, 0);
            port("RS485_1", WireKind.RS485, 0);
        } else if (kind == DeviceKind.S2000M_502_02) {
            port("POWER12_IN", WireKind.POWER12, 0);
            port("RS485_1", WireKind.RS485, 0);
            port("RS485_2", WireKind.RS485, 1);
        } else if (kind == DeviceKind.KDL_236) {
            port("POWER12_IN", WireKind.POWER12, 0);
            port("RS485_1", WireKind.RS485, 0);
            port("DPL_A", WireKind.DPL, 0);
            port("DPL_B", WireKind.DPL, 0);
        } else if (kind == DeviceKind.ASPT_353) {
            port("RS485_1", WireKind.RS485, 0);
            port("LOOP1", WireKind.FIRE_ZONE, 1);
            port("LOOP2", WireKind.FIRE_ZONE, 2);
            port("LOOP3", WireKind.FIRE_ZONE, 3);
            port("DOOR", WireKind.FIRE_ZONE, 4);
            port("OUT_SO1", WireKind.CONTROL24, 1);
            port("OUT_SO2", WireKind.CONTROL24, 2);
            port("OUT_ZO", WireKind.CONTROL24, 3);
            port("OUT_AUX", WireKind.CONTROL24, 4);
            port("OUT_START", WireKind.START24, 1);
        } else if (kind == DeviceKind.IPR_513_3AM || kind == DeviceKind.UDP_513_3AM) {
            port("DPL", WireKind.DPL, 0);
        } else if (kind == DeviceKind.PYRO) {
            port("START24_IN", WireKind.START24, 1);
        } else if (kind == DeviceKind.TABLO_NE_VHODI) {
            port("POWER12_IN", WireKind.POWER12, 2);
        } else if (kind == DeviceKind.TABLO_UHODI) {
            port("POWER12_IN", WireKind.POWER12, 1);
        } else if (kind == DeviceKind.RELAY_12) {
            port("COIL24", WireKind.CONTROL24, 1);
            port("POWER12_COM", WireKind.POWER12, 0);
            port("POWER12_NO1", WireKind.POWER12, 1);
            port("POWER12_NO2", WireKind.POWER12, 2);
            bridges.add(new Bridge("K1", "POWER12_COM", "POWER12_NO1", false));
            bridges.add(new Bridge("K2", "POWER12_COM", "POWER12_NO2", false));
        } else if (kind == DeviceKind.LOOP_SENSOR) {
            port("LOOP", WireKind.FIRE_ZONE, 1);
        } else if (kind == DeviceKind.DOOR_CONTACT) {
            port("DOOR", WireKind.FIRE_ZONE, 4);
        }
    }

    public PortKey getPortKey(String role) {
        return ports.get(role);
    }

    public int getChannel(String role) {
        PortKey k = ports.get(role);
        return k == null ? -1 : k.channel;
    }

    public void setChannel(String role, int channel) {
        PortKey old = ports.get(role);
        if (old != null) ports.put(role, new PortKey(old.kind, channel, role));
    }

    public List<String> rolesOfKind(WireKind kind) {
        List<String> out = new ArrayList<String>();
        for (Map.Entry<String, PortKey> e : ports.entrySet()) {
            if (e.getValue().kind == kind) out.add(e.getKey());
        }
        return out;
    }

    public Bridge findBridge(String id) {
        for (Bridge b : bridges) if (b.id.equals(id)) return b;
        return null;
    }

    public boolean isBridgeClosed(String id) {
        Bridge b = findBridge(id);
        return b != null && b.closed;
    }

    public void setBridgeClosed(String id, boolean closed) {
        Bridge b = findBridge(id);
        if (b != null) b.closed = closed;
    }

    @Override
    public String toString() {
        return kind.displayName() + " @" + pos;
    }
}
