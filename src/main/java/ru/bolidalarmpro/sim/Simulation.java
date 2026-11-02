package ru.bolidalarmpro.sim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Deterministic electrical/fire-alarm simulator. It knows nothing about Minecraft:
 * the Forge layer supplies cable blocks, device flags and consumes SimEvents.
 */
public final class Simulation {
    public static final double RIP_NOMINAL_A = 2.0;
    public static final double RIP_MAX_A = 3.0;
    public static final double RIP_OVERLOAD_TRIP_S = 600.0;
    public static final double DPL_MAX_R = 200.0;
    public static final double DPL_MIN_V = 8.0;
    public static final double DPL_MAX_V = 11.0;
    public static final double TABLO_MIN_V = 9.0;
    public static final double TABLO_MAX_V = 13.8;
    public static final double ASPT_AUTO_DELAY_S = 30.0;
    public static final double ASPT_REMOTE_DELAY_S = 15.0;
    public static final double ASPT_START_S = 15.0;
    public static final double ASPT_TUSH_S = 120.0;
    public static final double PYRO_TRIGGER_A = 0.5;
    public static final double PYRO_TRIGGER_S = 0.2;
    public static final double EVENT_LATENCY_S = 0.3;

    public static final String MODE_IDLE = "ДЕЖУРНЫЙ";
    public static final String MODE_DELAY = "ЗАДЕРЖКА ПУСКА";
    public static final String MODE_START = "ЗАПУСК АУП";
    public static final String MODE_TUSH = "ТУШЕНИЕ";
    public static final String MODE_WAIT_RESET = "ОЖИДАНИЕ СБРОСА";
    public static final String MODE_POWER_FAULT = "АВАРИЯ ПИТАНИЯ";

    private static final String[] CONTROL_ROLES = new String[] {"OUT_SO1", "OUT_SO2", "OUT_ZO", "OUT_AUX"};
    private static final String[] LOOP_ROLES = new String[] {"LOOP1", "LOOP2", "LOOP3"};

    private NetworkGraph graph;
    private List<SimDevice> devices = new ArrayList<SimDevice>();
    private Map<String, DeviceRuntime> runtimes = new HashMap<String, DeviceRuntime>();
    private final EventLog log = new EventLog();
    private final List<SimEvent> pending = new ArrayList<SimEvent>();
    private final Map<String, String> lastFaults = new HashMap<String, String>();
    private double time;
    private long tick;
    private double timeScale = 1.0;

    public Simulation(List<WireNode> wires, List<SimDevice> deviceList) {
        setTopology(NetworkGraph.build(wires, deviceList), deviceList);
    }

    public void setTopology(NetworkGraph newGraph, List<SimDevice> newDevices) {
        this.graph = newGraph;
        this.devices = new ArrayList<SimDevice>(newDevices);
        Map<String, DeviceRuntime> old = this.runtimes;
        this.runtimes = new HashMap<String, DeviceRuntime>();
        for (SimDevice d : newDevices) {
            DeviceRuntime r = old.get(d.id);
            if (r == null) r = new DeviceRuntime(d);
            else r.syncConfigFromDevice(d);
            runtimes.put(d.id, r);
        }
    }

    public void tick(double dt) {
        dt *= timeScale;
        if (dt <= 0.0) return;
        time += dt;
        tick++;

        syncInputFlags();
        updateAsptPower(dt);
        computeControl24();
        graph.applyBridges();
        computePower12(dt);
        updateAsptStateMachine(dt);
        computeControl24();
        graph.applyBridges();
        computePower12(dt);
        computeRs485();
        computeDpl(dt);
        processDplEvents();
        computeStart24(dt);
        updateTablo();
        updateIndication();
        consumeOneShotFlags();
    }

    private DeviceRuntime rt(SimDevice d) {
        return runtimes.get(d.id);
    }

    private DeviceRuntime rt(String id) {
        return runtimes.get(id);
    }

    private void syncInputFlags() {
        for (SimDevice d : devices) {
            DeviceRuntime r = rt(d);
            if (r == null) continue;
            r.syncConfigFromDevice(d);
            if (d.alarmLatched) r.alarm = true;
            if (d.resetRequested) r.resetRequested = true;
            if (d.localStartRequested) r.localStartRequested = true;
            if (d.cancelRequested) r.cancelRequested = true;
            if (d.autoToggleRequested) r.autoToggleRequested = true;
            if (d.testIndicationRequested) r.testIndicationRequested = true;
        }
    }

    private void consumeOneShotFlags() {
        for (SimDevice d : devices) {
            d.resetRequested = false;
            d.localStartRequested = false;
            d.cancelRequested = false;
            d.autoToggleRequested = false;
            d.testIndicationRequested = false;
            DeviceRuntime r = rt(d);
            if (r == null) continue;
            r.resetRequested = false;
            r.localStartRequested = false;
            r.cancelRequested = false;
            r.autoToggleRequested = false;
            r.testIndicationRequested = false;
        }
    }

    private boolean mainsAvailable(SimDevice d) {
        for (SimDevice m : devices) {
            if (!m.kind.isMains() || !m.mainsEnabled) continue;
            if (m.pos.manhattan(d.pos) <= 2) return true;
        }
        return false;
    }

    private void updateAsptPower(double dt) {
        for (SimDevice d : devices) {
            if (!d.kind.isAspt()) continue;
            DeviceRuntime r = rt(d);
            double load = 0.060;
            for (int i = 0; i < 5; i++) {
                if (r.outputEnabled[i] && r.outputs[i]) load += (i == 4 ? 0.5 : 0.05);
            }
            BatteryModel.updateAspt(r, d, mainsAvailable(d), load, dt);
            r.voltage = r.outputVoltage;
            r.currentA = load;
            if (!r.poweredInternal) {
                r.mode = MODE_POWER_FAULT;
                for (int i = 0; i < 5; i++) r.outputs[i] = false;
                r.startOutput = false;
                r.flash = false;
                r.buzzer = false;
            }
            String fault = r.poweredInternal ? "" : "АВАРИЯ ПИТАНИЯ АСПТ (СЕТЬ / 2x4,5 А·ч)";
            r.fault = fault;
            logFault(d.id + "|PWR", fault);
        }
    }

    private static final class Driver {
        final SimDevice device;
        final int output;
        Driver(SimDevice device, int output) {
            this.device = device;
            this.output = output;
        }
    }

