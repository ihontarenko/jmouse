package org.jmouse.parallax.circuit;

import org.jmouse.formula.Complex;

/**
 * A digital output on an analog node: a source that moves between the supply rails over the family's edge time and
 * holds the pin there through the family's output resistance, drawing what it sources from VCC and what it sinks into
 * GND. Its pin's capacitance is an ordinary capacitor beside it ({@link EventSimulation#pinCapacitances}), there when the
 * output is released (Z) too; an unknown one (X) sits at mid-rail.
 */
final class OutputBridge implements Device {

    /** The pull spans three terminals: the pin, VCC and GND. */
    private static final int PULL_TERMINALS = 3;

    private final DigitalDriver driver;
    private final int pin;
    private final int supply;
    private final int ground;
    /** The family whose output law it pulls by, and whose edge it moves over. */
    private final LogicFamily family;
    private final double edge;

    OutputBridge(DigitalDriver driver, int pin, int supply, int ground, LogicFamily family) {
        this.driver = driver;
        this.pin = pin;
        this.supply = supply;
        this.ground = ground;
        this.family = family;
        this.edge = family.edge();
    }

    /** Its pull follows the rails it is powered from, read from the guess. */
    @Override
    public boolean nonlinear() {
        return true;
    }

    @Override
    public void stamp(Assembly assembly) {
        if (driver.level() == LogicLevel.RELEASED) {
            return;
        }
        double share = shown(assembly.isTransient() ? assembly.time() : Double.POSITIVE_INFINITY);
        double conductance = conductance(assembly.voltage(supply) - assembly.voltage(ground));
        // The pin is pulled towards (1 − s)·GND + s·VCC: the current enters the pin and leaves VCC (share s) and GND.
        stampPull(assembly, conductance, share);
    }

    @Override
    public void stampPhasor(PhasorAssembly assembly) {
        if (driver.level() == LogicLevel.RELEASED) {
            return;
        }
        double share = driver.level().fraction();
        double conductance = conductance(operating(assembly, supply) - operating(assembly, ground));
        for (int row = 0; row < PULL_TERMINALS; row++) {
            for (int column = 0; column < PULL_TERMINALS; column++) {
                assembly.add(pullTerminal(row), pullTerminal(column),
                        Complex.of(pullWeight(row, share) * conductance * pullWeight(column, share)));
            }
        }
    }

    /**
     * How strongly it pulls across these rails: its family's {@code output_resistance(supply)} — one law, the family
     * document's, for the transient and the small signal alike.
     */
    private double conductance(double rails) {
        return 1 / family.outputResistance(rails);
    }

    private static double operating(PhasorAssembly assembly, int node) {
        return node == Netlist.GROUND ? 0 : assembly.operatingValue(node);
    }

    /** How far the output has moved from its previous level to its present one. */
    private double shown(double time) {
        double from = (driver.previous() == LogicLevel.RELEASED ? driver.level() : driver.previous()).fraction();
        double to = driver.level().fraction();
        double progress = Math.clamp((time - driver.changedAt()) / edge, 0, 1);
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
