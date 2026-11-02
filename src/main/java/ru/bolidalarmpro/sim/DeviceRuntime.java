package ru.bolidalarmpro.sim;

import java.util.ArrayList;
import java.util.List;

/** All live state of one device. This is what the GUI and the renderer consume. */
public final class DeviceRuntime {
    public SimDevice device;
    public final String id;
    public final DeviceKind kind;
    public final Pos pos;

    public boolean powered;
    public boolean poweredInternal;
    public boolean online;
    public boolean ready;
    public boolean alarm;
    public boolean lit;
    public boolean damaged;
    public boolean spent;
    public boolean coilClosed;
    public boolean mainsPresent;
    public boolean hasBattery;
    public boolean batteryPresentLast;
    public boolean buzzer;
    public boolean flash;
    public boolean doorOpen;
    public boolean sensorTriggered;
    public boolean autoEnabled;
    public boolean remoteStartEnabled;
    public boolean remotePriority;
    public boolean autoRestore;
    public boolean outputShutdown;
    public boolean startOutput;

    public double voltage;
    public double currentA;
    public double loopVoltage;
    public double batterySoc;
    public double batteryVoltage;
    public double batteryChargeAh;
    public double batteryCapacityAh;
    public double outputVoltage;
    public double dplResistance;
    public double dplVoltage;
    public double rsQuality;
    public double quality;
    public double pulseTime;
    public double overloadTimer;
    public double startupRemaining;
    public double delayRemaining;
    public double startRemaining;
    public double tushRemaining;
    public double sirenRemaining;
    public double testIndicationRemaining;

    public boolean eventPending;
    public boolean eventDelivered;
    public double eventReadyAt;
    public String eventText = "";
    public boolean remoteStartRequested;
    public boolean localStartRequested;
    public boolean cancelRequested;
    public boolean resetRequested;
    public boolean autoToggleRequested;
    public boolean testIndicationRequested;

    public int rsAddress;
    public int dplAddress;
    public int configuredAsptAddress;
    public int rsQualityPercent;
    public int connectedAddressable;
    public int duplicateAddress;
    public int dplDeviceCount;

    public String mode = "ДЕЖУРНЫЙ";
    public String fault = "";
    public String display1 = "";
    public String display2 = "";

    public final String[] lcd = new String[] {"", ""};
    public final boolean[] outputs = new boolean[5];
    public final boolean[] outputEnabled = new boolean[5];
    public final boolean[] outputFault = new boolean[5];
    public final boolean[] loopAlarm = new boolean[3];
    public final boolean[] led = new boolean[4]; // power, alarm, fault, comm

    public final List<String> log = new ArrayList<String>();
    public final List<String> statusLines = new ArrayList<String>();
    public final List<String> connectedDevices = new ArrayList<String>();

    public DeviceRuntime(SimDevice d) {
        this.device = d;
        this.id = d.id;
        this.kind = d.kind;
        this.pos = d.pos;
        this.startupRemaining = d.kind.startupSeconds();
        this.batteryCapacityAh = d.kind == DeviceKind.ASPT_353 ? 9.0 : 7.0;
        syncConfigFromDevice(d);
        lcd[0] = d.kind.displayName();
        lcd[1] = "ПО v" + d.kind.firmware();
    }

    public void syncConfigFromDevice(SimDevice d) {
        this.device = d;
        this.rsAddress = d.rsAddress;
        this.dplAddress = d.dplAddress;
        this.configuredAsptAddress = d.configuredAsptAddress;
        this.autoEnabled = d.autoEnabled;
        this.remoteStartEnabled = d.remoteStartEnabled;
        this.remotePriority = d.remotePriority;
        this.autoRestore = d.autoRestore;
        this.mainsPresent = d.mainsEnabled;
        this.hasBattery = d.hasBattery7 || d.hasBattery45x2;
        this.doorOpen = d.doorOpen;
        this.sensorTriggered = d.sensorTriggered;
        this.damaged = d.damaged;
        this.spent = d.spent;
        this.coilClosed = d.coilClosed;
        for (int i = 0; i < 5; i++) outputEnabled[i] = d.outputEnabled[i];
    }

    public void addLog(String line) {
        log.add(line);
        while (log.size() > 50) log.remove(0);
    }

    public void setLcd(String a, String b) {
        lcd[0] = a == null ? "" : a;
        lcd[1] = b == null ? "" : b;
        display1 = lcd[0];
        display2 = lcd[1];
    }

    public void setStatus(String[] lines) {
        statusLines.clear();
        for (String s : lines) statusLines.add(s);
    }

    public List<String> getStatusLines() {
        return new ArrayList<String>(statusLines);
    }

    public boolean isFlashing() {
        return flash;
    }

    @Override
    public String toString() {
        return kind.displayName() + " [" + mode + "] U=" + voltage + " B";
    }
}
