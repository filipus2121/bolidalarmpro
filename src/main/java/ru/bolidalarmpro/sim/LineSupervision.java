package ru.bolidalarmpro.sim;

/** Thresholds for supervised lines: EOL resistors, terminators, shorts, polarity, loop resistance. */
public final class LineSupervision {
    public static final class LineStatus {
        public boolean healthy;
        public String fault = "";
        public String warning = "";
        public boolean shortCircuit;
        public boolean openCircuit;
        public boolean eolOk;
        public boolean terminationOk;
        public boolean resistanceOk;
        public boolean polarityOk;
        public double supervisionCurrentA;
    }

    private LineSupervision() {}

    public static LineStatus classify(Net net, WireKind kind) {
        LineStatus s = new LineStatus();
        s.eolOk = true;
        s.terminationOk = true;
        s.resistanceOk = true;
        s.polarityOk = true;
        if (net == null) {
            s.healthy = false;
            s.openCircuit = true;
            s.fault = "НЕТ ЛИНИИ";
            return s;
        }
        if (net.shortCount > 0) {
            s.shortCircuit = true;
            s.fault = "КЗ ЛИНИИ";
        }
        if (kind.needsEol()) {
            if (net.eolCount == 0) {
                s.openCircuit = true;
                s.eolOk = false;
                if (s.fault.length() == 0) s.fault = "ОБРЫВ / НЕТ ОКОНЕЧНОГО РЕЗИСТОРА";
            } else if (net.eolCount > 1) {
                s.eolOk = false;
                if (s.fault.length() == 0) s.fault = "НЕСКОЛЬКО ОКОНЕЧНЫХ РЕЗИСТОРОВ";
            }
            s.supervisionCurrentA = kind == WireKind.FIRE_ZONE ? 0.003 : 0.0015;
        }
        if (kind.needsTerminators()) {
            if (net.terminatorCount == 0) {
                s.terminationOk = false;
                if (s.fault.length() == 0) s.fault = "НЕТ ТЕРМИНАТОРОВ 120 ОМ";
                s.warning = "БЕЗ ТЕРМИНАТОРОВ: ПОМЕХИ НА RS-485";
            } else if (net.terminatorCount == 1) {
                s.terminationOk = false;
                s.warning = "ОДИН ТЕРМИНАТОР ВМЕСТО ДВУХ";
            } else if (net.terminatorCount > 2) {
                s.terminationOk = false;
                s.warning = "ЛИШНИЕ ТЕРМИНАТОРЫ 120 ОМ";
            }
            if (net.swapCount > 0) {
                s.polarityOk = false;
                if (s.fault.length() == 0) s.fault = "ПЕРЕПУТАНЫ A/B RS-485";
            }
        }
        if (kind == WireKind.DPL && net.resistanceOhms > 200.0) {
            s.resistanceOk = false;
            if (s.fault.length() == 0) s.fault = "R ДПЛС > 200 ОМ";
        }
        if (net.resistanceOhms > kind.maxLength() * kind.loopResistancePerBlock()) {
            s.resistanceOk = false;
            if (s.fault.length() == 0) s.fault = "ПРЕВЫШЕНА ДЛИНА ЛИНИИ";
        }
        if ((kind == WireKind.POWER12 || kind == WireKind.CONTROL24 || kind == WireKind.START24)
                && net.polarityCount > 0) {
            s.polarityOk = false;
            if (s.fault.length() == 0) s.fault = "ОБРАТНАЯ ПОЛЯРНОСТЬ";
        }
        s.healthy = s.fault.length() == 0;
        return s;
    }
}