    private Driver findControlDriver(Net net) {
        Driver found = null;
        int count = 0;
        for (SimDevice a : devices) {
            if (!a.kind.isAspt()) continue;
            DeviceRuntime r = rt(a);
            if (!r.poweredInternal) continue;
            for (int i = 0; i < CONTROL_ROLES.length; i++) {
                if (!r.outputEnabled[i] || !r.outputs[i]) continue;
                if (isAlertMode(r.mode) && !r.flash) continue;
                Net candidate = graph.getNetForPort(a, CONTROL_ROLES[i]);
                if (candidate != net) continue;
                count++;
                if (found == null) found = new Driver(a, i);
            }
        }
        if (count > 1) logFault(net.toString(), "КОНФЛИКТ ВЫХОДОВ 24 В: НЕСКОЛЬКО ИСТОЧНИКОВ");
        return found;
    }

    private Driver findStartDriver(Net net) {
        for (SimDevice a : devices) {
            if (!a.kind.isAspt()) continue;
            DeviceRuntime r = rt(a);
            if (!r.poweredInternal || !r.outputEnabled[4] || !r.startOutput) continue;
            if (graph.getNetForPort(a, "OUT_START") == net) return new Driver(a, 4);
        }
        return null;
    }

    private void computeControl24() {
        for (Net net : graph.getNetsOfKind(WireKind.CONTROL24)) {
            LineSupervision.LineStatus st = LineSupervision.classify(net, WireKind.CONTROL24);
            Driver driver = findControlDriver(net);
            double coilLoad = 0.05 * net.devicesByKind(DeviceKind.RELAY_12).size();
            net.voltage = 0.0;
            net.currentA = st.supervisionCurrentA;
            net.fault = st.fault;
            if (driver != null && st.healthy) {
                DeviceRuntime ar = rt(driver.device);
                net.voltage = ar.outputVoltage;
                net.currentA = coilLoad + st.supervisionCurrentA;
                net.fault = "";
                ar.outputFault[driver.output] = false;
            } else if (driver != null) {
                rt(driver.device).outputFault[driver.output] = true;
            }
            logFault(net.toString() + "|C24", net.fault);
            boolean closed = net.voltage >= 20.4 && net.voltage <= 27.6 && st.healthy && !st.shortCircuit;
            for (SimDevice relay : net.devicesByKind(DeviceKind.RELAY_12)) {
                relay.setBridgeClosed("K1", closed);
                relay.setBridgeClosed("K2", closed);
                relay.coilClosed = closed;
                DeviceRuntime rr = rt(relay);
                if (rr != null) {
                    rr.coilClosed = closed;
                    rr.voltage = net.voltage;
                    rr.currentA = closed ? 0.05 : 0.0;
                }
            }
        }
    }

    private double loadCurrentForDevice(SimDevice d, DeviceRuntime r) {
        if (d.kind == DeviceKind.RIP_12 || d.kind.isRelay()) return 0.0;
        if (d.kind.isPanel()) return 0.120;
        if (d.kind.isKdl()) return 0.080 + 0.0005 * r.connectedAddressable;
        if (d.kind.isTablo()) return r.damaged ? 0.0 : 0.040;
        return 0.0;
    }

    private double sumPower12Load(Net net) {
        double sum = 0.0;
        for (SimDevice d : net.devices()) {
            if (d.kind == DeviceKind.RIP_12) continue;
            sum += loadCurrentForDevice(d, rt(d));
        }
        return sum;
    }

    private void computePower12(double dt) {
        for (Net net : graph.getNetsOfKind(WireKind.POWER12)) {
            List<SimDevice> sources = net.devicesByKind(DeviceKind.RIP_12);
            if (sources.isEmpty()) {
                net.voltage = 0.0;
                net.currentA = 0.0;
                net.fault = "НЕТ ИСТОЧНИКА 12 В";
                for (SimDevice d : net.devices()) {
                    DeviceRuntime r = rt(d);
                    r.voltage = 0.0;
                    r.powered = false;
                }
                logFault(net.toString() + "|P12", net.fault);
                continue;
            }
            if (sources.size() > 1) logFault(net.toString() + "|SRC", "НЕСКОЛЬКО РИП-12 В ОДНОЙ СЕТИ 12 В");
            SimDevice rip = sources.get(0);
            DeviceRuntime rr = rt(rip);
            double load = sumPower12Load(net);
            boolean mains = mainsAvailable(rip);
            BatteryModel.updateRip12(rr, rip, mains, load, dt, rip.test24V);

            LineSupervision.LineStatus st = LineSupervision.classify(net, WireKind.POWER12);
            String fault = st.fault;
            if (load > RIP_MAX_A) {
                rr.overloadTimer += dt;
                if (fault.length() == 0) fault = "ПЕРЕГРУЗКА > 3 А";
                if (rr.overloadTimer > RIP_OVERLOAD_TRIP_S) rr.outputShutdown = true;
            } else if (load > RIP_NOMINAL_A) {
                rr.overloadTimer = Math.max(0.0, rr.overloadTimer - dt);
                if (fault.length() == 0) fault = "ПЕРЕГРУЗКА > 2 А";
            } else {
                rr.overloadTimer = Math.max(0.0, rr.overloadTimer - dt * 2.0);
            }
            if (!st.shortCircuit && st.polarityOk && load <= RIP_NOMINAL_A && rr.overloadTimer <= 0.0) {
                rr.outputShutdown = false;
            }
            boolean hardFault = st.shortCircuit || !st.polarityOk || rr.outputShutdown;
            if (hardFault) {
                net.voltage = 0.0;
                rr.outputShutdown = true;
                if (fault.length() == 0) fault = "ВЫХОД РИП-12 ОТКЛЮЧЁН";
            } else {
                net.voltage = Math.max(0.0, rr.outputVoltage - load * net.resistanceOhms);
                if (net.voltage > 0.0 && net.voltage < 10.2 && fault.length() == 0) {
                    fault = "ПОНИЖЕННОЕ НАПРЯЖЕНИЕ";
                }
            }
            net.currentA = load;
            net.fault = fault;
            rr.fault = fault;
            rr.powered = !rr.outputShutdown && (mains || rr.hasBattery) && rr.outputVoltage > 0.0;
            rr.voltage = net.voltage;
            rr.currentA = load;
            logFault(net.toString() + "|P12", fault);

            for (SimDevice d : net.devices()) {
                if (d.kind == DeviceKind.RIP_12 || d.kind.isRelay()) continue;
                DeviceRuntime r = rt(d);
                r.voltage = net.voltage;
                r.currentA = loadCurrentForDevice(d, r);
                boolean inRange = net.voltage >= d.kind.minVoltage() && net.voltage <= d.kind.maxVoltage();
                if (d.kind.isTablo() && net.voltage > TABLO_MAX_V) {
                    r.damaged = true;
                    d.damaged = true;
                    fault = "ПЕРЕНАПРЯЖЕНИЕ ТАБЛО > 13,8 В";
                    logFault(d.id + "|OV", fault);
                }
                if (d.kind.isPanel() || d.kind.isKdl() || d.kind.isTablo()) {
                    r.powered = inRange && !r.damaged && !hardFault;
                }
                if (!inRange && r.fault.length() == 0) r.fault = "U ПИТАНИЯ ВНЕ ДОПУСКА";
            }
        }
    }

