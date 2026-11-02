package ru.bolidalarmpro.sim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class NetworkGraph {
    private final List<CableComponent> components = new ArrayList<CableComponent>();
    private final List<SimDevice> devices = new ArrayList<SimDevice>();
    private final Map<String, SimDevice> byId = new HashMap<String, SimDevice>();
    private final Map<Pos, SimDevice> byPos = new HashMap<Pos, SimDevice>();
    private final Map<String, Integer> portToComponent = new HashMap<String, Integer>();
    private final Map<String, Integer> portToNet = new HashMap<String, Integer>();
    private final List<Net> nets = new ArrayList<Net>();

    private NetworkGraph() {}

    public static NetworkGraph build(List<WireNode> wires, List<SimDevice> deviceList) {
        NetworkGraph g = new NetworkGraph();
        g.devices.addAll(deviceList);
        for (SimDevice d : deviceList) {
            g.byId.put(d.id, d);
            g.byPos.put(d.pos, d);
        }

        Map<Pos, WireNode> wireAt = new HashMap<Pos, WireNode>();
        Map<Pos, Integer> wireIndex = new HashMap<Pos, Integer>();
        for (int i = 0; i < wires.size(); i++) {
            wireAt.put(wires.get(i).pos, wires.get(i));
            wireIndex.put(wires.get(i).pos, Integer.valueOf(i));
        }
        boolean[] used = new boolean[wires.size()];
        Map<Pos, Integer> posToComponent = new HashMap<Pos, Integer>();

        for (int i = 0; i < wires.size(); i++) {
            if (used[i]) continue;
            WireNode start = wires.get(i);
            CableComponent comp = new CableComponent(g.components.size(), start.kind, start.channel);
            ArrayList<WireNode> queue = new ArrayList<WireNode>();
            queue.add(start);
            used[i] = true;
            int cursor = 0;
            while (cursor < queue.size()) {
                WireNode cur = queue.get(cursor++);
                comp.wires.add(cur);
                posToComponent.put(cur.pos, Integer.valueOf(comp.id));
                Pos p = cur.pos;
                Pos[] nbs = new Pos[] {
                    p.offset(1, 0, 0), p.offset(-1, 0, 0),
                    p.offset(0, 1, 0), p.offset(0, -1, 0),
                    p.offset(0, 0, 1), p.offset(0, 0, -1)
                };
                for (Pos np : nbs) {
                    WireNode nb = wireAt.get(np);
                    if (nb == null || nb.kind != cur.kind || nb.channel != cur.channel) continue;
                    Integer idx = wireIndex.get(np);
                    if (idx == null || used[idx.intValue()]) continue;
                    used[idx.intValue()] = true;
                    queue.add(nb);
                }
            }
            comp.calculate(posToComponent);
            g.components.add(comp);
        }

        for (CableComponent comp : g.components) {
            Set<String> seen = new HashSet<String>();
            // A terminal block may be connected by short jumpers up to three blocks away.
            // This lets one panel expose many independent cable lanes on its six sides.
            for (WireNode w : comp.wires) {
                Pos p = w.pos;
                for (int dx = -3; dx <= 3; dx++) {
                    for (int dy = -3; dy <= 3; dy++) {
                        for (int dz = -3; dz <= 3; dz++) {
                            if (dx == 0 && dy == 0 && dz == 0) continue;
                            SimDevice d = g.byPos.get(p.offset(dx, dy, dz));
                            if (d == null) continue;
                            for (Map.Entry<String, PortKey> e : d.ports.entrySet()) {
                                PortKey k = e.getValue();
                                if (k.kind != comp.kind || k.channel != comp.channel) continue;
                                String pid = d.id + "|" + e.getKey();
                                if (!seen.add(pid)) continue;
                                DevicePortRef ref = new DevicePortRef(d, e.getKey(), k);
                                comp.ports.add(ref);
                                g.portToComponent.put(pid, Integer.valueOf(comp.id));
                            }
                        }
                    }
                }
            }
        }
        g.applyBridges();
        return g;
    }

    private static final class UnionFind {
        private final int[] parent;
        UnionFind(int n) {
            parent = new int[n];
            for (int i = 0; i < n; i++) parent[i] = i;
        }
        int find(int x) {
            while (parent[x] != x) x = parent[x] = parent[parent[x]];
            return x;
        }
        void union(int a, int b) {
            int ra = find(a);
            int rb = find(b);
            if (ra != rb) parent[rb] = ra;
        }
    }

    /** Recompute electrical nets after relay contacts change state. */
    public void applyBridges() {
        UnionFind uf = new UnionFind(components.size());
        for (SimDevice d : devices) {
            for (Bridge b : d.bridges) {
                if (!b.closed) continue;
                Integer a = portToComponent.get(d.id + "|" + b.roleA);
                Integer c = portToComponent.get(d.id + "|" + b.roleB);
                if (a == null || c == null) continue;
                CableComponent ca = components.get(a.intValue());
                CableComponent cb = components.get(c.intValue());
                if (ca.kind != cb.kind) continue;
                uf.union(a.intValue(), c.intValue());
            }
        }
        Map<Integer, List<CableComponent>> groups = new HashMap<Integer, List<CableComponent>>();
        for (CableComponent c : components) {
            Integer root = Integer.valueOf(uf.find(c.id));
            List<CableComponent> list = groups.get(root);
            if (list == null) {
                list = new ArrayList<CableComponent>();
                groups.put(root, list);
            }
            list.add(c);
        }
        nets.clear();
        portToNet.clear();
        for (Map.Entry<Integer, List<CableComponent>> e : groups.entrySet()) {
            List<CableComponent> group = e.getValue();
            if (group.isEmpty()) continue;
            Net net = new Net(nets.size(), group.get(0).kind);
            for (CableComponent c : group) {
                net.components.add(c);
                net.ports.addAll(c.ports);
            }
            net.recalculate();
            nets.add(net);
            for (DevicePortRef p : net.ports) portToNet.put(p.id(), Integer.valueOf(net.id));
        }
    }

    public Net getNetForPort(SimDevice device, String role) {
        Integer idx = portToNet.get(device.id + "|" + role);
        return idx == null ? null : nets.get(idx.intValue());
    }

    public Net getNetForPort(String deviceId, String role) {
        Integer idx = portToNet.get(deviceId + "|" + role);
        return idx == null ? null : nets.get(idx.intValue());
    }

    public List<Net> getNetsOfKind(WireKind kind) {
        List<Net> out = new ArrayList<Net>();
        for (Net n : nets) if (n.kind == kind) out.add(n);
        return out;
    }

    public List<Net> getNets() { return nets; }
    public List<CableComponent> getComponents() { return components; }
    public List<SimDevice> getDevices() { return devices; }
    public SimDevice getDeviceById(String id) { return byId.get(id); }

    public List<Net> getPortNets(SimDevice device, WireKind kind) {
        List<Net> out = new ArrayList<Net>();
        Set<Integer> seen = new HashSet<Integer>();
        for (String role : device.rolesOfKind(kind)) {
            Net n = getNetForPort(device, role);
            if (n != null && seen.add(Integer.valueOf(n.id))) out.add(n);
        }
        return out;
    }
}
