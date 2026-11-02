package ru.bolidalarmpro.wiring;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;
import ru.bolidalarmpro.sim.CableFlags;
import ru.bolidalarmpro.sim.Pos;

/** Per-world registry of cable blocks, device blocks and injected cable faults. */
public final class WiringWorldData extends WorldSavedData {
    public static final String ID = "bolidalarmpro_wiring";

    private final Map<Pos, CableFlags> flags = new HashMap<Pos, CableFlags>();
    private final Set<Pos> cables = new HashSet<Pos>();
    private final Set<Pos> devices = new HashSet<Pos>();
    private boolean topologyDirty = true;

    public WiringWorldData(String name) {
        super(name);
    }

    public static WiringWorldData get(World world) {
        MapStorage storage = world.perWorldStorage;
        WiringWorldData data = (WiringWorldData) storage.loadData(WiringWorldData.class, ID);
        if (data == null) {
            data = new WiringWorldData(ID);
            storage.setData(ID, data);
        }
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        flags.clear();
        cables.clear();
        devices.clear();
        NBTTagList cableList = nbt.getTagList("cables", 10);
        for (int i = 0; i < cableList.tagCount(); i++) {
            NBTTagCompound e = cableList.getCompoundTagAt(i);
            Pos p = new Pos(e.getInteger("x"), e.getInteger("y"), e.getInteger("z"));
            cables.add(p);
            int bits = e.getInteger("flags");
            if (bits != 0) flags.put(p, CableFlags.fromBits(bits));
        }
        NBTTagList deviceList = nbt.getTagList("devices", 10);
        for (int i = 0; i < deviceList.tagCount(); i++) {
            NBTTagCompound e = deviceList.getCompoundTagAt(i);
            devices.add(new Pos(e.getInteger("x"), e.getInteger("y"), e.getInteger("z")));
        }
        topologyDirty = true;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        NBTTagList cableList = new NBTTagList();
        for (Pos p : cables) {
            NBTTagCompound e = new NBTTagCompound();
            e.setInteger("x", p.x);
            e.setInteger("y", p.y);
            e.setInteger("z", p.z);
            e.setInteger("flags", getFlags(p).toBits());
            cableList.appendTag(e);
        }
        nbt.setTag("cables", cableList);
        NBTTagList deviceList = new NBTTagList();
        for (Pos p : devices) {
            NBTTagCompound e = new NBTTagCompound();
            e.setInteger("x", p.x);
            e.setInteger("y", p.y);
            e.setInteger("z", p.z);
            deviceList.appendTag(e);
        }
        nbt.setTag("devices", deviceList);
    }

    public void markTopologyDirty() {
        topologyDirty = true;
        markDirty();
    }

    public boolean isTopologyDirty() { return topologyDirty; }
    public void clearTopologyDirty() { topologyDirty = false; }

    public void addCable(Pos p) {
        cables.add(p);
        markTopologyDirty();
    }

    public void removeCable(Pos p) {
        cables.remove(p);
        flags.remove(p);
        markTopologyDirty();
    }

    public void addDevice(Pos p) {
        devices.add(p);
        markTopologyDirty();
    }

    public void removeDevice(Pos p) {
        devices.remove(p);
        markTopologyDirty();
    }

    public CableFlags getFlags(Pos p) {
        CableFlags f = flags.get(p);
        if (f == null) {
            f = new CableFlags();
            flags.put(p, f);
        }
        return f;
    }

    public void setFlags(Pos p, CableFlags f) {
        if (f == null || f.empty()) flags.remove(p);
        else flags.put(p, f);
        markTopologyDirty();
    }

    public Set<Pos> allCables() { return new HashSet<Pos>(cables); }
    public Set<Pos> allDevices() { return new HashSet<Pos>(devices); }
}