    private void computeRs485() {
        for (SimDevice d : devices) {
            if (!d.kind.isRs485()) continue;
            DeviceRuntime r = rt(d);
            r.online = false;
            r.rsQuality = 0.0;
            r.rsQualityPercent = 0;
            r.fault = "";
            r.connectedDevices.clear();
        }
        for (Net net : graph.getNetsOfKind(WireKind.RS485)) {
            LineSupervision.LineStatus st = LineSupervision.classify(net, WireKind.RS485);
            Map<Integer, List<SimDevice>> byAddress = new HashMap<Integer, List<SimDevice>>();
            for (SimDevice d : net.devices()) {
                if (!d.kind.isRs485()) continue;
                Integer addr = Integer.valueOf(d.rsAddress);
                List<SimDevice> list = byAddress.get(addr);
                if (list == null) {
                    list = new ArrayList<SimDevice>();
                    byAddress.put(addr, list);
                }
                list.add(d);
            }
            Set<String> duplicates = new HashSet<String>();
            String duplicateText = "";
            for (Map.Entry<Integer, List<SimDevice>> e : byAddress.entrySet()) {
                if (e.getValue().size() > 1) {
                    duplicateText = "ДУБЛЬ АДРЕСА RS-485 " + e.getKey().intValue();
                    for (SimDevice d : e.getValue()) duplicates.add(d.id);
                }
            }
            double q = 1.0;
            if (net.terminatorCount == 0) q = 0.35;
            else if (net.terminatorCount == 1) q = 0.75;
            else if (net.terminatorCount > 2) q = 0.60;
            if (net.swapCount > 0 || !st.polarityOk) q *= 0.20;
            if (!st.resistanceOk) q *= 0.40;
            if (st.shortCircuit) q = 0.0;
            if (duplicates.size() > 0) q = 0.0;
            net.quality = Math.max(0.0, Math.min(1.0, q));
            boolean loss = net.quality < 0.99 && (tick % 20L) < (long) ((1.0 - net.quality) * 20.0);
            net.commOk = net.quality > 0.30 && !loss;
            net.fault = st.fault.length() > 0 ? st.fault : duplicateText;
            if (net.fault.length() == 0 && st.warning.length() > 0) net.fault = st.warning;
            logFault(net.toString() + "|RS", net.fault);

            List<SimDevice> panels = new ArrayList<SimDevice>();
            panels.addAll(net.devicesByKind(DeviceKind.S2000M_205));
            panels.addAll(net.devicesByKind(DeviceKind.S2000M_502_02));
            boolean anyPanelOnline = false;
            for (SimDevice p : panels) {
                DeviceRuntime pr = rt(p);
                boolean ok = pr.powered && net.commOk && !duplicates.contains(p.id);
                pr.online = pr.online || ok;
                pr.rsQuality = Math.max(pr.rsQuality, net.quality);
                pr.rsQualityPercent = Math.max(pr.rsQualityPercent, (int) Math.round(net.quality * 100.0));
                if (ok) anyPanelOnline = true;
                if (net.fault.length() > 0) pr.fault = net.fault;
                for (SimDevice d : net.devices()) {
                    if (!d.kind.isRs485() || d.id.equals(p.id)) continue;
                    DeviceRuntime dr = rt(d);
                    pr.connectedDevices.add(d.rsAddress + " " + d.kind.displayName() + " " + (dr.online ? "ОНЛАЙН" : "ОФЛАЙН"));
                }
            }
            for (SimDevice d : net.devices()) {
                if (!d.kind.isRs485()) continue;
                DeviceRuntime dr = rt(d);
                boolean ok = (dr.powered || dr.poweredInternal) && net.commOk && anyPanelOnline && !duplicates.contains(d.id);
                dr.online = dr.online || ok;
                dr.rsQuality = Math.max(dr.rsQuality, net.quality);
                dr.rsQualityPercent = Math.max(dr.rsQualityPercent, (int) Math.round(net.quality * 100.0));
                if (!ok && dr.fault.length() == 0 && net.fault.length() > 0) dr.fault = net.fault;
            }
        }
    }

