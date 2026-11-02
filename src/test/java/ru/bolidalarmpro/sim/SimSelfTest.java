package ru.bolidalarmpro.sim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** No JUnit: a plain main() so the simulator can be checked without Minecraft/Forge. */
public final class SimSelfTest {
    private static final class Opt {
        boolean shortPower;
        boolean duplicateRs;
        boolean duplicateDpl;
        boolean mainsOn = true;
        boolean battery = true;
        boolean test24V;
        boolean sensors;
        boolean doorOpen;
        boolean noTerminators;
        boolean dplShort;
        boolean noStartEol;
        boolean tabloDirect24V;
    }

    private static final class Fixture {
        Simulation sim;
        final List<WireNode> wires = new ArrayList<WireNode>();
        final List<SimDevice> devices = new ArrayList<SimDevice>();
        final Map<String, SimDevice> byId = new HashMap<String, SimDevice>();
        SimDevice get(String id) { return byId.get(id); }
        DeviceRuntime rt(String id) { return sim.getRuntime(id); }
        void rebuild() { sim.setTopology(NetworkGraph.build(wires, devices), devices); }
    }

    private static final class Lane {
        final WireKind kind;
        final int channel;
        final int dy;
        final int dz;
        Lane(WireKind kind, int channel, int dy, int dz) {
            this.kind = kind;
            this.channel = channel;
            this.dy = dy;
            this.dz = dz;
        }
        String key() { return kind.id() + "/" + channel; }
    }

    private static final Lane[] LANES = new Lane[] {
        new Lane(WireKind.POWER12, 0, 0, 1),
        new Lane(WireKind.RS485, 0, 0, 2),
        new Lane(WireKind.DPL, 0, 0, 3),
        new Lane(WireKind.CONTROL24, 1, 0, -1),
        new Lane(WireKind.START24, 1, 0, -2),
        new Lane(WireKind.FIRE_ZONE, 1, 0, -3),
        new Lane(WireKind.FIRE_ZONE, 2, 1, 0),
        new Lane(WireKind.FIRE_ZONE, 3, -3, 0),
        new Lane(WireKind.FIRE_ZONE, 4, 2, 0),
        new Lane(WireKind.POWER12, 1, -1, 0),
        new Lane(WireKind.POWER12, 2, -2, 0)
    };

    private static void dev(Fixture f, String id, DeviceKind kind, int x) {
        dev(f, id, kind, new Pos(x, 0, 0));
    }

    private static void dev(Fixture f, String id, DeviceKind kind, Pos pos) {
        SimDevice d = SimDevice.createDefault(id, kind, pos);
        f.devices.add(d);
        f.byId.put(id, d);
    }

    private static CableFlags flagsFor(Opt o, Lane lane, int x) {
        CableFlags f = new CableFlags();
        if (o.shortPower && lane.kind == WireKind.POWER12 && lane.channel == 0 && x == 6) f.shortFault = true;
        if (o.dplShort && lane.kind == WireKind.DPL && lane.channel == 0 && x == 18) f.shortFault = true;
        if (lane.kind == WireKind.RS485 && !o.noTerminators && (x == 0 || x == 26)) f.terminator = true;
        if (lane.kind == WireKind.CONTROL24 && lane.channel == 1 && x == 10) f.eol = true;
        if (lane.kind == WireKind.START24 && lane.channel == 1 && x == 16 && !o.noStartEol) f.eol = true;
        if (lane.kind == WireKind.FIRE_ZONE && lane.channel == 1 && x == 22) f.eol = true;
        if (lane.kind == WireKind.FIRE_ZONE && lane.channel == 2 && x == 24) f.eol = true;
        if (lane.kind == WireKind.FIRE_ZONE && lane.channel == 3 && x == 0) f.eol = true;
        if (lane.kind == WireKind.FIRE_ZONE && lane.channel == 4 && x == 26) f.eol = true;
        return f;
    }

