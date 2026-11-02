package ru.bolidalarmpro.sim;

public final class DevicePortRef {
    public final SimDevice device;
    public final String role;
    public final PortKey key;

    public DevicePortRef(SimDevice device, String role, PortKey key) {
        this.device = device;
        this.role = role;
        this.key = key;
    }

    public String id() { return device.id + "|" + role; }

    @Override
    public String toString() { return device.kind.displayName() + " " + role; }
}