    private void computeDpl(double dt) {
        for (SimDevice kdl : devices) {
            if (!kdl.kind.isKdl()) continue;
            DeviceRuntime kr = rt(kdl);
            if (kr.powered) kr.startupRemaining = Math.max(0.0, kr.startupRemaining - dt);
            else kr.startupRemaining = kdl.kind.startupSeconds();
            kr.ready = kr.powered && kr.startupRemaining <= 0.0;
            kr.connectedAddressable = 0;
            kr.dplDeviceCount = 0;
            kr.dplResistance = 0.0;
            kr.fault = "";
            double loopVoltage = 0.0;
            if (kr.powered && kr.voltage > 0.0) {
                loopVoltage = Math.min(DPL_MAX_V, Math.max(DPL_MIN_V, kr.voltage - 3.0));
            }
            kr.dplVoltage = loopVoltage;
            List<Net> dplNets = graph.getPortNets(kdl, WireKind.DPL);
            String kdlFault = "";
            if (dplNets.isEmpty()) kdlFault = "ДПЛС НЕ ПОДКЛЮЧЕНА";
            if (dplNets.size() > 1) kdlFault = "ОБРЫВ КОЛЬЦА: ОПРОС С ДВУХ СТОРОН";
            for (Net net : dplNets) {
                LineSupervision.LineStatus st = LineSupervision.classify(net, WireKind.DPL);
                net.fault = st.fault;
                net.commOk = true;
                net.ringIntact = net.hasCycle() && dplNets.size() == 1 && !st.shortCircuit;
                if (st.shortCircuit) {
                    List<SimDevice> isolators = net.devicesByKind(DeviceKind.UDP_513_3AM);
                    if (isolators.isEmpty()) {
                        net.commOk = false;
                        net.fault = "КЗ ДПЛС: НЕТ БРИЗ";
                    } else {
                        net.fault = "КЗ ДПЛС ЛОКАЛИЗОВАНО БРИЗ УДП 513-3АМ";
                    }
                }
                if (!st.resistanceOk || net.resistanceOhms > DPL_MAX_R) {
                    net.commOk = false;
                    net.fault = "СОПРОТИВЛЕНИЕ ДПЛС ВНЕ НОРМЫ";
                }
                if (dplNets.size() == 1 && !net.hasCycle() && net.fault.length() == 0) {
                    net.fault = "КОЛЬЦО ДПЛС НЕ ЗАМКНУТО";
                }
                Map<Integer, List<SimDevice>> byAddress = new HashMap<Integer, List<SimDevice>>();
                for (SimDevice d : net.devices()) {
                    if (!d.kind.isAddressable()) continue;
                    Integer addr = Integer.valueOf(d.dplAddress);
                    List<SimDevice> list = byAddress.get(addr);
                    if (list == null) {
                        list = new ArrayList<SimDevice>();
                        byAddress.put(addr, list);
                    }
                    list.add(d);
                }
                Set<String> duplicates = new HashSet<String>();
                for (Map.Entry<Integer, List<SimDevice>> e : byAddress.entrySet()) {
                    if (e.getValue().size() > 1) {
                        net.fault = "ДУБЛЬ АДРЕСА ДПЛС " + e.getKey().intValue();
                        for (SimDevice d : e.getValue()) duplicates.add(d.id);
                    }
                }
                if (net.fault.length() > 0 && kdlFault.length() == 0) kdlFault = net.fault;
                kr.dplResistance = Math.max(kr.dplResistance, net.resistanceOhms);
                logFault(kdl.id + "|" + net.toString(), net.fault);

                for (SimDevice dev : net.devices()) {
                    if (!dev.kind.isAddressable()) continue;
                    kr.dplDeviceCount++;
                    DeviceRuntime dr = rt(dev);
                    dr.fault = "";
                    dr.duplicateAddress = duplicates.contains(dev.id) ? dev.dplAddress : 0;
                    double localV = loopVoltage - dev.kind.standbyCurrentA() * net.resistanceOhms;
                    dr.loopVoltage = Math.max(0.0, localV);
                    dr.powered = kr.powered && loopVoltage > 0.0 && net.commOk && localV >= DPL_MIN_V && !duplicates.contains(dev.id);
                    if (dr.powered) dr.startupRemaining = Math.max(0.0, dr.startupRemaining - dt);
                    else dr.startupRemaining = dev.kind.startupSeconds();
                    dr.ready = dr.powered && dr.startupRemaining <= 0.0;
                    dr.online = dr.ready;
                    dr.rsQualityPercent = kr.rsQualityPercent;
                    if (dr.online) kr.connectedAddressable++;
                    if (dev.alarmLatched) {
                        dr.alarm = true;
                        if (!dr.eventPending && !dr.eventDelivered) {
                            dr.eventPending = true;
                            dr.eventReadyAt = time + EVENT_LATENCY_S;
                            dr.eventText = dev.kind == DeviceKind.UDP_513_3AM ? "ПУСК АУП" : "ПОЖАР";
                        }
                    }
                    if (dev.resetRequested) {
                        dr.alarm = false;
                        dr.eventPending = false;
                        dr.eventDelivered = false;
                        dr.eventText = "";
                        dev.alarmLatched = false;
                        dr.addLog("СБРОС КЛЮЧОМ");
                    }
                    if (!dr.online && dr.fault.length() == 0 && !kr.ready) dr.fault = "ТЕХНИЧЕСКАЯ ГОТОВНОСТЬ 15 с";
                }
            }
            kr.fault = kdlFault;
            logFault(kdl.id + "|DPL", kdlFault);
        }
    }

    private void processDplEvents() {
        for (SimDevice dev : devices) {
            if (!dev.kind.isAddressable()) continue;
            DeviceRuntime dr = rt(dev);
            if (!dr.eventPending || time < dr.eventReadyAt || !dr.online) continue;
            SimDevice kdl = null;
            for (SimDevice k : devices) {
                if (!k.kind.isKdl()) continue;
                for (Net n : graph.getPortNets(k, WireKind.DPL)) {
                    if (n.contains(dev)) kdl = k;
                }
            }
            if (kdl == null) continue;
            boolean delivered = false;
            for (Net rs : graph.getPortNets(kdl, WireKind.RS485)) {
                if (!rs.commOk) continue;
                List<SimDevice> panels = new ArrayList<SimDevice>();
                panels.addAll(rs.devicesByKind(DeviceKind.S2000M_205));
                panels.addAll(rs.devicesByKind(DeviceKind.S2000M_502_02));
                for (SimDevice panel : panels) {
                    DeviceRuntime pr = rt(panel);
                    if (!pr.online || !pr.powered) continue;
                    String text = dr.eventText + " " + kdl.rsAddress + "/" + dev.dplAddress;
                    pr.addLog(text);
                    pr.setLcd(crop16(text), crop16(dev.kind.displayName()));
                    pr.alarm = true;
                    pr.buzzer = true;
                    delivered = true;
                    log.add(time, SimEvent.ALARM, panel.kind.displayName(), text);
                    emit(SimEvent.INFO, SimEvent.ALARM, dev.pos, dev.id, text);
                    if (dev.kind == DeviceKind.UDP_513_3AM && panel.kind.supportsRemoteStart() && pr.remoteStartEnabled) {
                        boolean found = false;
                        for (SimDevice aspt : rs.devicesByKind(DeviceKind.ASPT_353)) {
                            if (aspt.rsAddress != pr.configuredAsptAddress) continue;
                            DeviceRuntime ar = rt(aspt);
                            if (!ar.poweredInternal || !ar.online) continue;
                            ar.remoteStartRequested = true;
                            ar.addLog("КОМАНДА «ЗАПУСК АУП» ОТ " + panel.kind.displayName());
                            found = true;
                        }
                        if (!found) {
                            String t = "АСПТ С АДРЕСОМ " + pr.configuredAsptAddress + " НЕ НАЙДЕН";
                            pr.addLog(t);
                            log.add(time, SimEvent.WARNING, panel.kind.displayName(), t);
                        }
                    }
                }
            }
            if (delivered) {
                dr.eventPending = false;
                dr.eventDelivered = true;
            } else {
                dr.fault = "СОБЫТИЕ НЕ ПЕРЕДАНО: НЕТ RS-485 / ПУЛЬТА";
            }
        }
    }

