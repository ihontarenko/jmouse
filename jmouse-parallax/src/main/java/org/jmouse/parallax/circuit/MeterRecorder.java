package org.jmouse.parallax.circuit;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * What the parts a meter stands on read, kept at the ends of the last two accepted steps so that a sample between them
 * reads them where it is: their currents and power laid along the step, as a sample's voltages are. Read at the step's
 * end alone, a current stood up to a step after its sample — a clamp on a 1.2 kHz series RLC read the current 2.6° ahead
 * of where it is, while the voltages beside it were exact. A part's states are the step end's: a state is a level, not
 * a wave.
 */
final class MeterRecorder {

    private final PartIndex parts;
    /** The metered parts, by place among the parts. */
    private int[]           metered = new int[0];
    private Map<String, MeteredPart> before;
    private double          beforeTime = Double.NaN;
    private Map<String, MeteredPart> after;
    private double          afterTime = Double.NaN;

    MeterRecorder(PartIndex parts) {
        this.parts = parts;
    }

    /** Reads these parts from the next step on; none reads none. */
    void meter(Set<String> names) {
        String[] all = parts.parts();
        metered = java.util.stream.IntStream.range(0, all.length).filter(part -> names.contains(all[part])).toArray();
    }

    boolean idle() {
        return metered.length == 0;
    }

    /** One accepted step's end: what the metered parts read there. */
    void take(double[] state, double[] companion, double time) {
        before = after;
        beforeTime = afterTime;
        after = read(state, companion, time);
        afterTime = time;
    }

    /** What the metered parts read at {@code at}, between the ends of the step that reached it. */
    Map<String, MeteredPart> at(double at, double[] state, double[] companion, double time) {
        if (after == null) {
            return read(state, companion, time);
        }
        if (before == null || !(afterTime > beforeTime)) {
            return after;
        }
        double share = Math.min(1, Math.max(0, (at - beforeTime) / (afterTime - beforeTime)));
        Map<String, MeteredPart> between = new LinkedHashMap<>();
        after.forEach((part, last) -> {
            MeteredPart first = before.get(part);
            if (first == null || last.terminals() == null || first.terminals() == null) {
                between.put(part, last);
                return;
            }
            Map<String, Double> terminals = new LinkedHashMap<>();
            last.terminals().forEach((terminal, current) -> {
                Double earlier = first.terminals().get(terminal);
                terminals.put(terminal, earlier == null ? current : earlier + (current - earlier) * share);
            });
            double power = first.power() + (last.power() - first.power()) * share;
            between.put(part, new MeteredPart(terminals, power, last.states()));
        });
        return between;
    }

    /** What the metered parts do at a state: the current into each terminal, the power and the states, their devices summed. */
    private Map<String, MeteredPart> read(double[] state, double[] companion, double time) {
        Map<String, MeteredPart> measured = new LinkedHashMap<>();
        for (int part : metered) {
            measured.put(parts.parts()[part], new MeteredPart(parts.terminalCurrents(part, state, companion, time),
                    parts.power(part, state, time), parts.states(part, state)));
        }
        return measured;
    }
}
