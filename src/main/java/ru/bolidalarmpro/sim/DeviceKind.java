package ru.bolidalarmpro.sim;

/** Exactly the devices of update 0.1.0 plus clearly labelled laboratory infrastructure. */
public enum DeviceKind {
    RIP_12("РИП-12 исп.02", "2/7М1", "rip12", 13.6, 10.2, 14.2, 0.040, 0.040, 0, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false),
    S2000M_205("С2000М", "2.05", "s2000m205", 12.0, 10.2, 28.4, 0.120, 0.140, 0, false, true, true, false, false, false, false, false, false, false, false, false, false, false, false),
    S2000M_502_02("С2000М исп.02", "5.02", "s2000m502", 12.0, 10.2, 28.4, 0.120, 0.140, 0, false, true, true, false, false, false, false, false, false, false, false, false, false, true, false),
    KDL_236("С2000-КДЛ", "2.36", "kdl236", 12.0, 10.2, 28.4, 0.080, 0.160, 15, false, true, false, true, false, false, false, false, false, false, false, false, false, false, false),
    ASPT_353("С2000-АСПТ", "3.53", "aspt353", 24.0, 20.4, 27.6, 0.000, 0.000, 0, false, true, false, false, true, false, false, false, false, false, false, false, false, false, false),
    IPR_513_3AM("ИПР 513-3АМ", "3АМ", "ipr513", 9.5, 8.0, 11.0, 0.0005, 0.0005, 15, true, false, false, false, false, false, true, false, false, false, false, false, false, false, false),
    UDP_513_3AM("УДП 513-3АМ", "3АМ", "udp513", 9.5, 8.0, 11.0, 0.0006, 0.0030, 15, true, false, false, false, false, false, true, false, false, false, false, false, true, false, false),
    PYRO("Пиропатрон порошковый", "стендовый", "pyro", 24.0, 6.0, 28.0, 0.000, 1.000, 0, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false),
    TABLO_NE_VHODI("Молния2-12 «Порошок! НЕ ВХОДИ!»", "12 В", "tablo_ne", 12.0, 9.0, 13.8, 0.040, 0.040, 0, false, false, false, false, false, false, false, false, true, false, false, false, false, false, true),
    TABLO_UHODI("Молния2-12 «Порошок! УХОДИ!»", "12 В", "tablo_uh", 12.0, 9.0, 13.8, 0.040, 0.040, 0, false, false, false, false, false, false, false, false, true, false, false, false, false, false, true),
    MAINS_220("Стендовый ввод ~220 В", "лаборатория", "mains220", 220.0, 187.0, 242.0, 0.000, 0.000, 0, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false),
    RELAY_12("Промежуточное реле 12 В", "2 контакта", "relay12", 24.0, 20.4, 27.6, 0.050, 0.050, 0, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false),
    LOOP_SENSOR("Шлейфный извещатель", "сухой контакт", "loop_sensor", 24.0, 0.0, 27.0, 0.003, 0.003, 0, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false),
    DOOR_CONTACT("Датчик состояния двери", "сухой контакт", "door_contact", 24.0, 0.0, 27.0, 0.003, 0.003, 0, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false);

    private final String displayName;
    private final String firmware;
    private final String texture;
    private final double nominalVoltage;
    private final double minVoltage;
    private final double maxVoltage;
    private final double standbyCurrentA;
    private final double activeCurrentA;
    private final int startupSeconds;
    private final boolean addressable;
    private final boolean rs485;
    private final boolean panel;
    private final boolean kdl;
    private final boolean aspt;
    private final boolean mains;
    private final boolean manualCallPoint;
    private final boolean pyro;
    private final boolean tablo;
    private final boolean relay;
    private final boolean loopSensor;
    private final boolean doorContact;
    private final boolean dplIsolator;
    private final boolean supportsRemoteStart;
    private final boolean twoSidedTablo;

    DeviceKind(String displayName, String firmware, String texture,
               double nominalVoltage, double minVoltage, double maxVoltage,
               double standbyCurrentA, double activeCurrentA, int startupSeconds,
               boolean addressable, boolean rs485, boolean panel, boolean kdl,
               boolean aspt, boolean mains, boolean manualCallPoint, boolean pyro,
               boolean tablo, boolean relay, boolean loopSensor, boolean doorContact,
               boolean dplIsolator, boolean supportsRemoteStart, boolean twoSidedTablo) {
        this.displayName = displayName;
        this.firmware = firmware;
        this.texture = texture;
        this.nominalVoltage = nominalVoltage;
        this.minVoltage = minVoltage;
        this.maxVoltage = maxVoltage;
        this.standbyCurrentA = standbyCurrentA;
        this.activeCurrentA = activeCurrentA;
        this.startupSeconds = startupSeconds;
        this.addressable = addressable;
        this.rs485 = rs485;
        this.panel = panel;
        this.kdl = kdl;
        this.aspt = aspt;
        this.mains = mains;
        this.manualCallPoint = manualCallPoint;
        this.pyro = pyro;
        this.tablo = tablo;
        this.relay = relay;
        this.loopSensor = loopSensor;
        this.doorContact = doorContact;
        this.dplIsolator = dplIsolator;
        this.supportsRemoteStart = supportsRemoteStart;
        this.twoSidedTablo = twoSidedTablo;
    }

    public String displayName() { return displayName; }
    public String firmware() { return firmware; }
    public String texture() { return texture; }
    public double nominalVoltage() { return nominalVoltage; }
    public double minVoltage() { return minVoltage; }
    public double maxVoltage() { return maxVoltage; }
    public double standbyCurrentA() { return standbyCurrentA; }
    public double activeCurrentA() { return activeCurrentA; }
    public int startupSeconds() { return startupSeconds; }
    public boolean isAddressable() { return addressable; }
    public boolean isRs485() { return rs485; }
    public boolean isPanel() { return panel; }
    public boolean isKdl() { return kdl; }
    public boolean isAspt() { return aspt; }
    public boolean isMains() { return mains; }
    public boolean isManualCallPoint() { return manualCallPoint; }
    public boolean isPyro() { return pyro; }
    public boolean isTablo() { return tablo; }
    public boolean isRelay() { return relay; }
    public boolean isLoopSensor() { return loopSensor; }
    public boolean isDoorContact() { return doorContact; }
    public boolean hasDplIsolator() { return dplIsolator; }
    public boolean supportsRemoteStart() { return supportsRemoteStart; }
    public boolean isTwoSidedTablo() { return twoSidedTablo; }
    public boolean isInfrastructure() { return mains || relay || loopSensor || doorContact; }
}
