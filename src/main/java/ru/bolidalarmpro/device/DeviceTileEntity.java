package ru.bolidalarmpro.device;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import ru.bolidalarmpro.network.ModPackets;
import ru.bolidalarmpro.sim.DeviceKind;
import ru.bolidalarmpro.sim.DeviceRuntime;
import ru.bolidalarmpro.sim.PortKey;
import ru.bolidalarmpro.sim.Pos;
import ru.bolidalarmpro.sim.SimDevice;
import ru.bolidalarmpro.wiring.WiringWorldData;

/** One tile entity for all 10 devices and the laboratory infrastructure. */
public final class DeviceTileEntity extends TileEntity {
    public DeviceKind kind = DeviceKind.RIP_12;
    public int facing = 2;
    public int rsAddress = 127;
    public int dplAddress = 1;
    public int configuredAsptAddress = 2;
    public final Map<String, Integer> channels = new HashMap<String, Integer>();

    public boolean mainsEnabled = true;
    public boolean hasBattery7 = false;
    public boolean hasBattery45x2 = false;
    public boolean autoEnabled = true;
    public boolean remoteStartEnabled = false;
    public boolean remotePriority = false;
    public boolean autoRestore = false;
    public boolean test24V = false;
    public boolean doorOpen = false;
    public boolean sensorTriggered = false;
    public boolean alarmLatched = false;
    public boolean spent = false;
    public boolean damaged = false;
    public boolean coilClosed = false;

    public boolean resetRequested = false;
    public boolean localStartRequested = false;
    public boolean cancelRequested = false;
    public boolean autoToggleRequested = false;
    public boolean testIndicationRequested = false;

    public boolean[] outputEnabled = new boolean[] {true, false, false, false, true};

    // Synced to the client for GUI and TESR.
    public String clientStatusText = "";
    public String clientLogText = "";
    public String clientLcd0 = "";
    public String clientLcd1 = "";
    public String clientMode = "ДЕЖУРНЫЙ";
    public String clientFault = "";
    public boolean[] clientLeds = new boolean[4];
    public boolean clientLit = false;
    public boolean clientAlarm = false;
    public boolean clientPowered = false;
    public boolean clientOnline = false;
    public boolean clientSpent = false;
    public boolean clientDamaged = false;
    public boolean clientCoilClosed = false;
    public boolean clientFlash = false;
    public boolean clientBuzzer = false;
    public double clientVoltage = 0.0;
    public double clientCurrent = 0.0;
    public double clientBatterySoc = 0.0;
    public double clientRsQuality = 0.0;
    public double clientDplVoltage = 0.0;
    public double clientDplResistance = 0.0;
    public double clientDelay = 0.0;
    public double clientStart = 0.0;
    public int clientRsAddress = 127;
    public int clientDplAddress = 1;
    public int clientConfiguredAsptAddress = 2;
    public int clientConnected = 0;
    public int clientRsQualityPercent = 0;
    public int clientFacing = 2;

    public void setKindFromMetadata(int meta) {
        int ord = meta & 15;
        DeviceKind[] all = DeviceKind.values();
        if (ord < 0 || ord >= all.length) ord = 0;
        this.kind = all[ord];
        configureDefaults();
    }

    private void configureDefaults() {
        SimDevice proto = SimDevice.createDefault("defaults", kind, new Pos(0, 0, 0));
        channels.clear();
        for (Map.Entry<String, PortKey> e : proto.ports.entrySet()) {
            channels.put(e.getKey(), Integer.valueOf(e.getValue().channel));
        }
        rsAddress = proto.rsAddress;
        dplAddress = proto.dplAddress;
        configuredAsptAddress = proto.configuredAsptAddress;
        remoteStartEnabled = proto.remoteStartEnabled;
        for (int i = 0; i < 5; i++) outputEnabled[i] = proto.outputEnabled[i];
        autoEnabled = true;
        mainsEnabled = true;
        test24V = false;
        remotePriority = false;
        autoRestore = false;
    }

