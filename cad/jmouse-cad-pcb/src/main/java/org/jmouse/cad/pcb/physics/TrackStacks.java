package org.jmouse.cad.pcb.physics;

import org.jmouse.cad.pcb.PcbLayers;
import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.pcb.drawing.PcbDrawing;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What a pcb's stack means for the signals and the heat on it: the impedance of a track over its reference plane, the width
 * that gives an impedance, the impedance of a pair, and how warm the whole pcb gets from the watts spent on it.
 *
 * <p>⚠️ These are closed formulas, not a field solver, and the plane is assumed: a track on an outer layer is a microstrip
 * over a whole plane on the next layer, a track on an inner layer a stripline centred between planes. The layers are taken
 * as evenly spaced through the pcb's thickness until the reader says the real height: a house's 4-layer stack is not even,
 * so a controlled impedance is still confirmed with the house. The warmth is one figure for the whole pcb.</p>
 */
public final class TrackStacks {

    /**
     * What a track's impedance depends on but its width.
     *
     * @param height       the dielectric between it and its plane, millimetres
     * @param thickness    its copper's thickness, millimetres
     * @param permittivity the dielectric's relative permittivity
     */
    public record Stack(StackPhysics.TrackLine line, double height, double thickness, double permittivity) {
    }

    /**
     * @param effectivePermittivity what the wave sees, part in air and part in the dielectric
     * @param delay                 picoseconds per millimetre of track
     */
    public record Impedance(double impedance, double effectivePermittivity, double delay) {
    }

    /** A layer's stack, or why a track there has no plane to stand over, in words. */
    public sealed interface Standing permits Stands, Unplaned {
    }

    public record Stands(Stack stack) implements Standing {
    }

    public record Unplaned(String refusal) implements Standing {
    }

    private final StackPhysics physics;

    public TrackStacks(StackPhysics physics) {
        this.physics = physics;
    }

    public StackPhysics physics() {
        return physics;
    }

    /**
     * A layer's stack as the pcb's way of making gives it.
     *
     * @param values       the values the pcb is held to, by rule word
     * @param height       the reader's own height to the plane, where they say it
     * @param permittivity the reader's own permittivity, where they say it; the physics' otherwise
     */
    public Standing stackOf(List<String> copperLayers, Map<String, Double> values, String layer, Optional<Double> height,
                            Optional<Double> permittivity) {
        if (copperLayers.size() < 2) {
            return new Unplaned("A one-layer pcb has no plane under its tracks: their impedance is not set by the stack.");
        }
        boolean inner = copperLayers.contains(layer) && PcbLayers.isInnerCopper(layer);
        double evenly = values.get(PcbVocabulary.RULE_PCB_THICKNESS) / (copperLayers.size() - 1);
        double thickness = values.get(inner ? PcbVocabulary.RULE_INNER_COPPER_THICKNESS : PcbVocabulary.RULE_COPPER_THICKNESS);
        return new Stands(new Stack(inner ? StackPhysics.TrackLine.STRIPLINE : StackPhysics.TrackLine.MICROSTRIP, height.orElse(evenly),
                thickness, permittivity.orElse(physics.permittivity())));
    }

    /** A track's impedance over its plane; widths are in millimetres. */
    public Impedance impedanceOf(double width, Stack stack) {
        double[] answer = stack.line() == StackPhysics.TrackLine.MICROSTRIP ? microstrip(width, stack) : stripline(width, stack);
        return new Impedance(answer[0], answer[1], 1000 * Math.sqrt(answer[1]) / physics.speedOfLight());
    }

    /** The width that gives an impedance — a wider track is always a lower one, so it is bisected —; empty when none searched does. */
    public Optional<Double> widthFor(double impedance, Stack stack) {
        StackPhysics.Search search = physics.searchWidths();
        double narrow = search.least();
        double wide = search.most();
        if (impedanceOf(narrow, stack).impedance() < impedance || impedanceOf(wide, stack).impedance() > impedance) {
            return Optional.empty();
        }
        while (wide - narrow > search.precision()) {
            double middle = (narrow + wide) / 2;
            if (impedanceOf(middle, stack).impedance() > impedance) {
                narrow = middle;
            } else {
                wide = middle;
            }
        }
        return Optional.of((narrow + wide) / 2);
    }

    /** A pair's differential impedance, ohms, from one of its tracks' width and the gap between them. */
    public double pairImpedance(double width, double gap, Stack stack) {
        StackPhysics.Coupling coupling = physics.couplingOf(stack.line());
        double reach = stack.line() == StackPhysics.TrackLine.MICROSTRIP ? stack.height() : 2 * stack.height() + stack.thickness();
        return 2 * impedanceOf(width, stack).impedance() * (1 - coupling.share() * Math.exp(-coupling.decay() * gap / reach));
    }

    /** How many kelvin a pcb of this size, millimetres, warms by from the watts spent on it: both faces give them off, the edges too little to count. */
    public double warmth(double power, double width, double height) {
        double squareMetres = 2 * width * height * 1e-6;
        return squareMetres > 0 ? power / (physics.surfaceCoefficient() * squareMetres) : 0;
    }

    /** Hammerstad and Jensen, with the copper's thickness: accurate to about 1 % over what a pcb is made with. */
    private double[] microstrip(double width, Stack stack) {
        double ratio = width / stack.height();
        double relativeThickness = stack.thickness() / stack.height();
        double permittivity = stack.permittivity();
        double widened = 0;
        if (relativeThickness > 0) {
            double cotangent = 1 / Math.tanh(Math.sqrt(6.517 * ratio));
            widened = relativeThickness / Math.PI * Math.log(1 + 4 * Math.E / (relativeThickness * cotangent * cotangent));
        }
        double inAir = ratio + widened;
        double inDielectric = ratio + 0.5 * (1 + 1 / Math.cosh(Math.sqrt(permittivity - 1))) * widened;
        double effective = filled(inDielectric, permittivity) * Math.pow(air(inAir) / air(inDielectric), 2);
        return new double[] {air(inDielectric) / Math.sqrt(filled(inDielectric, permittivity)), effective};
    }

    private double air(double share) {
        double shape = 6 + (2 * Math.PI - 6) * Math.exp(-Math.pow(30.666 / share, 0.7528));
        return physics.freeSpace() / (2 * Math.PI) * Math.log(shape / share + Math.sqrt(1 + Math.pow(2 / share, 2)));
    }

    private static double filled(double share, double permittivity) {
        double first = 1 + Math.log((Math.pow(share, 4) + Math.pow(share / 52, 2)) / (Math.pow(share, 4) + 0.432)) / 49
                + Math.log(1 + Math.pow(share / 18.1, 3)) / 18.7;
        double second = 0.564 * Math.pow((permittivity - 0.9) / (permittivity + 3), 0.053);
        return (permittivity + 1) / 2 + (permittivity - 1) / 2 * Math.pow(1 + 10 / share, -first * second);
    }

    /** Wheeler's, between planes {@code 2 · height + thickness} apart; a stripline's field is all in the dielectric. */
    private static double[] stripline(double width, Stack stack) {
        double between = 2 * stack.height() + stack.thickness();
        double ratio = width / between;
        double effective = ratio > 0.35 ? ratio : ratio - Math.pow(0.35 - ratio, 2);
        return new double[] {30 * Math.PI / Math.sqrt(stack.permittivity()) / (effective + 0.441), stack.permittivity()};
    }
}