    private static final class ZoneStatus {
        int sensorCount;
        boolean loopFault;
        boolean doorOpen;
        final boolean[] loopAlarm = new boolean[3];
    }

    private ZoneStatus readZones(SimDevice aspt) {
        ZoneStatus z = new ZoneStatus();
        DeviceRuntime ar = rt(aspt);
        z.doorOpen = ar.doorOpen;
        for (int i = 0; i < LOOP_ROLES.length; i++) {
            Net net = graph.getNetForPort(aspt, LOOP_ROLES[i]);
            LineSupervision.LineStatus st = LineSupervision.classify(net, WireKind.FIRE_ZONE);
            if (!st.healthy) {
                z.loopFault = true;
                continue;
            }
            if (net == null) continue;
            for (SimDevice sensor : net.devicesByKind(DeviceKind.LOOP_SENSOR)) {
                DeviceRuntime sr = rt(sensor);
                if (sr.sensorTriggered || sensor.sensorTriggered) {
                    z.sensorCount++;
                    z.loopAlarm[i] = true;
                }
            }
        }
        Net doorNet = graph.getNetForPort(aspt, "DOOR");
        LineSupervision.LineStatus ds = LineSupervision.classify(doorNet, WireKind.FIRE_ZONE);
        if (!ds.healthy) z.loopFault = true;
        else if (doorNet != null) {
            for (SimDevice door : doorNet.devicesByKind(DeviceKind.DOOR_CONTACT)) {
                if (rt(door).doorOpen || door.doorOpen) z.doorOpen = true;
            }
        }
        return z;
    }

    private void updateOutputSupervision(SimDevice aspt) {
        DeviceRuntime r = rt(aspt);
        String fault = "";
        for (int i = 0; i < CONTROL_ROLES.length; i++) {
            if (!r.outputEnabled[i]) {
                r.outputFault[i] = false;
                logFault(aspt.id + "|OUT" + i, "");
                continue;
            }
            Net net = graph.getNetForPort(aspt, CONTROL_ROLES[i]);
            LineSupervision.LineStatus st = LineSupervision.classify(net, WireKind.CONTROL24);
            r.outputFault[i] = !st.healthy;
            if (!st.healthy) fault = "НЕИСПРАВНОСТЬ ВЫХОДА СО" + (i + 1);
            logFault(aspt.id + "|OUT" + i, st.fault);
        }
        if (r.outputEnabled[4]) {
            Net net = graph.getNetForPort(aspt, "OUT_START");
            LineSupervision.LineStatus st = LineSupervision.classify(net, WireKind.START24);
            r.outputFault[4] = !st.healthy;
            if (!st.healthy) fault = "НЕИСПРАВНОСТЬ ЦЕПИ ЗАПУСКА";
            logFault(aspt.id + "|OUT4", st.fault);
        } else {
            r.outputFault[4] = false;
            logFault(aspt.id + "|OUT4", "");
        }
        if (fault.length() > 0) r.fault = fault;
    }

    private boolean isAlertMode(String mode) {
        return MODE_DELAY.equals(mode) || MODE_START.equals(mode) || MODE_TUSH.equals(mode) || MODE_WAIT_RESET.equals(mode);
    }

    private void enterDelay(DeviceRuntime r, SimDevice d, double seconds, String reason) {
        r.mode = MODE_DELAY;
        r.delayRemaining = seconds;
        r.alarm = true;
        r.buzzer = true;
        r.addLog("ЗАДЕРЖКА ПУСКА " + (int) seconds + " с: " + reason);
        log.add(time, SimEvent.ALARM, d.kind.displayName(), "ЗАДЕРЖКА ПУСКА " + (int) seconds + " с");
        emit(SimEvent.INFO, SimEvent.ALARM, d.pos, d.id, "ЗАДЕРЖКА ПУСКА");
    }

    private void setAlertOutputs(DeviceRuntime r, boolean start) {
        r.outputs[0] = r.outputEnabled[0];
        r.outputs[1] = r.outputEnabled[1];
        r.outputs[2] = r.outputEnabled[2];
        r.outputs[3] = r.outputEnabled[3];
        r.outputs[4] = start && r.outputEnabled[4];
        r.startOutput = r.outputs[4];
        r.flash = (time % 1.0) < 0.5;
        r.buzzer = MODE_DELAY.equals(r.mode) && r.flash;
    }

    private void clearOutputs(DeviceRuntime r) {
        for (int i = 0; i < 5; i++) r.outputs[i] = false;
        r.startOutput = false;
        r.flash = false;
        r.buzzer = false;
    }

