package ru.bolidalarmpro.sim;

/** A world-side effect requested by the simulator (particles, extinguishing, sounds). */
public final class SimEvent {
    public static final int INFO = 0;
    public static final int WARNING = 1;
    public static final int ALARM = 2;
    public static final int PYRO_FIRED = 3;
    public static final int POWDER_DISCHARGE = 4;
    public static final int BUZZER = 5;

    public final int type;
    public final int severity;
    public final double time;
    public final Pos pos;
    public final String deviceId;
    public final String text;

    public SimEvent(int type, int severity, double time, Pos pos, String deviceId, String text) {
        this.type = type;
        this.severity = severity;
        this.time = time;
        this.pos = pos;
        this.deviceId = deviceId;
        this.text = text;
    }
}
