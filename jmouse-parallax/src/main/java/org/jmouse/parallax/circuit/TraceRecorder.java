package org.jmouse.parallax.circuit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The stretch between two traced samples of a trace that keeps one sample in several, gathered step by step: a column
 * for every terminal of every part, laid out once, each device writing its terminals' currents into an array of its
 * own — no map is built on a step. What a {@link TraceWindow} then means and RMSes.
 */
final class TraceRecorder {

    private final PartIndex   parts;
    private final TraceWindow window;
    /** Each part's devices, and for each the column each of its terminals goes into. */
    private final int[]       devices;
    private final int[][]     columns;
    /** Each of those devices' terminal currents on a step, and every column's sum. */
    private final double[][]  deviceCurrents;
    private final double[]    stepCurrents;

    TraceRecorder(PartIndex parts, int nodes) {
        this.parts = parts;
        Map<String, Integer> places = new LinkedHashMap<>();
        List<String> columnParts = new ArrayList<>();
        List<String> columnTerminals = new ArrayList<>();
        List<Integer> traced = new ArrayList<>();
        List<int[]> terminalColumns = new ArrayList<>();
        for (int part = 0; part < parts.count(); part++) {
            String name = parts.parts()[part];
            for (int device : parts.devicesOf(part)) {
                String[] terminals = parts.device(device).reportedTerminals();
                int[] own = new int[terminals.length];
                for (int terminal = 0; terminal < terminals.length; terminal++) {
                    String terminalName = terminals[terminal];
                    own[terminal] = places.computeIfAbsent(name + '\u0000' + terminalName, ignored -> {
                        columnParts.add(name);
                        columnTerminals.add(terminalName);
                        return columnParts.size() - 1;
                    });
                }
                traced.add(device);
                terminalColumns.add(own);
            }
        }
        this.devices = traced.stream().mapToInt(Integer::intValue).toArray();
        this.columns = terminalColumns.toArray(int[][]::new);
        this.deviceCurrents = terminalColumns.stream().map(own -> new double[own.length]).toArray(double[][]::new);
        this.stepCurrents = new double[columnParts.size()];
        this.window = new TraceWindow(nodes, columnParts.toArray(String[]::new), columnTerminals.toArray(String[]::new));
    }

    /** One accepted step, held over {@code duration} at {@code held}: every terminal column's current over it. */
    void take(double[] held, double[] companion, double at, double duration) {
        Arrays.fill(stepCurrents, 0);
        for (int index = 0; index < devices.length; index++) {
            double[] currents = deviceCurrents[index];
            parts.device(devices[index]).terminalCurrents(held, companion, at, currents);
            int[] own = columns[index];
            for (int terminal = 0; terminal < own.length; terminal++) {
                stepCurrents[own[terminal]] += currents[terminal];
            }
        }
        window.take(held, stepCurrents, duration);
    }

    /** How much circuit time it has gathered since it was last closed. */
    double duration() {
        return window.duration();
    }

    TraceWindow.Closed close() {
        return window.close();
    }

    void clear() {
        window.clear();
    }
}