    private void updateAsptStateMachine(double dt) {
        for (SimDevice d : devices) {
            if (!d.kind.isAspt()) continue;
            DeviceRuntime r = rt(d);
            if (r.autoToggleRequested) {
                r.autoEnabled = !r.autoEnabled;
                d.autoEnabled = r.autoEnabled;
                r.addLog(r.autoEnabled ? "АВТОМАТИКА ВКЛЮЧЕНА" : "АВТОМАТИКА ОТКЛЮЧЕНА");
                log.add(time, SimEvent.INFO, d.kind.displayName(), r.autoEnabled ? "АВТОМАТИКА ВКЛ." : "АВТОМАТИКА ВЫКЛ.");
            }
            if (r.testIndicationRequested) {
                r.testIndicationRemaining = 5.0;
                r.addLog("ТЕСТ ИНДИКАЦИИ 5 с");
            }
            if (r.testIndicationRemaining > 0.0) r.testIndicationRemaining = Math.max(0.0, r.testIndicationRemaining - dt);
            ZoneStatus z = readZones(d);
            for (int i = 0; i < 3; i++) r.loopAlarm[i] = z.loopAlarm[i];
            r.doorOpen = z.doorOpen;
            d.doorOpen = z.doorOpen;
            updateOutputSupervision(d);
            if (z.loopFault && r.fault.length() == 0) r.fault = "НЕИСПРАВНОСТЬ ШЛЕЙФОВ / ДВЕРИ";
            logFault(d.id + "|ASPT", r.fault);

            if (!r.poweredInternal) {
                r.mode = MODE_POWER_FAULT;
                clearOutputs(r);
                continue;
            }
            if (MODE_POWER_FAULT.equals(r.mode)) r.mode = MODE_IDLE;

            boolean autoStart = r.autoEnabled && !z.doorOpen && z.sensorCount >= 2 && !z.loopFault;
            boolean localStart = r.localStartRequested;
            boolean remoteStart = r.remoteStartRequested;
            boolean cancel = r.cancelRequested;
            boolean reset = r.resetRequested;

            if (MODE_IDLE.equals(r.mode)) {
                clearOutputs(r);
                r.alarm = false;
                if (reset) r.addLog("СБРОС ТРЕВОГ");
                if (autoStart || localStart || remoteStart) {
                    double delay = autoStart ? ASPT_AUTO_DELAY_S : ASPT_REMOTE_DELAY_S;
                    String reason = autoStart ? "2 ИЗВЕЩАТЕЛЯ" : (remoteStart ? "КОМАНДА С2000М" : "РУЧНОЙ ПУСК");
                    enterDelay(r, d, delay, reason);
                }
            } else if (MODE_DELAY.equals(r.mode)) {
                boolean doorCancel = z.doorOpen && autoStart && !r.remotePriority;
                if (cancel || doorCancel) {
                    r.mode = MODE_IDLE;
                    r.alarm = false;
                    clearOutputs(r);
                    r.addLog(cancel ? "ПУСК ОТМЕНЁН" : "ПУСК ОТМЕНЁН: ДВЕРЬ ОТКРЫТА");
                    log.add(time, SimEvent.WARNING, d.kind.displayName(), "ПУСК ОТМЕНЁН");
                } else {
                    r.delayRemaining -= dt;
                    setAlertOutputs(r, false);
                    if (r.delayRemaining <= 0.0) {
                        r.mode = MODE_START;
                        r.startRemaining = ASPT_START_S;
                        r.addLog("ЗАПУСК АУП: ПУСКОВОЙ ИМПУЛЬС 15 с");
                        log.add(time, SimEvent.ALARM, d.kind.displayName(), "ЗАПУСК АУП");
                        emit(SimEvent.INFO, SimEvent.ALARM, d.pos, d.id, "ЗАПУСК АУП");
                    }
                }
            } else if (MODE_START.equals(r.mode)) {
                setAlertOutputs(r, true);
                r.startRemaining -= dt;
                if (r.startRemaining <= 0.0) {
                    r.mode = MODE_TUSH;
                    r.tushRemaining = ASPT_TUSH_S;
                    r.addLog("ТУШЕНИЕ: КОНТРОЛЬ ВЫХОДА ОТВ");
                    log.add(time, SimEvent.ALARM, d.kind.displayName(), "ТУШЕНИЕ");
                }
            } else if (MODE_TUSH.equals(r.mode)) {
                setAlertOutputs(r, false);
                r.tushRemaining -= dt;
                if (r.tushRemaining <= 0.0) {
                    r.mode = MODE_WAIT_RESET;
                    r.addLog("ОЖИДАНИЕ СБРОСА");
                }
            } else if (MODE_WAIT_RESET.equals(r.mode)) {
                setAlertOutputs(r, false);
                r.buzzer = false;
                if (reset) {
                    r.mode = MODE_IDLE;
                    r.alarm = false;
                    clearOutputs(r);
                    r.addLog("СБРОС ПОЖАРА И ПУСКА");
                    log.add(time, SimEvent.INFO, d.kind.displayName(), "СБРОС");
                }
            }
            r.remoteStartRequested = false;
        }
    }

    private void computeStart24(double dt) {
        for (Net net : graph.getNetsOfKind(WireKind.START24)) {
            LineSupervision.LineStatus st = LineSupervision.classify(net, WireKind.START24);
            Driver driver = findStartDriver(net);
            net.voltage = 0.0;
            net.currentA = st.supervisionCurrentA;
            net.fault = st.fault;
            List<SimDevice> pyros = net.devicesByKind(DeviceKind.PYRO);
            boolean fired = false;
            if (driver != null && st.healthy) {
                DeviceRuntime ar = rt(driver.device);
                net.voltage = ar.outputVoltage;
                ar.outputFault[4] = false;
                double current = Math.min(1.0, Math.max(0.0, net.voltage / 2.0));
                net.currentA = current + st.supervisionCurrentA;
                for (SimDevice pyro : pyros) {
                    DeviceRuntime pr = rt(pyro);
                    pr.voltage = net.voltage;
                    if (pr.spent || pyro.spent) {
                        pr.currentA = 0.0;
                        pr.pulseTime = 0.0;
                        continue;
                    }
                    pr.currentA = current;
                    if (current >= PYRO_TRIGGER_A) {
                        pr.pulseTime += dt;
                        if (pr.pulseTime >= PYRO_TRIGGER_S) {
                            pr.spent = true;
                            pyro.spent = true;
                            pr.alarm = true;
                            pr.pulseTime = 0.0;
                            fired = true;
                            pr.addLog("ПИРОПАТРОН СРАБОТАЛ: ВЫХОД ПОРОШКА");
                            log.add(time, SimEvent.ALARM, pyro.kind.displayName(), "ПУСКОВОЙ ТОК " + current + " А");
                            emit(SimEvent.PYRO_FIRED, SimEvent.ALARM, pyro.pos, pyro.id, "ПИРОПАТРОН СРАБОТАЛ");
                            emit(SimEvent.POWDER_DISCHARGE, SimEvent.ALARM, pyro.pos, pyro.id, "ВЫХОД ОГНЕТУШАЩЕГО ПОРОШКА");
                        }
                    } else {
                        pr.pulseTime = 0.0;
                    }
                }
                if (pyros.isEmpty()) net.fault = "НЕТ ПИРОПАТРОНА В ЦЕПИ ЗАПУСКА";
            } else {
                if (driver != null) rt(driver.device).outputFault[4] = true;
                for (SimDevice pyro : pyros) {
                    DeviceRuntime pr = rt(pyro);
                    pr.voltage = 0.0;
                    pr.currentA = 0.0;
                    pr.pulseTime = 0.0;
                }
            }
            if (fired) net.fault = "ПИРОПАТРОН СРАБОТАЛ";
            logFault(net.toString() + "|START", net.fault);
        }
    }

    private SimDevice findAsptForCoilNet(Net coilNet) {
        if (coilNet == null) return null;
        for (SimDevice a : devices) {
            if (!a.kind.isAspt()) continue;
            for (int i = 0; i < CONTROL_ROLES.length; i++) {
                if (graph.getNetForPort(a, CONTROL_ROLES[i]) == coilNet) return a;
            }
        }
        return null;
    }

