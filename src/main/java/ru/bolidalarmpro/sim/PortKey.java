package ru.bolidalarmpro.sim;

/** A named terminal of a device. The cable network is selected by (kind, channel). */
public final class PortKey {
    public final WireKind kind;
    public final int channel;
    public final String role;

    public PortKey(WireKind kind, int channel, String role) {
        this.kind = kind;
        this.channel = channel & 15;
        this.role = role;
    }

    public boolean sameCable(PortKey other) {
        return other != null && kind == other.kind && channel == other.channel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PortKey)) return false;
        PortKey p = (PortKey) o;
        return kind == p.kind && channel == p.channel && role.equals(p.role);
    }

    @Override
    public int hashCode() {
        return kind.hashCode() * 31 * 31 + channel * 31 + role.hashCode();
    }

    @Override
    public String toString() {
        return role + ":" + kind.id() + "/" + channel;
    }
}
