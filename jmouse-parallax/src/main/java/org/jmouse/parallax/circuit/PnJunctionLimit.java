package org.jmouse.parallax.circuit;

/**
 * SPICE's {@code pnjlim} for a p-n junction, whose current is an exponential of its voltage: above the critical
 * voltage — where the curve turns steep — a step longer than two thermal voltages is taken on the logarithm of the
 * current instead of on the voltage.
 *
 * @param thermalVoltage the junction's thermal voltage times its emission coefficient, volts
 * @param critical       where the exponential turns steep: SPICE's {@code vcrit}, volts
 */
record PnJunctionLimit(double thermalVoltage, double critical) implements VoltageLimit {

    /** The limit of a junction with this saturation current, at this thermal voltage. */
    static PnJunctionLimit of(double saturation, double thermalVoltage) {
        return new PnJunctionLimit(thermalVoltage, thermalVoltage * Math.log(thermalVoltage / (Math.sqrt(2) * saturation)));
    }

    @Override
    public double limited(double wanted, double previous) {
        if (Double.isNaN(previous) || wanted <= critical || Math.abs(wanted - previous) <= 2 * thermalVoltage) {
            return wanted;
        }
        if (previous > 0) {
            double argument = 1 + (wanted - previous) / thermalVoltage;
            return argument > 0 ? previous + thermalVoltage * Math.log(argument) : critical;
        }
        return thermalVoltage * Math.log(wanted / thermalVoltage);
    }
}
