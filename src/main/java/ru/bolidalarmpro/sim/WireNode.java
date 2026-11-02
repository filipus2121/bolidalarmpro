package ru.bolidalarmpro.sim;

public final class WireNode {
    public final Pos pos;
    public final WireKind kind;
    public final int channel;
    public final CableFlags flags;

    public WireNode(Pos pos, WireKind kind, int channel, CableFlags flags) {
        this.pos = pos;
        this.kind = kind;
        this.channel = channel & 15;
        this.flags = flags == null ? new CableFlags() : flags;
    }
}
