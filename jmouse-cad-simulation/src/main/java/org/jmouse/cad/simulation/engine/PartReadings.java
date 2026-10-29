package org.jmouse.cad.simulation.engine;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What every part of a circuit does at one solved state — an operating point, a point of a sweep —, in the columns the
 * analyses report it in ({@link ColumnNames}): the current into each of its terminals, and the power it takes. Read
 * through each device's own report, so a resistor, a diode and a model are read alike; the solver's own leaks and a
 * failed part's stand-ins are no parts and are not read.
 */
final class PartReadings {

    private PartReadings() {
    }

    /** Every part's current at a solved state, by column ({@link ColumnNames#partCurrents}); a capacitance carries nothing at rest. */
    static Map<String, Double> currents(List<Device> devices, double[] state) {
        double[] atRest = new double[devices.size()];
        Map<String, Double> currents = new LinkedHashMap<>();
        for (Device device : devices) {
            if (device.part() == null) {
                continue;
            }
            ColumnNames.partCurrents(device.part(), device.terminalCurrents(state, atRest, 0)).forEach(currents::putIfAbsent);
        }
        return currents;
    }

    /** Every part's power at a solved state, watts, by column ({@link ColumnNames#partPower}): what its devices take, added up. */
    static Map<String, Double> powers(List<Device> devices, double[] state) {
        Map<String, Double> powers = new LinkedHashMap<>();
        for (Device device : devices) {
            if (device.part() != null) {
                powers.merge(ColumnNames.partPower(device.part()), device.power(state, 0), Double::sum);
            }
        }
        return powers;
    }
}