    private void ensureChannels() {
        SimDevice proto = SimDevice.createDefault("defaults", kind, new Pos(0, 0, 0));
        for (Map.Entry<String, PortKey> e : proto.ports.entrySet()) {
            if (!channels.containsKey(e.getKey())) channels.put(e.getKey(), Integer.valueOf(e.getValue().channel));
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setString("kind", kind.name());
        nbt.setInteger("facing", facing);
        nbt.setInteger("rsAddress", rsAddress);
        nbt.setInteger("dplAddress", dplAddress);
        nbt.setInteger("asptAddress", configuredAsptAddress);
        nbt.setBoolean("mains", mainsEnabled);
        nbt.setBoolean("bat7", hasBattery7);
        nbt.setBoolean("bat45", hasBattery45x2);
        nbt.setBoolean("auto", autoEnabled);
        nbt.setBoolean("remoteStart", remoteStartEnabled);
        nbt.setBoolean("remotePriority", remotePriority);
        nbt.setBoolean("autoRestore", autoRestore);
        nbt.setBoolean("test24", test24V);
        nbt.setBoolean("door", doorOpen);
        nbt.setBoolean("sensor", sensorTriggered);
        nbt.setBoolean("alarm", alarmLatched);
        nbt.setBoolean("spent", spent);
        nbt.setBoolean("damaged", damaged);
        nbt.setBoolean("coil", coilClosed);
        NBTTagList list = new NBTTagList();
        for (Map.Entry<String, Integer> e : channels.entrySet()) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setString("role", e.getKey());
            entry.setInteger("channel", e.getValue().intValue());
            list.appendTag(entry);
        }
        nbt.setTag("channels", list);
        NBTTagCompound outputs = new NBTTagCompound();
        for (int i = 0; i < 5; i++) outputs.setBoolean("oe" + i, outputEnabled[i]);
        nbt.setTag("outputs", outputs);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        String kindName = nbt.getString("kind");
        try {
            kind = DeviceKind.valueOf(kindName);
        } catch (IllegalArgumentException e) {
            kind = DeviceKind.RIP_12;
        }
        configureDefaults();
        if (nbt.hasKey("facing")) facing = nbt.getInteger("facing");
        if (facing < 2 || facing > 5) facing = 2;
        if (nbt.hasKey("rsAddress")) rsAddress = clampAddr(nbt.getInteger("rsAddress"), 127);
        if (nbt.hasKey("dplAddress")) dplAddress = clampAddr(nbt.getInteger("dplAddress"), 1);
        if (nbt.hasKey("asptAddress")) configuredAsptAddress = clampAddr(nbt.getInteger("asptAddress"), 2);
        if (nbt.hasKey("mains")) mainsEnabled = nbt.getBoolean("mains");
        if (nbt.hasKey("bat7")) hasBattery7 = nbt.getBoolean("bat7");
        if (nbt.hasKey("bat45")) hasBattery45x2 = nbt.getBoolean("bat45");
        if (nbt.hasKey("auto")) autoEnabled = nbt.getBoolean("auto");
        if (nbt.hasKey("remoteStart")) remoteStartEnabled = nbt.getBoolean("remoteStart");
        if (nbt.hasKey("remotePriority")) remotePriority = nbt.getBoolean("remotePriority");
        if (nbt.hasKey("autoRestore")) autoRestore = nbt.getBoolean("autoRestore");
        if (nbt.hasKey("test24")) test24V = nbt.getBoolean("test24");
        if (nbt.hasKey("door")) doorOpen = nbt.getBoolean("door");
        if (nbt.hasKey("sensor")) sensorTriggered = nbt.getBoolean("sensor");
        if (nbt.hasKey("alarm")) alarmLatched = nbt.getBoolean("alarm");
        if (nbt.hasKey("spent")) spent = nbt.getBoolean("spent");
        if (nbt.hasKey("damaged")) damaged = nbt.getBoolean("damaged");
        if (nbt.hasKey("coil")) coilClosed = nbt.getBoolean("coil");
        channels.clear();
        NBTTagList list = nbt.getTagList("channels", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            channels.put(entry.getString("role"), Integer.valueOf(entry.getInteger("channel") & 15));
        }
        ensureChannels();
        NBTTagCompound outputs = nbt.getCompoundTag("outputs");
        for (int i = 0; i < 5; i++) {
            if (outputs.hasKey("oe" + i)) outputEnabled[i] = outputs.getBoolean("oe" + i);
        }
    }

