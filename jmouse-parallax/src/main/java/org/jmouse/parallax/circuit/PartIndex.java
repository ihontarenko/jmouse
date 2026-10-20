package org.jmouse.parallax.circuit;

import org.jmouse.parallax.component.TemperatureScale;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The parts one piece of a circuit is made of, and which of its devices each one is — the one rule every reading of a
 * part goes by: a device belongs to the part it names ({@link Device#part}), and one that names none — a node's leak, a
 * failed part's stand-in, a logic pin's capacitance — belongs to no part and is never read. A part of several units
 * ({@code A1.a}, {@code A1.b}) is all of them, added up.
 */
final class PartIndex {

    private final List<Device> devices;
    /** The parts, in the order their first device was built. */
    private final String[]     parts;
    /** Each part's devices, by the part's place in {@link #parts}. */
    private final int[][]      devicesOfPart;
    /** Each device's part, by the device's place; -1 for none. */
    private final int[]        partOfDevice;

    PartIndex(List<Device> devices) {
        this.devices = List.copyOf(devices);
        Map<String, List<Integer>> owned = new LinkedHashMap<>();
        Map<String, Integer> places = new LinkedHashMap<>();
        this.partOfDevice = new int[devices.size()];
        for (int index = 0; index < devices.size(); index++) {
            String part = devices.get(index).part();
            if (part == null) {
                partOfDevice[index] = -1;
                continue;
            }
            owned.computeIfAbsent(part, ignored -> new ArrayList<>()).add(index);
            partOfDevice[index] = places.computeIfAbsent(part, ignored -> places.size());
        }
        this.parts = owned.keySet().toArray(String[]::new);
        this.devicesOfPart = owned.values().stream().map(own -> own.stream().mapToInt(Integer::intValue).toArray()).toArray(int[][]::new);
    }

    /** The parts, in the order their first device was built. */
    String[] parts() {
        return parts;
    }

    int count() {
        return parts.length;
    }

    /** The part a device is, by place among the devices; -1 for none. */
    int partOf(int device) {
        return partOfDevice[device];
    }

    /** The devices one part is made of, by place among the devices. */
    int[] devicesOf(int part) {
        return devicesOfPart[part];
    }

    Device device(int index) {
        return devices.get(index);
    }

    /** Whether a part is among those asked for: every part when none are named. */
    private static boolean asked(Set<String> only, String part) {
        return only == null || only.contains(part);
    }

    /** The current into each terminal of every part named — or of every part —, its devices on one terminal added up. */
    Map<String, Map<String, Double>> terminalCurrents(double[] state, double[] companion, double time, Set<String> only) {
        Map<String, Map<String, Double>> terminals = new LinkedHashMap<>();
        for (int part = 0; part < parts.length; part++) {
            if (asked(only, parts[part])) {
                terminals.put(parts[part], terminalCurrents(part, state, companion, time));
            }
        }
        return terminals;
    }

    /** The current into each of one part's terminals. */
    Map<String, Double> terminalCurrents(int part, double[] state, double[] companion, double time) {
        Map<String, Double> into = new LinkedHashMap<>();
        for (int device : devicesOfPart[part]) {
            devices.get(device).terminalCurrents(state, companion, time).forEach((terminal, current) -> into.merge(terminal, current, Double::sum));
        }
        return into;
    }

    /** The power every part named — or every part — takes at a state, its devices added up, watts. */
    Map<String, Double> powers(double[] state, double time, Set<String> only) {
        Map<String, Double> powers = new LinkedHashMap<>();
        for (int part = 0; part < parts.length; part++) {
            if (asked(only, parts[part])) {
                powers.put(parts[part], power(part, state, time));
            }
        }
        return powers;
    }

    /** The power one part takes at a state. */
    double power(int part, double[] state, double time) {
        double power = 0;
        for (int device : devicesOfPart[part]) {
            power += devices.get(device).power(state, time);
        }
        return power;
    }

    /** One part's states at a state, by name, its devices' together. */
    Map<String, Double> states(int part, double[] state) {
        Map<String, Double> states = new LinkedHashMap<>();
        for (int device : devicesOfPart[part]) {
            states.putAll(devices.get(device).stateValues(state));
        }
        return states;
    }

    /** The heat of every part that warms, by part: its first warming unit's for a part of several. */
    Map<String, PartHeat> heats() {
        Map<String, PartHeat> heats = new LinkedHashMap<>();
        for (int part = 0; part < parts.length; part++) {
            for (int device : devicesOfPart[part]) {
                PartHeat heat = devices.get(device).heat();
                if (heat != null) {
                    heats.putIfAbsent(parts[part], heat);
                }
            }
        }
        return heats;
    }

    /** How warm each part is now, °C — the hottest of its warming devices —, by place; NaN for one that does not warm. */
    double[] temperatures() {
        double[] temperatures = new double[parts.length];
        for (int part = 0; part < parts.length; part++) {
            double hottest = Double.NaN;
            for (int device : devicesOfPart[part]) {
                PartHeat heat = devices.get(device).heat();
                if (heat != null) {
                    double celsius = TemperatureScale.celsius(heat.temperature());
                    hottest = Double.isNaN(hottest) ? celsius : Math.max(hottest, celsius);
                }
            }
            temperatures[part] = hottest;
        }
        return temperatures;
    }
}
