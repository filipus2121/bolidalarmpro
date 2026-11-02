package ru.bolidalarmpro.sim;

import java.io.Serializable;

/** Immutable block coordinate. Used by both the pure simulator and the Forge adapter. */
public final class Pos implements Serializable {
    private static final long serialVersionUID = 1L;
    public final int x;
    public final int y;
    public final int z;

    public Pos(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int manhattan(Pos other) {
        return Math.abs(x - other.x) + Math.abs(y - other.y) + Math.abs(z - other.z);
    }

    public double distance(Pos other) {
        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public Pos offset(int dx, int dy, int dz) {
        return new Pos(x + dx, y + dy, z + dz);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pos)) return false;
        Pos p = (Pos) o;
        return x == p.x && y == p.y && z == p.z;
    }

    @Override
    public int hashCode() {
        return (x * 73856093) ^ (y * 19349663) ^ (z * 83492791);
    }

    @Override
    public String toString() {
        return x + "," + y + "," + z;
    }
}