    private static Fixture buildSystem(Opt o) {
        Fixture f = new Fixture();
        dev(f, "mains", DeviceKind.MAINS_220, 0);
        dev(f, "rip", DeviceKind.RIP_12, 2);
        dev(f, "panel", DeviceKind.S2000M_502_02, 4);
        dev(f, "kdl", DeviceKind.KDL_236, 6);
        dev(f, "aspt", DeviceKind.ASPT_353, 8);
        dev(f, "relay", DeviceKind.RELAY_12, 10);
        dev(f, "tablo_ne", DeviceKind.TABLO_NE_VHODI, 12);
        dev(f, "tablo_uh", DeviceKind.TABLO_UHODI, 14);
        dev(f, "pyro", DeviceKind.PYRO, 16);
        dev(f, "ipr", DeviceKind.IPR_513_3AM, 18);
        dev(f, "udp", DeviceKind.UDP_513_3AM, 20);
        dev(f, "sensor1", DeviceKind.LOOP_SENSOR, 22);
        dev(f, "sensor2", DeviceKind.LOOP_SENSOR, 24);
        dev(f, "door", DeviceKind.DOOR_CONTACT, 26);

        f.get("mains").mainsEnabled = o.mainsOn;
        f.get("rip").hasBattery7 = o.battery;
        f.get("rip").test24V = o.test24V;
        f.get("aspt").hasBattery45x2 = o.battery;
        f.get("aspt").autoEnabled = true;
        f.get("panel").remoteStartEnabled = true;
        f.get("panel").configuredAsptAddress = 2;
        f.get("kdl").rsAddress = 1;
        f.get("aspt").rsAddress = o.duplicateRs ? 1 : 2;
        f.get("ipr").dplAddress = 1;
        f.get("udp").dplAddress = o.duplicateDpl ? 1 : 2;
        f.get("sensor1").sensorTriggered = o.sensors;
        f.get("sensor2").setChannel("LOOP", 1);
        f.get("sensor2").sensorTriggered = o.sensors;
        f.get("sensor2").setChannel("LOOP", 2);
        f.get("door").doorOpen = o.doorOpen;

        if (o.tabloDirect24V) {
            dev(f, "mains24a", DeviceKind.MAINS_220, new Pos(12, -1, 2));
            dev(f, "mains24b", DeviceKind.MAINS_220, new Pos(14, -2, 2));
            dev(f, "rip24a", DeviceKind.RIP_12, new Pos(12, -1, 1));
            dev(f, "rip24b", DeviceKind.RIP_12, new Pos(14, -2, 1));
            f.get("rip24a").setChannel("POWER12_OUT", 1);
            f.get("rip24b").setChannel("POWER12_OUT", 2);
            f.get("rip24a").test24V = true;
            f.get("rip24b").test24V = true;
            f.get("rip24a").hasBattery7 = true;
            f.get("rip24b").hasBattery7 = true;
        }

        for (Lane lane : LANES) {
            for (int x = 0; x <= 26; x++) {
                f.wires.add(new WireNode(new Pos(x, lane.dy, lane.dz), lane.kind, lane.channel, flagsFor(o, lane, x)));
            }
        }
        f.sim = new Simulation(f.wires, f.devices);
        return f;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new RuntimeException(message);
    }

    private static void tick(Fixture f, double seconds) {
        int n = (int) (seconds * 10.0);
        for (int i = 0; i < n; i++) f.sim.tick(0.1);
    }

    private static void tickLarge(Fixture f, double seconds) {
        int n = (int) seconds;
        for (int i = 0; i < n; i++) f.sim.tick(1.0);
    }

    private static boolean contains(List<String> lines, String needle) {
        for (String s : lines) if (s.indexOf(needle) >= 0) return true;
        return false;
    }

    private static int eventCount(List<SimEvent> events, int type) {
        int n = 0;
        for (SimEvent e : events) if (e.type == type) n++;
        return n;
    }

    private static void testNominal() {
        Fixture f = buildSystem(new Opt());
        tick(f, 16.0);
        check(f.rt("rip").voltage > 13.0, "РИП-12 должен выдавать 13,6 В от сети");
        check(f.rt("panel").powered, "С2000М должен питаться от РИП-12");
        check(f.rt("kdl").ready, "С2000-КДЛ должен выйти в техготовность за 15 с");
        check(f.rt("panel").rsQualityPercent >= 95, "RS-485 с двумя терминаторами должен быть стабилен");
        check(f.rt("ipr").online, "ИПР 513-3АМ должен опрашиваться по ДПЛС");
        check(f.rt("udp").online, "УДП 513-3АМ должен опрашиваться по ДПЛС");
        check(f.rt("aspt").poweredInternal, "С2000-АСПТ должен иметь основное/резервное питание");
        check(Simulation.MODE_IDLE.equals(f.rt("aspt").mode), "АСПТ должен быть в дежурном режиме");
        check(!f.rt("tablo_ne").lit, "В дежурном режиме табло не должно светиться");
        check(!f.rt("tablo_ne").damaged, "При 13,6 В табло не должно повреждаться");
    }

    private static void testBatteryAndOverload() {
        Opt o = new Opt();
        o.mainsOn = false;
        o.battery = true;
        Fixture f = buildSystem(o);
        tickLarge(f, 3600.0);
        check(f.rt("rip").batterySoc < 1.0, "АКБ 7 А·ч должна разряжаться при пропаже сети");
        check(f.rt("rip").outputVoltage < 13.6, "Напряжение от АКБ должно быть ниже сетевого 13,6 В");
        check(f.rt("aspt").batterySoc < 1.0, "АКБ 2x4,5 А·ч АСПТ должна разряжаться");
        check(f.rt("panel").powered, "Система должна работать от АКБ");

        Opt s = new Opt();
        s.shortPower = true;
        Fixture g = buildSystem(s);
        tick(g, 1.0);
        check(g.rt("rip").outputShutdown, "КЗ на выходе РИП-12 должно отключить нагрузку");
        check(!g.rt("panel").powered, "При КЗ 12 В пульт должен обесточиться");
    }