    private void updateTablo() {
        for (SimDevice d : devices) {
            if (!d.kind.isTablo()) continue;
            DeviceRuntime r = rt(d);
            Net powerNet = graph.getNetForPort(d, "POWER12_IN");
            r.voltage = powerNet == null ? 0.0 : powerNet.voltage;
            boolean relayOk = false;
            boolean asptFlash = false;
            for (SimDevice relay : devices) {
                if (!relay.kind.isRelay()) continue;
                Net no1 = graph.getNetForPort(relay, "POWER12_NO1");
                Net no2 = graph.getNetForPort(relay, "POWER12_NO2");
                if (no1 != powerNet && no2 != powerNet) continue;
                DeviceRuntime rr = rt(relay);
                if (!rr.coilClosed) continue;
                relayOk = true;
                SimDevice aspt = findAsptForCoilNet(graph.getNetForPort(relay, "COIL24"));
                if (aspt != null) {
                    DeviceRuntime ar = rt(aspt);
                    asptFlash = ar.flash && isAlertMode(ar.mode);
                }
            }
            r.powered = r.voltage >= TABLO_MIN_V && r.voltage <= TABLO_MAX_V && !r.damaged;
            r.online = r.powered && relayOk;
            r.flash = asptFlash;
            r.lit = r.powered && relayOk && asptFlash && !r.damaged;
            if (r.damaged) r.fault = "ТАБЛО ПОВРЕЖДЕНО ПЕРЕНАПРЯЖЕНИЕМ";
            else if (r.voltage > 0.0 && r.voltage < TABLO_MIN_V) r.fault = "ПОНИЖЕННОЕ НАПРЯЖЕНИЕ ТАБЛО";
            else if (!relayOk && r.powered) r.fault = "ЦЕПЬ УПРАВЛЕНИЯ ТАБЛО РАЗОМКНУТА";
            else r.fault = "";
        }
    }

    private static String fmt(double v) {
        long x = Math.round(v * 10.0);
        return "" + (x / 10.0);
    }

    private static String crop16(String s) {
        if (s == null) return "";
        return s.length() <= 16 ? s : s.substring(0, 16);
    }

