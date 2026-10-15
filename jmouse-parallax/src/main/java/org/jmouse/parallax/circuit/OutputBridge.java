package org.jmouse.parallax.circuit;

import org.jmouse.formula.Complex;

/**
 * A digital output on an analog node: a source that moves between the supply rails over the family's edge time and
 * holds the pin there through the family's output resistance, drawing what it sources from VCC and what it sinks into
 * GND. A released output (Z) still has its pin capacitance; an unknown one (X) sits at mid-rail.
 */
final class OutputBridge implements Device {

    /** The pull spans three terminals: the pin, VCC and GND. */
    private static final int PULL_TERMINALS = 3;

    private final DigitalDriver driver;
    private final int pin;
    private final int supply;
    private final int ground;
    private final double resistance;
    private final double referenceSupply;
    private final double edge;
    private final double capacitance;
    private int slot = -1;

    OutputBridge(DigitalDriver driver, int pin, int supply, int ground, double resistance, double referenceSupply, double edge, double capacitance) {
        this.driver = driver;
        this.pin = pin;
        this.supply = supply;
        this.ground = ground;
        this.resistance = resistance;
        this.referenceSupply = referenceSupply;
        this.edge = edge;
        this.capacitance = capacitance;
    }

    void slot(int slot) {
        this.slot = slot;
    }

    /** Its pull follows the rails it is powered from, read from the guess. */
    @Override
    public boolean nonlinear() {
        return true;
    }

    @Override
    public void stamp(Assembly assembly) {
        if (capacitance > 0 && assembly.isTransient()) {
            double conductance = capacitiveConductance(assembly);
            assembly.conductance(pin, ground, conductance);
            assembly.current(pin, ground, -history(assembly, conductance));
        }
        if (driver.level.equals(DigitalDriver.RELEASED)) {
            return;
        }
        double share = shown(assembly.isTransient() ? assembly.time() : Double.POSITIVE_INFINITY);
        double rails = Math.max(assembly.voltage(supply) - assembly.voltage(ground), 0.5);
        double conductance = 1 / (resistance * Math.pow(referenceSupply / rails, 1.5));
        // The pin is pulled towards (1 − s)·GND + s·VCC: the current enters the pin and leaves VCC (share s) and GND.
        stampPull(assembly, conductance, share);
    }

    @Override
    public void accept(Assembly assembly, double[] state) {
        if (capacitance > 0) {
            double conductance = capacitiveConductance(assembly);
            double across = (pin == Netlist.GROUND ? 0 : state[pin])
                    - (ground == Netlist.GROUND ? 0 : state[ground]);
            assembly.rememberCompanion(slot, conductance * across - history(assembly, conductance));
        }
    }

    /** The pin capacitance's companion conductance over the assembly's step. */
    private double capacitiveConductance(Assembly assembly) {
        double conductance = capacitance / assembly.step();
        if (assembly.integration() == Assembly.Integration.TRAPEZOIDAL) {
            conductance *= 2;
        }
        return conductance;
    }

    private double history(Assembly assembly, double conductance) {
        double previous = assembly.previousVoltage(pin) - assembly.previousVoltage(ground);
        double value = conductance * previous;
        return assembly.integration() == Assembly.Integration.TRAPEZOIDAL
                ? value + assembly.companion(slot) : value;
    }

    @Override
    public void stampPhasor(PhasorAssembly assembly) {
        if (capacitance > 0) {
            assembly.admittance(pin, ground, new Complex(0, assembly.angular() * capacitance));
        }
        if (driver.level.equals(DigitalDriver.RELEASED)) {
            return;
        }
        double share = DigitalDriver.fraction(driver.level);
        double conductance = 1 / resistance;
        for (int row = 0; row < PULL_TERMINALS; row++) {
            for (int column = 0; column < PULL_TERMINALS; column++) {
                assembly.add(pullTerminal(row), pullTerminal(column),
                        Complex.of(pullWeight(row, share) * conductance * pullWeight(column, share)));
            }
        }
    }

    /** How far the output has moved from its previous level to its present one. */
    private double shown(double time) {
        double from = DigitalDriver.fraction(driver.previous.equals(DigitalDriver.RELEASED) ? driver.level : driver.previous);
        double to = DigitalDriver.fraction(driver.level);
        double progress = Math.clamp((time - driver.changedAt) / edge, 0, 1);
        return from + (to - from) * progress;
    }

    /**
     * I = G (s·V(vcc) + (1 − s)·V(gnd) − V(pin)) enters the pin; s of it leaves VCC and 1 − s leaves GND. Each row's
     * "leaving" coefficient is written out. Stamped in every Newton iteration, so without arrays of its own.
     */
    private void stampPull(Assembly assembly, double conductance, double share) {
        for (int row = 0; row < PULL_TERMINALS; row++) {
            for (int column = 0; column < PULL_TERMINALS; column++) {
                assembly.add(pullTerminal(row), pullTerminal(column), pullWeight(row, share) * conductance * pullWeight(column, share));
            }
        }
    }

    /** The pull's terminals in order: the pin, VCC, GND. */
    private int pullTerminal(int index) {
        return switch (index) {
            case 0 -> pin;
            case 1 -> supply;
            default -> ground;
        };
    }

    /** Each terminal's share of the pull: the pin's whole, VCC's s, GND's 1 − s. */
    private static double pullWeight(int index, double share) {
        return switch (index) {
            case 0 -> -1;
            case 1 -> share;
            default -> 1 - share;
        };
    }
}
