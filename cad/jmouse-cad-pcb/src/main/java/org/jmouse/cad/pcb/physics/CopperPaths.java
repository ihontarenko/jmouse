package org.jmouse.cad.pcb.physics;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.PcbLayers;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.geometry.CopperNets;
import org.jmouse.core.graph.DisjointSets;
import org.jmouse.numerics.ConductanceNetwork;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The pcb's copper as an electrical network: what two points of it are apart in ohms, how a current between them spreads
 * through the copper, how warm it makes each piece, and which piece is too thin for it.
 *
 * <p>Every straight piece of a track or of copper drawn as lines, every arc and every via barrel between two layers is a
 * resistor, ρ·length / section, at the ambient temperature; a pad, a poured zone and an area of copper drawn filled are
 * taken as perfect conductors — a pad is short and wide, a zone's spreading resistance is not worked out, and the answer
 * says when one is on the way. Copper joins where it touches, as the nets are joined ({@link CopperNets}): a track ending
 * on a pad, branching off another's middle, crossing a via. The network is solved for one ampere (conjugate gradients over
 * the nodal equations), then scaled to the current asked. How much a piece carries for an allowed rise is IPC-2221's
 * ({@link CopperPhysics}); the copper's thickness is the pcb's way of making.</p>
 */
public final class CopperPaths {

    /**
     * A point asked about: where it stands, on which copper layer — empty for any —, and how far off copper it may stand and
     * still be on it. It is on the one piece of copper nearest it — a pad before a track over it —, never on two, so a
     * generous reach cannot join neighbouring nets; on a zone only where nothing else is.
     */
    public record Probe(double x, double y, Optional<String> layer, double reach) {
    }

    /** What a resistor of the network is of. */
    public enum Thing {
        TRACK, ARC, SHAPE, VIA;

        /** The word an answer says it with: {@code track}. */
        public String word() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * One resistor: a straight piece of a track or of copper drawn as lines, an arc, or a via's barrel between two
     * neighbouring layers.
     *
     * @param index      the track's, arc's, shape's or via's place in the pcb
     * @param layer      the copper layer; a barrel's upper one
     * @param length     millimetres along the current
     * @param width      the copper's width across the current, millimetres: a barrel's is its circumference
     * @param thickness  the copper's thickness, millimetres
     * @param resistance ohms, at the ambient temperature
     * @param capacity   amperes it carries within the allowed rise
     */
    public record Branch(Thing thing, int index, String layer, Point from, Point to, double length, double width,
                         double thickness, double resistance, double capacity) {
    }

    /**
     * What the current asked about does in one branch.
     *
     * @param current     amperes
     * @param power       watts it warms the pcb with
     * @param rise        kelvin it warms by, by IPC-2221
     * @param neededWidth for a track or an arc too thin: the width that would carry the current
     * @param neededVias  for a via too thin: how many like it, side by side, would
     */
    public record Flow(Branch branch, double current, double power, double rise, boolean overloaded, Optional<Double> neededWidth,
                       Optional<Integer> neededVias) {
    }

    /**
     * The answer between two points.
     *
     * @param resistance ohms, at the ambient temperature
     * @param drop       volts lost between the points at the current
     * @param power      watts the copper turns into heat
     * @param flows      the branches the current flows through, the most loaded — current against capacity — first
     * @param pours      the zones it may pass through, taken as perfect conductors: the true resistance is somewhat higher
     */
    public record Path(double resistance, double current, double temperatureRise, double drop, double power, List<Flow> flows, List<Integer> pours) {

        /** The most loaded branch: where the copper should be strengthened first. */
        public Optional<Flow> weakest() {
            return flows.stream().findFirst();
        }
    }

    /** A path between two points, or why there is none, in words. */
    public sealed interface Answer permits Found, Refused {
    }

    public record Found(Path path) implements Answer {
    }

    public record Refused(String refusal) implements Answer {
    }

    private CopperPaths() {
    }

