package ru.bolidalarmpro.wiring;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import ru.bolidalarmpro.device.DeviceTileEntity;
import ru.bolidalarmpro.sim.Pos;
import ru.bolidalarmpro.sim.SimDevice;
import ru.bolidalarmpro.sim.WireNode;

public final class TopologyScanner {
    public static final class ScanResult {
        public final List<WireNode> wires = new ArrayList<WireNode>();
        public final List<SimDevice> devices = new ArrayList<SimDevice>();
    }

    private TopologyScanner() {}

    public static ScanResult scan(World world, WiringWorldData data) {
        ScanResult result = new ScanResult();
        Set<Pos> cablePositions = data.allCables();
        for (Pos p : cablePositions) {
            if (!world.blockExists(p.x, p.y, p.z)) continue;
            Block block = world.getBlock(p.x, p.y, p.z);
            if (!(block instanceof BlockCable)) continue;
            BlockCable cable = (BlockCable) block;
            int channel = world.getBlockMetadata(p.x, p.y, p.z) & 15;
            result.wires.add(new WireNode(p, cable.kind, channel, data.getFlags(p)));
        }
        Set<Pos> devicePositions = data.allDevices();
        for (Pos p : devicePositions) {
            if (!world.blockExists(p.x, p.y, p.z)) continue;
            TileEntity te = world.getTileEntity(p.x, p.y, p.z);
            if (!(te instanceof DeviceTileEntity)) continue;
            SimDevice d = ((DeviceTileEntity) te).toSimDevice();
            if (d.kind.isLoopSensor()) {
                d.sensorTriggered = d.sensorTriggered || fireNearby(world, d.pos);
            }
            result.devices.add(d);
        }
        return result;
    }

    private static boolean fireNearby(World world, Pos p) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (world.getBlock(p.x + dx, p.y + dy, p.z + dz) == Blocks.fire) return true;
                }
            }
        }
        return false;
    }
}