    private void updateIndication() {
        for (SimDevice d : devices) {
            DeviceRuntime r = rt(d);
            r.statusLines.clear();
            r.led[0] = r.powered || r.poweredInternal;
            r.led[1] = r.alarm || r.lit;
            r.led[2] = r.fault.length() > 0 || r.damaged || r.outputShutdown;
            r.led[3] = r.online;
            if (r.lcd[0].length() == 0) r.setLcd(d.kind.displayName(), "ПО v" + d.kind.firmware());
            r.display1 = r.lcd[0];
            r.display2 = r.lcd[1];

            if (d.kind == DeviceKind.RIP_12) {
                r.statusLines.add("РИП-12 исп.02 / РИП-12-2/7М1");
                r.statusLines.add("U вых = " + fmt(r.voltage) + " В");
                r.statusLines.add("I нагр = " + fmt(r.currentA) + " А (ном. 2 А / макс. 3 А)");
                r.statusLines.add("U АКБ = " + fmt(r.batteryVoltage) + " В, заряд " + (int) Math.round(r.batterySoc * 100.0) + "%");
                r.statusLines.add("~220 В: " + (r.mainsPresent ? "ЕСТЬ" : "АВАРИЯ СЕТИ"));
                r.statusLines.add("АКБ 12 В 7 А·ч: " + (r.hasBattery ? "УСТАНОВЛЕНА" : "НЕТ"));
                r.statusLines.add("Защита: КЗ / перегрузка / глубокий разряд 10,2 В");
            } else if (d.kind.isPanel()) {
                r.statusLines.add(d.kind.displayName() + " / ПО v" + d.kind.firmware());
                r.statusLines.add("ЖКИ 2x16: " + r.lcd[0]);
                r.statusLines.add("         " + r.lcd[1]);
                r.statusLines.add("RS-485: " + r.rsQualityPercent + "% / " + (r.online ? "ОПРОС" : "НЕТ СВЯЗИ"));
                r.statusLines.add("Устройств на шине: " + r.connectedDevices.size());
                r.statusLines.add("Удалённый пуск АСПТ: " + (d.kind.supportsRemoteStart() ? (r.remoteStartEnabled ? "ВКЛ" : "ВЫКЛ") : "НЕ ПОДДЕРЖИВАЕТСЯ 2.05"));
                r.statusLines.add("Адрес АСПТ в тактике: " + r.configuredAsptAddress);
                r.statusLines.add("Журнал событий 32000 (на экране последние 8):");
                int from = Math.max(0, r.log.size() - 8);
                for (int i = from; i < r.log.size(); i++) r.statusLines.add("  " + r.log.get(i));
            } else if (d.kind.isKdl()) {
                r.statusLines.add("С2000-КДЛ / РЭ v2.36");
                r.statusLines.add("Адрес RS-485: " + r.rsAddress);
                r.statusLines.add("ДПЛС: " + fmt(r.dplVoltage) + " В, R = " + fmt(r.dplResistance) + " Ом");
                r.statusLines.add("АУ онлайн: " + r.connectedAddressable + " / 127");
                r.statusLines.add("Кольцо ДПЛС: " + (r.dplResistance < DPL_MAX_R ? "КОНТРОЛЬ" : "R ВНЕ НОРМЫ"));
                r.statusLines.add("Тех. готовность 15 с: " + fmt(r.startupRemaining) + " с");
                r.statusLines.add("RS-485: " + r.rsQualityPercent + "%");
                r.statusLines.add("Макс. R ДПЛС 200 Ом, 8–11 В, до 127 АУ");
            } else if (d.kind.isAspt()) {
                r.statusLines.add("С2000-АСПТ / прошивка v3.53");
                r.statusLines.add("Режим: " + r.mode);
                r.statusLines.add("Автоматика: " + (r.autoEnabled ? "ВКЛЮЧЕНА" : "ОТКЛЮЧЕНА"));
                r.statusLines.add("Задержка: " + fmt(r.delayRemaining) + " с / пуск " + fmt(r.startRemaining) + " с");
                r.statusLines.add("ШС1/2/3: " + (r.loopAlarm[0] ? "ПОЖАР" : "НОРМА") + " / " + (r.loopAlarm[1] ? "ПОЖАР" : "НОРМА") + " / " + (r.loopAlarm[2] ? "ПОЖАР" : "НОРМА"));
                r.statusLines.add("Дверь: " + (r.doorOpen ? "ОТКРЫТА (автопуск заблокирован)" : "ЗАКРЫТА"));
                r.statusLines.add("Выходы СО1/СО2/ЗО: " + (r.outputs[0] ? "ВКЛ" : "ВЫКЛ") + " / " + (r.outputs[1] ? "ВКЛ" : "ВЫКЛ") + " / " + (r.outputs[2] ? "ВКЛ" : "ВЫКЛ"));
                r.statusLines.add("Цепь запуска: " + (r.startOutput ? "24 В / ИМПУЛЬС" : "КОНТРОЛЬ 1,5 мА"));
                r.statusLines.add("Оконечные резисторы 4,7 кОм, контроль обрыва/КЗ");
                r.statusLines.add("АКБ 2 x 4,5 А·ч: " + (int) Math.round(r.batterySoc * 100.0) + "%");
                int from = Math.max(0, r.log.size() - 4);
                for (int i = from; i < r.log.size(); i++) r.statusLines.add("  " + r.log.get(i));
            } else if (d.kind.isAddressable()) {
                r.statusLines.add(d.kind.displayName() + " / ДПЛС v2.xx");
                r.statusLines.add("Адрес в ДПЛС: " + r.dplAddress);
                r.statusLines.add("U в точке подключения: " + fmt(r.loopVoltage) + " В");
                r.statusLines.add("I дежурный: " + fmt(d.kind.standbyCurrentA() * 1000.0) + " мА");
                r.statusLines.add("Время готовности: 15 с, фиксация: 300 мс");
                r.statusLines.add("Состояние: " + (r.alarm ? (d.kind == DeviceKind.UDP_513_3AM ? "ПУСК ПОЖАРОТУШЕНИЯ" : "ПОЖАР") : "ДЕЖУРНЫЙ"));
                if (d.kind.hasDplIsolator()) r.statusLines.add("Встроенный БРИЗ: локализация КЗ <= 200 мс");
                if (r.duplicateAddress != 0) r.statusLines.add("ОШИБКА: дубль адреса " + r.duplicateAddress);
            } else if (d.kind.isPyro()) {
                r.statusLines.add("Пиропатрон порошковый (стендовый модуль)");
                r.statusLines.add("R мостика: 2,0 Ом / ток срабатывания >= 0,5 А");
                r.statusLines.add("Безопасный ток контроля: <= 50 мА");
                r.statusLines.add("U пуска: " + fmt(r.voltage) + " В, I = " + fmt(r.currentA) + " А");
                r.statusLines.add("Состояние: " + (r.spent ? "СРАБОТАЛ / ОДНОРАЗОВЫЙ" : "ГОТОВ"));
            } else if (d.kind.isTablo()) {
                r.statusLines.add(d.kind.displayName());
                r.statusLines.add("Молния-2-12: 9–13,8 В DC, 40 мА, двустороннее");
                r.statusLines.add("U = " + fmt(r.voltage) + " В");
                r.statusLines.add("Подсветка: " + (r.lit ? "ВКЛ" : "ВЫКЛ") + " / мигание 0,5 с");
                r.statusLines.add("Надпись видна с двух сторон");
            } else if (d.kind.isRelay()) {
                r.statusLines.add("Промежуточное реле, катушка 24 В");
                r.statusLines.add("Сухие контакты K1 / K2 (COM-NO)");
                r.statusLines.add("Катушка: " + (r.coilClosed ? "ПОД НАПРЯЖЕНИЕМ" : "ОБЕСТОЧЕНА"));
                r.statusLines.add("Контакты: " + (r.coilClosed ? "ЗАМКНУТЫ" : "РАЗОМКНУТЫ"));
            } else if (d.kind.isMains()) {
                r.statusLines.add("Стендовый ввод ~220 В, 50 Гц");
                r.statusLines.add("Состояние: " + (d.mainsEnabled ? "ВКЛЮЧЁН" : "ОТКЛЮЧЁН"));
                r.statusLines.add("Питает РИП-12 и АСПТ в радиусе 2 блоков");
            } else if (d.kind.isLoopSensor()) {
                r.statusLines.add("Неадресный шлейфный извещатель / сухой контакт");
                r.statusLines.add("ШС типа 2, оконечный резистор 4,7 кОм");
                r.statusLines.add("Состояние: " + (r.sensorTriggered ? "ТРЕВОГА" : "НОРМА"));
            } else if (d.kind.isDoorContact()) {
                r.statusLines.add("Датчик состояния двери");
                r.statusLines.add("Дверь: " + (r.doorOpen ? "ОТКРЫТА" : "ЗАКРЫТА"));
                r.statusLines.add("Открытая дверь блокирует автоматический пуск");
            }
            if (r.fault.length() > 0) r.statusLines.add("! " + r.fault);
        }
    }

    private void emit(int type, int severity, Pos pos, String deviceId, String text) {
        pending.add(new SimEvent(type, severity, time, pos, deviceId, text));
        while (pending.size() > 256) pending.remove(0);
    }

    private void logFault(String key, String text) {
        String old = lastFaults.get(key);
        if (old == null) old = "";
        if (old.equals(text)) return;
        lastFaults.put(key, text);
        if (text.length() > 0) {
            log.add(time, SimEvent.WARNING, key, text);
            emit(SimEvent.INFO, SimEvent.WARNING, null, key, text);
        } else if (old.length() > 0) {
            log.add(time, SimEvent.INFO, key, "ВОССТАНОВЛЕНО");
        }
    }

    public DeviceRuntime getRuntime(String id) { return runtimes.get(id); }
    public Map<String, DeviceRuntime> getRuntimes() { return runtimes; }
    public NetworkGraph getGraph() { return graph; }
    public EventLog getGlobalLog() { return log; }
    public List<SimEvent> drainEvents() {
        List<SimEvent> out = new ArrayList<SimEvent>(pending);
        pending.clear();
        return out;
    }
    public double getTime() { return time; }
    public long getTick() { return tick; }
    public double getTimeScale() { return timeScale; }
    public void setTimeScale(double scale) { timeScale = Math.max(0.1, Math.min(20.0, scale)); }
    public Net getNetForPort(SimDevice d, String role) { return graph.getNetForPort(d, role); }
}
