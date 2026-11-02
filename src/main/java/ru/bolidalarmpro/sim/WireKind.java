package ru.bolidalarmpro.sim;

/** Physical cable classes. One in-world cable block equals one metre of a two-core pair. */
public enum WireKind {
    RS485("RS-485", 0.0, 0.075, 3000.0, false, true),
    DPL("ДПЛС", 9.5, 0.075, 1200.0, false, false),
    POWER12("+12 В", 13.6, 0.075, 200.0, false, false),
    CONTROL24("Выход 24 В", 24.0, 0.075, 200.0, true, false),
    START24("Цепь запуска", 24.0, 0.075, 200.0, true, false),
    FIRE_ZONE("ШС / сухой контакт", 24.0, 0.075, 200.0, true, false);

    private final String label;
    private final double nominalVoltage;
    private final double loopResistancePerBlock;
    private final double maxLength;
    private final boolean needsEol;
    private final boolean needsTerminators;

    WireKind(String label, double nominalVoltage, double loopResistancePerBlock,
             double maxLength, boolean needsEol, boolean needsTerminators) {
        this.label = label;
        this.nominalVoltage = nominalVoltage;
        this.loopResistancePerBlock = loopResistancePerBlock;
        this.maxLength = maxLength;
        this.needsEol = needsEol;
        this.needsTerminators = needsTerminators;
    }

    public String label() { return label; }
    public double nominalVoltage() { return nominalVoltage; }
    public double loopResistancePerBlock() { return loopResistancePerBlock; }
    public double maxLength() { return maxLength; }
    public boolean needsEol() { return needsEol; }
    public boolean needsTerminators() { return needsTerminators; }
    public String id() { return name().toLowerCase(); }
}
