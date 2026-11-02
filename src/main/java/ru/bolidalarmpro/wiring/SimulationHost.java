package ru.bolidalarmpro.wiring;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import ru.bolidalarmpro.ModConfig;
import ru.bolidalarmpro.device.DeviceTileEntity;
import ru.bolidalarmpro.network.ModPackets;
import ru.bolidalarmpro.sim.CableComponent;
import ru.bolidalarmpro.sim.DeviceRuntime;
import ru.bolidalarmpro.sim.Net;
import ru.bolidalarmpro.sim.NetworkGraph;
import ru.bolidalarmpro.sim.Pos;
import ru.bolidalarmpro.sim.SimEvent;
import ru.bolidalarmpro.sim.Simulation;
import ru.bolidalarmpro.sim.WireKind;
import ru.bolidalarmpro.sim.WireNode;

/** One deterministic simulator per dimension, driven from the server tick. */
public final class SimulationHost {
    private static final Map<Integer, SimulationHost> HOSTS = new HashMap<Integer, SimulationHost>();

    private Simulation sim;
    private long ticks;

    private SimulationHost() {}

    public static SimulationHost get(World world) {
        int dim = world.provider.dimensionId;
        Integer key = Integer.valueOf(dim);
        SimulationHost host = HOSTS.get(key);
        if (host == null) {
            host = new SimulationHost();
            HOSTS.put(key, host);
        }
        return host;
    }

    public void tick(World world, double dt) {
        WiringWorldData data = WiringWorldData.get(world);
        if (sim == null || data.isTopologyDirty()) {
            TopologyScanner.ScanResult scan = TopologyScanner.scan(world, data);
            if (sim == null) sim = new Simulation(scan.wires, scan.devices);
            else sim.setTopology(NetworkGraph.build(scan.wires, scan.devices), scan.devices);
            sim.setTimeScale(ModConfig.timeScale);
            data.clearTopologyDirty();
        }
        sim.tick(dt);
        List<SimEvent> events = sim.drainEvents();
        if (!events.isEmpty()) handleEvents(world, events);
        ticks++;
        if (ticks % ModConfig.syncIntervalTicks == 0) applyRuntime(world);
    }

    private void handleEvents(World world, List<SimEvent> events) {
        for (SimEvent e : events) {
            if (e.pos == null) continue;
            if (e.type == SimEvent.PYRO_FIRED) {
                world.playSoundEffect(e.pos.x + 0.5, e.pos.y + 0.5, e.pos.z + 0.5,
                        "random.fizz", 1.0F, 0.6F);
            } else if (e.type == SimEvent.POWDER_DISCHARGE) {
                extinguishFire(world, e.pos);
                ModPackets.sendPowderEffect(e.pos.x, e.pos.y, e.pos.z);
            } else if (e.type == SimEvent.BUZZER) {
                world.playSoundEffect(e.pos.x + 0.5, e.pos.y + 0.5, e.pos.z + 0.5,
                        "note.pling", 0.4F, 1.2F);
            }
        }
    }

    private void extinguishFire(World world, Pos p) {
        int r = ModConfig.powderRadius;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    int x = p.x + dx, y = p.y + dy, z = p.z + dz;
                    Block b = world.getBlock(x, y, z);
                    if (b == Blocks.fire) world.setBlockToAir(x, y, z);
                }
            }
        }
        world.playSoundEffect(p.x + 0.5, p.y + 0.5, p.z + 0.5, "random.explode", 0.7F, 1.8F);
    }

    private void applyRuntime(World world) {
        for (DeviceRuntime r : sim.getRuntimes().values()) {
            TileEntity te = world.getTileEntity(r.pos.x, r.pos.y, r.pos.z);
            if (te instanceof DeviceTileEntity) {
                ((DeviceTileEntity) te).applyRuntime(r);
                world.markBlockForUpdate(r.pos.x, r.pos.y, r.pos.z);
            }
        }
    }

    public String describeNetAt(Pos pos, WireKind kind, int channel) {
        if (sim == null) return "СИМУЛЯЦИЯ ЕЩЁ НЕ ЗАПУЩЕНА";
        for (Net net : sim.getGraph().getNets()) {
            for (CableComponent c : net.components) {
                if (c.kind != kind || c.channel != channel) continue;
                for (WireNode w : c.wires) {
                    if (!w.pos.equals(pos)) continue;
                    return "Сеть " + net.id + " " + kind.label()
                            + " | " + net.wireCount + " м | R=" + fmt(net.resistanceOhms) + " Ом"
                            + " | U=" + fmt(net.voltage) + " В | I=" + fmt(net.currentA) + " А"
                            + " | EOL=" + net.eolCount + " | 120Ом=" + net.terminatorCount
                            + " | Q=" + (int) Math.round(net.quality * 100.0) + "%"
                            + " | устройств=" + net.devices().size()
                            + (net.fault.length() > 0 ? " | ! " + net.fault : "");
                }
            }
        }
        return "НЕТ АКТИВНОЙ СЕТИ " + kind.label() + "/" + channel;
    }

    private static String fmt(double v) {
        long x = Math.round(v * 10.0);
        return "" + (x / 10.0);
    }
}