    private static int clampAddr(int value, int fallback) {
        if (value < 1 || value > 127) return fallback;
        return value;
    }

    @Override
    public Packet getDescriptionPacket() {
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, getClientNBT());
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        readClientNBT(pkt.func_148857_g());
    }

    private NBTTagCompound getClientNBT() {
        NBTTagCompound n = new NBTTagCompound();
        n.setString("kind", kind.name());
        n.setInteger("facing", facing);
        n.setString("status", clientStatusText);
        n.setString("journal", clientLogText);
        n.setString("lcd0", clientLcd0);
        n.setString("lcd1", clientLcd1);
        n.setString("mode", clientMode);
        n.setString("fault", clientFault);
        for (int i = 0; i < 4; i++) n.setBoolean("led" + i, clientLeds[i]);
        n.setBoolean("lit", clientLit);
        n.setBoolean("alarm", clientAlarm);
        n.setBoolean("powered", clientPowered);
        n.setBoolean("online", clientOnline);
        n.setBoolean("spent", clientSpent);
        n.setBoolean("damaged", clientDamaged);
        n.setBoolean("coil", clientCoilClosed);
        n.setBoolean("flash", clientFlash);
        n.setBoolean("buzzer", clientBuzzer);
        n.setDouble("voltage", clientVoltage);
        n.setDouble("current", clientCurrent);
        n.setDouble("battery", clientBatterySoc);
        n.setDouble("rsq", clientRsQuality);
        n.setDouble("dplV", clientDplVoltage);
        n.setDouble("dplR", clientDplResistance);
        n.setDouble("delay", clientDelay);
        n.setDouble("start", clientStart);
        n.setInteger("rsAddress", clientRsAddress);
        n.setInteger("dplAddress", clientDplAddress);
        n.setInteger("asptAddress", clientConfiguredAsptAddress);
        n.setInteger("connected", clientConnected);
        n.setInteger("rsqPercent", clientRsQualityPercent);
        return n;
    }

    private void readClientNBT(NBTTagCompound n) {
        if (n.hasKey("kind")) {
            try {
                kind = DeviceKind.valueOf(n.getString("kind"));
            } catch (IllegalArgumentException e) {
                kind = DeviceKind.RIP_12;
            }
        }
        facing = n.getInteger("facing");
        clientFacing = facing;
        clientStatusText = n.getString("status");
        clientLogText = n.getString("journal");
        clientLcd0 = n.getString("lcd0");
        clientLcd1 = n.getString("lcd1");
        clientMode = n.getString("mode");
        clientFault = n.getString("fault");
        for (int i = 0; i < 4; i++) clientLeds[i] = n.getBoolean("led" + i);
        clientLit = n.getBoolean("lit");
        clientAlarm = n.getBoolean("alarm");
        clientPowered = n.getBoolean("powered");
        clientOnline = n.getBoolean("online");
        clientSpent = n.getBoolean("spent");
        clientDamaged = n.getBoolean("damaged");
        clientCoilClosed = n.getBoolean("coil");
        clientFlash = n.getBoolean("flash");
        clientBuzzer = n.getBoolean("buzzer");
        clientVoltage = n.getDouble("voltage");
        clientCurrent = n.getDouble("current");
        clientBatterySoc = n.getDouble("battery");
        clientRsQuality = n.getDouble("rsq");
        clientDplVoltage = n.getDouble("dplV");
        clientDplResistance = n.getDouble("dplR");
        clientDelay = n.getDouble("delay");
        clientStart = n.getDouble("start");
        clientRsAddress = n.getInteger("rsAddress");
        clientDplAddress = n.getInteger("dplAddress");
        clientConfiguredAsptAddress = n.getInteger("asptAddress");
        clientConnected = n.getInteger("connected");
        clientRsQualityPercent = n.getInteger("rsqPercent");
    }

    public SimDevice toSimDevice() {
        String id = xCoord + "," + yCoord + "," + zCoord;
        SimDevice d = SimDevice.createDefault(id, kind, new Pos(xCoord, yCoord, zCoord));
        d.facing = facing;
        d.rsAddress = rsAddress;
        d.dplAddress = dplAddress;
        d.configuredAsptAddress = configuredAsptAddress;
        d.mainsEnabled = mainsEnabled;
        d.hasBattery7 = hasBattery7;
        d.hasBattery45x2 = hasBattery45x2;
        d.autoEnabled = autoEnabled;
        d.remoteStartEnabled = remoteStartEnabled;
        d.remotePriority = remotePriority;
        d.autoRestore = autoRestore;
        d.test24V = test24V;
        d.doorOpen = doorOpen;
        d.sensorTriggered = sensorTriggered;
        d.alarmLatched = alarmLatched;
        d.spent = spent;
        d.damaged = damaged;
        d.coilClosed = coilClosed;
        d.resetRequested = resetRequested;
        d.localStartRequested = localStartRequested;
        d.cancelRequested = cancelRequested;
        d.autoToggleRequested = autoToggleRequested;
        d.testIndicationRequested = testIndicationRequested;
        for (int i = 0; i < 5; i++) d.outputEnabled[i] = outputEnabled[i];
        for (Map.Entry<String, Integer> e : channels.entrySet()) {
            d.setChannel(e.getKey(), e.getValue().intValue());
        }
        if (kind.isRelay()) {
            d.setBridgeClosed("K1", coilClosed);
            d.setBridgeClosed("K2", coilClosed);
        }
        return d;
    }

    public void applyRuntime(DeviceRuntime r) {
        clientStatusText = join(r.statusLines);
        clientLogText = joinLast(r.log, 8);
        clientLcd0 = r.lcd[0];
        clientLcd1 = r.lcd[1];
        for (int i = 0; i < 4; i++) clientLeds[i] = r.led[i];
        clientLit = r.lit;
        clientAlarm = r.alarm;
        clientPowered = r.powered || r.poweredInternal;
        clientOnline = r.online;
        clientSpent = r.spent;
        clientDamaged = r.damaged;
        clientCoilClosed = r.coilClosed;
        clientFlash = r.flash;
        clientBuzzer = r.buzzer;
        clientMode = r.mode;
        clientFault = r.fault;
        clientVoltage = r.voltage;
        clientCurrent = r.currentA;
        clientBatterySoc = r.batterySoc;
        clientRsQuality = r.rsQuality;
        clientDplVoltage = r.dplVoltage;
        clientDplResistance = r.dplResistance;
        clientDelay = r.delayRemaining;
        clientStart = r.startRemaining;
        clientRsAddress = r.rsAddress;
        clientDplAddress = r.dplAddress;
        clientConfiguredAsptAddress = r.configuredAsptAddress;
        clientConnected = r.connectedAddressable;
        clientRsQualityPercent = r.rsQualityPercent;
        clientFacing = facing;

        SimDevice d = r.device;
        if (d != null) {
            spent = d.spent;
            damaged = d.damaged;
            coilClosed = d.coilClosed;
            autoEnabled = d.autoEnabled;
            alarmLatched = d.alarmLatched;
            doorOpen = d.doorOpen;
            sensorTriggered = d.sensorTriggered;
            remoteStartEnabled = d.remoteStartEnabled;
            rsAddress = d.rsAddress;
            dplAddress = d.dplAddress;
            configuredAsptAddress = d.configuredAsptAddress;
            mainsEnabled = d.mainsEnabled;
            hasBattery7 = d.hasBattery7;
            hasBattery45x2 = d.hasBattery45x2;
            test24V = d.test24V;
            remotePriority = d.remotePriority;
            autoRestore = d.autoRestore;
            for (int i = 0; i < 5; i++) outputEnabled[i] = d.outputEnabled[i];
            resetRequested = d.resetRequested;
            localStartRequested = d.localStartRequested;
            cancelRequested = d.cancelRequested;
            autoToggleRequested = d.autoToggleRequested;
            testIndicationRequested = d.testIndicationRequested;
        }
        markDirty();
    }

    private static String join(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (String s : lines) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(s);
        }
        return sb.toString();
    }

    private static String joinLast(List<String> lines, int count) {
        int from = Math.max(0, lines.size() - count);
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < lines.size(); i++) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(lines.get(i));
        }
        return sb.toString();
    }

    public void press() {
        if (!kind.isAddressable()) return;
        alarmLatched = true;
        markForSim();
    }

    public void requestReset() {
        resetRequested = true;
        alarmLatched = false;
        markForSim();
    }

    public void installBattery7() {
        hasBattery7 = true;
        markForSim();
    }

    public void installBattery45() {
        hasBattery45x2 = true;
        markForSim();
    }

    public void toggleMains() {
        mainsEnabled = !mainsEnabled;
        markForSim();
    }

    public void handleAction(int action, int a, int b) {
        switch (action) {
            case ModPackets.ACT_RESET:
                resetRequested = true;
                alarmLatched = false;
                break;
            case ModPackets.ACT_PRESS:
                if (kind.isAddressable()) alarmLatched = true;
                break;
            case ModPackets.ACT_TOGGLE_AUTO:
                autoToggleRequested = true;
                break;
            case ModPackets.ACT_LOCAL_START:
                localStartRequested = true;
                break;
            case ModPackets.ACT_CANCEL:
                cancelRequested = true;
                break;
            case ModPackets.ACT_TOGGLE_REMOTE_START:
                remoteStartEnabled = !remoteStartEnabled;
                break;
            case ModPackets.ACT_SET_RS_ADDRESS:
                rsAddress = clampAddr(a, 127);
                break;
            case ModPackets.ACT_SET_DPL_ADDRESS:
                dplAddress = clampAddr(a, 1);
                break;
            case ModPackets.ACT_SET_CHANNEL: {
                List<String> roles = DevicePorts.roles(kind);
                if (a >= 0 && a < roles.size()) channels.put(roles.get(a), Integer.valueOf(b & 15));
                break;
            }
            case ModPackets.ACT_SET_ASPT_ADDRESS:
                configuredAsptAddress = clampAddr(a, 2);
                break;
            case ModPackets.ACT_TOGGLE_OUTPUT:
                if (a >= 0 && a < 5) outputEnabled[a] = !outputEnabled[a];
                break;
            case ModPackets.ACT_TOGGLE_MAINS:
                mainsEnabled = !mainsEnabled;
                break;
            case ModPackets.ACT_TOGGLE_DOOR:
                doorOpen = !doorOpen;
                break;
            case ModPackets.ACT_TOGGLE_SENSOR:
                sensorTriggered = !sensorTriggered;
                break;
            case ModPackets.ACT_TOGGLE_24V_TEST:
                test24V = !test24V;
                break;
            case ModPackets.ACT_TOGGLE_BATTERY7:
                hasBattery7 = !hasBattery7;
                break;
            case ModPackets.ACT_TOGGLE_BATTERY45:
                hasBattery45x2 = !hasBattery45x2;
                break;
            case ModPackets.ACT_TEST_INDICATION:
                testIndicationRequested = true;
                break;
            case ModPackets.ACT_REPLACE_PYRO:
                spent = false;
                damaged = false;
                break;
            case ModPackets.ACT_RESET_DAMAGE:
                damaged = false;
                break;
            case ModPackets.ACT_TOGGLE_REMOTE_PRIORITY:
                remotePriority = !remotePriority;
                break;
            case ModPackets.ACT_TOGGLE_AUTO_RESTORE:
                autoRestore = !autoRestore;
                break;
            default:
                break;
        }
        markForSim();
    }

    private void markForSim() {
        if (worldObj != null) {
            WiringWorldData.get(worldObj).markTopologyDirty();
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
        markDirty();
    }
}
