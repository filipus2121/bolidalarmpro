package ru.bolidalarmpro.sim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** One connected run of physically identical cable blocks. */
public final class CableComponent {
    public final int id;
    public final WireKind kind;
    public final int channel;
    public final List<WireNode> wires = new ArrayList<WireNode>();
    public final List<DevicePortRef> ports = new ArrayList<DevicePortRef>();

    public double resistanceOhms;
    public int eolCount;
    public int terminatorCount;
    public int shortCount;
    public int swapCount;
    public int polarityCount;
    public int edgeCount;
    public boolean cycle;

    public CableComponent(int id, WireKind kind, int channel) {
        this.id = id;
        this.kind = kind;
        this.channel = channel;
    }

    public boolean hasCycle() { return cycle; }
    public boolean shorted() { return shortCount > 0; }
    public int wireCount() { return wires.size(); }

    public void calculate(Map<Pos, Integer> posToComponent) {
        resistanceOhms = wires.size() * kind.loopResistancePerBlock();
        eolCount = 0;
        terminatorCount = 0;
        shortCount = 0;
        swapCount = 0;
        polarityCount = 0;
        for (WireNode w : wires) {
            if (w.flags.eol) eolCount++;
            if (w.flags.terminator) terminatorCount++;
            if (w.flags.shortFault) shortCount++;
            if (w.flags.swappedAB) swapCount++;
            if (w.flags.reversedPolarity) polarityCount++;
        }
        Map<Pos, WireNode> local = new HashMap<Pos, WireNode>();
        for (WireNode w : wires) local.put(w.pos, w);
        edgeCount = 0;
        for (WireNode w : wires) {
            Pos p = w.pos;
            Pos[] neighbours = new Pos[] {
                p.offset(1, 0, 0), p.offset(-1, 0, 0),
                p.offset(0, 1, 0), p.offset(0, -1, 0),
                p.offset(0, 0, 1), p.offset(0, 0, -1)
            };
            for (Pos n : neighbours) {
                if (!local.containsKey(n)) continue;
                Integer comp = posToComponent.get(n);
                if (comp == null || comp.intValue() != id) continue;
                // Count each undirected edge exactly once.
                if (n.x > p.x || (n.x == p.x && (n.y > p.y || (n.y == p.y && n.z > p.z)))) {
                    edgeCount++;
                }
            }
        }
        // In a connected graph a cycle exists as soon as E >= V.
        cycle = wires.size() > 0 && edgeCount >= wires.size();
    }

    @Override
    public String toString() {
        return kind.id() + "/" + channel + " (" + wires.size() + " м, R=" + resistanceOhms + " Ом)";
    }
}