    /**
     * The copper between two points: its resistance, the drop and the heat at a current, and, piece by piece, where that
     * current flows and which piece is too thin for it within the allowed rise. Refused — with words — when a point is on no
     * copper, or when no copper joins them.
     *
     * @param values the values the pcb is held to, by rule word: the copper's, the plating's and the pcb's thicknesses
     */
    public static Answer between(PcbDrawing pcb, CopperNets nets, Map<String, Double> values, Probe from, Probe to, double current,
                                 double temperatureRise, CopperPhysics physics, int curveSteps) {
        CopperNetwork network = CopperNetwork.of(pcb, nets, values, physics, curveSteps, temperatureRise, List.of(from, to));
        List<Optional<Integer>> probed = network.probeNodes();
        if (probed.get(0).isEmpty()) {
            return new Refused("The first point is on no copper.");
        }
        if (probed.get(1).isEmpty()) {
            return new Refused("The second point is on no copper.");
        }
        DisjointSets nodes = network.nodes();
        int sourceRoot = nodes.rootOf(probed.get(0).get());
        int sinkRoot = nodes.rootOf(probed.get(1).get());
        // What copper reaches from the source: the conductors the resistors join.
        DisjointSets joined = new DisjointSets(nodes.size());
        for (CopperNetwork.Resistor resistor : network.resistors()) {
            joined.join(nodes.rootOf(resistor.from()), nodes.rootOf(resistor.to()));
        }
        if (!joined.together(sourceRoot, sinkRoot)) {
            return new Refused("No copper joins the two points.");
        }
        List<Integer> pours = new ArrayList<>();
        for (int zone = 0; zone < network.zoneNodes().size(); zone++) {
            if (joined.together(nodes.rootOf(network.zoneNodes().get(zone)), sourceRoot)) {
                pours.add(zone);
            }
        }
        if (sourceRoot == sinkRoot) {
            return new Found(new Path(0, current, temperatureRise, 0, 0, List.of(), pours));
        }
        // The conductors the source reaches, numbered as the solver's nodes, and the resistors between two of them.
        Map<Integer, Integer> numbers = new LinkedHashMap<>();
        numbers.put(sourceRoot, 0);
        List<CopperNetwork.Resistor> inside = new ArrayList<>();
        for (CopperNetwork.Resistor resistor : network.resistors()) {
            int one = nodes.rootOf(resistor.from());
            int other = nodes.rootOf(resistor.to());
            if (one != other && joined.together(one, sourceRoot)) {
                numbers.putIfAbsent(one, numbers.size());
                numbers.putIfAbsent(other, numbers.size());
                inside.add(resistor);
            }
        }
        ConductanceNetwork conductances = new ConductanceNetwork(numbers.size());
        for (CopperNetwork.Resistor resistor : inside) {
            conductances.connect(numbers.get(nodes.rootOf(resistor.from())), numbers.get(nodes.rootOf(resistor.to())), 1 / resistor.branch().resistance());
        }
        CopperPhysics.Solver solver = physics.solver();
        double[] solved = conductances.potentials(numbers.get(sourceRoot), numbers.get(sinkRoot),
                new ConductanceNetwork.Stopping(solver.tolerance(), solver.iterationsPerNode(), solver.iterations()));
        double resistance = solved[numbers.get(sourceRoot)];
        List<Flow> flows = new ArrayList<>();
        for (CopperNetwork.Resistor resistor : inside) {
            Branch branch = resistor.branch();
            double share = Math.abs(solved[numbers.get(nodes.rootOf(resistor.from()))] - solved[numbers.get(nodes.rootOf(resistor.to()))]) / branch.resistance();
            if (share < physics.carrying()) {
                continue;
            }
            flows.add(flowOf(branch, share * current, temperatureRise, physics));
        }
        flows.sort(Comparator.comparingDouble((Flow flow) -> flow.current() / flow.branch().capacity()).reversed());
        return new Found(new Path(resistance, current, temperatureRise, current * resistance, current * current * resistance, flows, pours));
    }

    /** What a current does in a branch: the heat, the rise, and — where it is too much — what would carry it. */
    private static Flow flowOf(Branch branch, double flowing, double temperatureRise, CopperPhysics physics) {
        double section = branch.width() * branch.thickness();
        boolean inner = branch.thing() != Thing.VIA && PcbLayers.isInnerCopper(branch.layer());
        boolean overloaded = flowing > branch.capacity();
        return new Flow(branch, flowing, flowing * flowing * branch.resistance(), physics.riseOf(flowing, section, inner), overloaded,
                overloaded && branch.thing() != Thing.VIA ? Optional.of(physics.sectionFor(flowing, temperatureRise, inner) / branch.thickness()) : Optional.empty(),
                overloaded && branch.thing() == Thing.VIA ? Optional.of((int) Math.ceil(flowing / branch.capacity())) : Optional.empty());
    }
}
