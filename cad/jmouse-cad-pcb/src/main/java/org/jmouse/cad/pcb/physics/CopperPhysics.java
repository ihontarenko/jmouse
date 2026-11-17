package org.jmouse.cad.pcb.physics;

import org.jmouse.settings.Bounds;
import org.jmouse.settings.Label;
import org.jmouse.cad.settings.SettingsSection;
/**
 * What copper is, as the engine works out a pcb's copper as an electrical network: its resistivity and how it changes with
 * temperature, the temperature a pcb stands in, IPC-2221's current-carrying constants, and how finely the network is solved.
 * Data, a section of the settings tree; the copper's thickness is the pcb's way of making, not this.
 *
 * @param source                 where each value comes from: the standards and books it is read from
 * @param resistivity            Ω·m at the reference temperature
 * @param referenceTemperature   °C the resistivity is given at
 * @param temperatureCoefficient per kelvin
 * @param ambient                °C the pcb stands in: a resistance is given at it
 * @param capacity               IPC-2221's constants for the current a cross-section carries
 * @param carrying               the share of the current below which a piece is taken as carrying none of it
 * @param edgeHalvings           how many halvings find where a piece crosses an area's edge
 * @param solver                 how finely the copper's network is solved
 */
public record CopperPhysics(@Label("Source") String source,
                            @Label("Resistivity") double resistivity,
                            @Label("Reference temperature") double referenceTemperature,
                            @Label("Temperature coefficient") double temperatureCoefficient,
                            @Label("Ambient") double ambient,
                            @Label("Capacity") Capacity capacity,
                            @Bounds(minimum = 0, maximum = 1, step = 0.001) @Label("Least carried share") double carrying,
                            @Label("Edge halvings") int edgeHalvings,
                            @Label("Solver") Solver solver) {

    /** Its section of the settings tree (ADR 0004): its file and schema under org/jmouse/cad/settings/pcb/physics/copper. */
    public static final SettingsSection<CopperPhysics> SECTION = new SettingsSection<>("pcb/physics/copper", "Copper physics", CopperPhysics.class);

    /**
     * IPC-2221's I = k·ΔT^riseExponent·A^areaExponent, A in square mils.
     *
     * @param outer                         k on an outer layer
     * @param inner                         k on an inner layer
     * @param riseExponent                  the power the temperature rise is raised to
     * @param areaExponent                  the power the cross-section is raised to
     * @param squareMilsPerSquareMillimetre square mils in a square millimetre: the formula's unit of area
     */
    public record Capacity(@Label("Outer layer constant") double outer,
                           @Label("Inner layer constant") double inner,
                           @Label("Rise exponent") double riseExponent,
                           @Label("Area exponent") double areaExponent,
                           @Label("Square mils per square millimetre") double squareMilsPerSquareMillimetre) {
    }

    /**
     * How the nodal equations are solved: conjugate gradients until the residual is this small, at most this many
     * iterations for each node and these more.
     *
     * @param tolerance         the residual the conjugate gradients stop at
     * @param iterationsPerNode iterations allowed for each node of the network
     * @param iterations        iterations allowed beyond those
     */
    public record Solver(@Label("Tolerance") double tolerance,
                         @Label("Iterations per node") int iterationsPerNode,
                         @Label("Iterations") int iterations) {
    }

    /** Ω·mm at the ambient temperature. */
    public double resistivityAtAmbient() {
        return resistivity * 1000 * (1 + temperatureCoefficient * (ambient - referenceTemperature));
    }

    /** The current a cross-section, mm², carries within a rise, kelvin, by IPC-2221. */
    public double capacityOf(double section, double temperatureRise, boolean inner) {
        return factor(inner) * Math.pow(temperatureRise, capacity.riseExponent())
               * Math.pow(section * capacity.squareMilsPerSquareMillimetre(), capacity.areaExponent());
    }

    /** The cross-section, mm², that carries a current within a rise — IPC-2221 turned round. */
    public double sectionFor(double current, double temperatureRise, boolean inner) {
        double carried = factor(inner) * Math.pow(temperatureRise, capacity.riseExponent());
        return Math.pow(current / carried, 1 / capacity.areaExponent()) / capacity.squareMilsPerSquareMillimetre();
    }

    /** How many kelvin a cross-section warms by carrying a current, by IPC-2221. */
    public double riseOf(double current, double section, boolean inner) {
        double carried = factor(inner) * Math.pow(section * capacity.squareMilsPerSquareMillimetre(), capacity.areaExponent());
        return Math.pow(current / carried, 1 / capacity.riseExponent());
    }

    private double factor(boolean inner) {
        return inner ? capacity.inner() : capacity.outer();
    }
}
