package ru.bolidalarmpro.sim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** An electrical net: one or more cable components joined by closed relay contacts. */
public final class Net {
    public final int id;
    public final WireKind kind;
    public final List<CableComponent> components = new ArrayList<CableComponent>();
    public final List<DevicePortRef> ports = new ArrayList<DevicePortRef>();

    public double resistanceOhms;
    public int wireCount;
    public int eolCount;
    public int terminatorCount;
    public int shortCount;
    public int swapCount;
    public int polarityCount;
    public int edgeCount;
    public boolean cycle;
    public boolean ringIntact;

    public double voltage;
    public double currentA;
    public double quality = 1.0;
    public boolean commOk = true;
    public String fault = "";

    public Net(int id, WireKind kind) {
        this.id = id;
        this.kind = kind;
    }

    public void recalculate() {
        resistanceOhms = 0.0;
        wireCount = 0;
        eolCount = 0;
        terminatorCount = 0;
        shortCount = 0;
        swapCount = 0;
        polarityCount = 0;
        edgeCount = 0;
        cycle = false;
        for (CableComponent c : components) {
            resistanceOhms += c.resistanceOhms;
            wireCount += c.wireCount();
            eolCount += c.eolCount;
            terminatorCount += c.terminatorCount;
            shortCount += c.shortCount;
            swapCount += c.swapCount;
            polarityCount += c.polarityCount;
            edgeCount += c.edgeCount;
            if (c.hasCycle()) cycle = true;
        }
    }

    public List<SimDevice> devices() {
        Map<String, SimDevice> unique = new HashMap<String, SimDevice>();
        for (DevicePortRef p : ports) unique.put(p.device.id, p.device);
        return new ArrayList<SimDevice>(unique.values());
    }

    public List<SimDevice> devicesByKind(DeviceKind kind) {
        List<SimDevice> out = new ArrayList<SimDevice>();
        for (SimDevice d : devices()) if (d.kind == kind) out.add(d);
        return out;
    }

    public boolean contains(SimDevice device) {
        for (DevicePortRef p : ports) if (p.device.id.equals(device.id)) return true;
        return false;
    }

    public List<DevicePortRef> portsByRole(String role) {
        List<DevicePortRef> out = new ArrayList<DevicePortRef>();
        for (DevicePortRef p : ports) if (p.role.equals(role)) out.add(p);
        return out;
    }

    public boolean hasShort() { return shortCount > 0; }
    public boolean hasCycle() { return cycle; }

    @Override
    public String toString() {
        return "Сеть " + id + " " + kind.label() + " " + wireCount + " м" + (fault.length() > 0 ? " [" + fault + "]" : "");
    }
}