    private static void testOvervoltage() {
        Opt o = new Opt();
        o.test24V = true;
        o.tabloDirect24V = true;
        Fixture f = buildSystem(o);
        tick(f, 1.0);
        check(f.rt("tablo_ne").damaged, "24 В вместо 12 В должны повредить Молния2-12");
        check(f.rt("tablo_uh").damaged, "24 В вместо 12 В должны повредить второе табло");
    }

    private static void testRs485AndDplDiagnostics() {
        Opt rs = new Opt();
        rs.duplicateRs = true;
        Fixture a = buildSystem(rs);
        tick(a, 2.0);
        check(a.rt("panel").rsQualityPercent == 0, "Дубль адреса RS-485 должен обрушить опрос");
        check(!a.rt("kdl").online, "КДЛ с дублем адреса должен быть офлайн");

        Opt noTerm = new Opt();
        noTerm.noTerminators = true;
        Fixture b = buildSystem(noTerm);
        tick(b, 2.0);
        check(b.rt("panel").rsQualityPercent <= 40, "Без терминаторов 120 Ом качество RS-485 должно падать");

        Opt dpl = new Opt();
        dpl.duplicateDpl = true;
        Fixture c = buildSystem(dpl);
        tick(c, 16.0);
        check(!c.rt("ipr").online && !c.rt("udp").online, "Дубль адреса ДПЛС должен отключить оба АУ");
        check(c.rt("kdl").fault.indexOf("ДУБЛЬ") >= 0, "КДЛ должен сообщить о дубле адреса");

        Opt sh = new Opt();
        sh.dplShort = true;
        Fixture d = buildSystem(sh);
        tick(d, 16.0);
        check(d.rt("kdl").fault.indexOf("БРИЗ") >= 0, "Встроенный БРИЗ УДП должен локализовать КЗ ДПЛС");
        check(d.rt("ipr").online, "После локализации КЗ ИПР должен остаться на связи");

        Opt eol = new Opt();
        eol.noStartEol = true;
        Fixture e = buildSystem(eol);
        tick(e, 1.0);
        check(e.rt("aspt").outputFault[4], "Без оконечного резистора цепь запуска должна быть в неисправности");
    }

    private static void testManualCallPointAndRemoteStart() {
        Fixture f = buildSystem(new Opt());
        tick(f, 16.0);
        f.get("ipr").alarmLatched = true;
        tick(f, 0.5);
        check(contains(f.rt("panel").log, "ПОЖАР"), "Нажатие ИПР должно попасть в журнал С2000М");
        check(f.rt("panel").lcd[0].indexOf("ПОЖАР") >= 0, "ЖКИ С2000М должен показать ПОЖАР");
        check(!f.rt("ipr").eventPending, "Событие ИПР должно быть доставлено и снято с очереди");

        f.get("udp").alarmLatched = true;
        tick(f, 0.5);
        tick(f, 0.1);
        check(contains(f.rt("panel").log, "ПУСК АУП"), "УДП должен передать ПУСК АУП на пульт");
        check(Simulation.MODE_DELAY.equals(f.rt("aspt").mode), "5.02 должен дистанционно перевести АСПТ в задержку пуска");
        check(f.rt("aspt").delayRemaining <= 15.0 && f.rt("aspt").delayRemaining > 13.0,
              "Дистанционный пуск должен дать задержку 15 с");

        f.sim.drainEvents();
        boolean litSeen = false;
        for (int i = 0; i < 160; i++) {
            f.sim.tick(0.1);
            if (f.rt("tablo_ne").lit || f.rt("tablo_uh").lit) litSeen = true;
        }
        List<SimEvent> events = f.sim.drainEvents();
        check(litSeen, "Табло «Порошок!» должны мигать во время задержки/пуска");
        check(eventCount(events, SimEvent.PYRO_FIRED) >= 1, "Пусковой импульс должен поджечь пиропатрон");
        check(f.rt("pyro").spent, "Пиропатрон одноразовый и должен остаться сработавшим");
        f.sim.tick(0.1);
        check(eventCount(f.sim.drainEvents(), SimEvent.PYRO_FIRED) == 0, "Сработавший пиропатрон не должен срабатывать повторно");
    }

    private static void testAutomaticStartAndDoor() {
        Opt o = new Opt();
        o.sensors = true;
        Fixture f = buildSystem(o);
        tick(f, 1.0);
        check(Simulation.MODE_DELAY.equals(f.rt("aspt").mode), "Два шлейфных извещателя должны включить автоматический пуск");
        check(f.rt("aspt").delayRemaining > 28.0, "Автоматический пуск должен иметь задержку 30 с");

        Opt d = new Opt();
        d.sensors = true;
        d.doorOpen = true;
        Fixture g = buildSystem(d);
        tick(g, 1.0);
        check(Simulation.MODE_IDLE.equals(g.rt("aspt").mode), "Открытая дверь должна блокировать автоматику");
    }

    public static void main(String[] args) {
        testNominal();
        testBatteryAndOverload();
        testOvervoltage();
        testRs485AndDplDiagnostics();
        testManualCallPointAndRemoteStart();
        testAutomaticStartAndDoor();
        System.out.println("SIM SELF-TEST OK: 6 groups / 39 checks");
    }
}
