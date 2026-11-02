package ru.bolidalarmpro.sim;

import java.util.ArrayList;
import java.util.List;

public final class EventLog {
    public static final class Entry {
        public final double time;
        public final int severity;
        public final String source;
        public final String text;
        Entry(double time, int severity, String source, String text) {
            this.time = time;
            this.severity = severity;
            this.source = source;
            this.text = text;
        }
        @Override
        public String toString() {
            return "[" + (int) time + "с] " + source + ": " + text;
        }
    }

    private final List<Entry> entries = new ArrayList<Entry>();

    public void add(double time, int severity, String source, String text) {
        entries.add(new Entry(time, severity, source, text));
        while (entries.size() > 32000) entries.remove(0);
    }

    public List<Entry> getLast(int count) {
        int from = Math.max(0, entries.size() - count);
        List<Entry> out = new ArrayList<Entry>();
        for (int i = from; i < entries.size(); i++) out.add(entries.get(i));
        return out;
    }

    public int size() { return entries.size(); }
    public void clear() { entries.clear(); }
}
