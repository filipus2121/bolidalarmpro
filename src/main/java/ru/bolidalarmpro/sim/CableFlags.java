package ru.bolidalarmpro.sim;

/** Faults and end-of-line parts attached to one cable block. */
public final class CableFlags {
    public boolean shortFault;
    public boolean eol;
    public boolean terminator;
    public boolean swappedAB;
    public boolean reversedPolarity;

    public static final int SHORT = 1;
    public static final int EOL = 2;
    public static final int TERMINATOR = 4;
    public static final int SWAPPED_AB = 8;
    public static final int REVERSED_POLARITY = 16;

    public int toBits() {
        int b = 0;
        if (shortFault) b |= SHORT;
        if (eol) b |= EOL;
        if (terminator) b |= TERMINATOR;
        if (swappedAB) b |= SWAPPED_AB;
        if (reversedPolarity) b |= REVERSED_POLARITY;
        return b;
    }

    public static CableFlags fromBits(int b) {
        CableFlags f = new CableFlags();
        f.shortFault = (b & SHORT) != 0;
        f.eol = (b & EOL) != 0;
        f.terminator = (b & TERMINATOR) != 0;
        f.swappedAB = (b & SWAPPED_AB) != 0;
        f.reversedPolarity = (b & REVERSED_POLARITY) != 0;
        return f;
    }

    public boolean empty() { return toBits() == 0; }
}
