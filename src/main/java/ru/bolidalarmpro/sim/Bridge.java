package ru.bolidalarmpro.sim;

/** A dry contact inside a relay. When closed, two otherwise separate cable nets are joined. */
public final class Bridge {
    public final String id;
    public final String roleA;
    public final String roleB;
    public boolean closed;

    public Bridge(String id, String roleA, String roleB, boolean closed) {
        this.id = id;
        this.roleA = roleA;
        this.roleB = roleB;
        this.closed = closed;
    }
}
