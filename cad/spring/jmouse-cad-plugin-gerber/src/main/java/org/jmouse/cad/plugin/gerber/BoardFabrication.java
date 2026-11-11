package org.jmouse.cad.plugin.gerber;

import org.jmouse.cad.board.drawing.BoardDrawing;
import org.jmouse.cad.board.drawing.FootprintDrawing;
import org.jmouse.cad.board.geometry.CopperNets;
import org.jmouse.cad.board.geometry.PadOutline;
import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.circuit.Part;
import org.jmouse.cad.Setting;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.cad.simulation.ComponentFacts;
import org.jmouse.cad.simulation.EvaluatedParameter;
import org.jmouse.cad.simulation.ParameterEvaluation;
import org.jmouse.cad.simulation.SimulationModule;
import org.jmouse.cad.simulation.SimulationReading;
import org.jmouse.cad.Formula;
import org.jmouse.units.Quantity;
import org.jmouse.units.Units;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A board as what its fabrication files are written from: the pcb, the circuit it lays out, each seated part with its
 * value, its footprint and the footprint's drawing. Read once from the documents; the plain set ({@link GerberSet}) and a
 * board house's own are both written from it.
 */
public final class BoardFabrication {

    /** Designators in the order a person reads them: by letters, then by number — R2 before R10. */
    public static final Comparator<String> BY_DESIGNATOR = BoardFabrication::compareDesignators;

    private final String                        name;
    private final BoardDrawing                  board;
    private final ComponentFacts                facts;
    private final List<SeatedPart>              parts;
    private final List<String>                  notes;

    /**
     * A part as it stands on the board.
     *
     * @param value     its value as a person reads it — {@code 10 kΩ} —, else its component's slug
     * @param footprint the footprint's slug; empty when neither the part nor its component names one
     * @param drawing   the footprint's drawing; empty when it cannot be read
     * @param padNets   the net each of its pads carries, by pad number, as the circuit wires it
     */
    public record SeatedPart(BoardDrawing.Placement placement, String component, String value, Optional<String> footprint,
                             Optional<FootprintDrawing> drawing, Map<String, String> padNets) {

        public String designator() {
            return placement.designator();
        }
    }

    private BoardFabrication(String name, BoardDrawing board, ComponentFacts facts, List<SeatedPart> parts, List<String> notes) {
        this.name = name;
        this.board = board;
        this.facts = facts;
        this.parts = List.copyOf(parts);
        this.notes = List.copyOf(notes);
    }

    /**
     * @param pcb       the board's document
     * @param catalogue the documents it names — its circuit, the components, the footprints —, as the reader may see them
     * @throws IllegalArgumentException when its circuit cannot be read
     */
    public static BoardFabrication of(DocumentNode pcb, CatalogueDocuments catalogue) {
        BoardDrawing board = BoardDrawing.of(pcb);
        DocumentNode circuitDocument = catalogue.find(board.circuit())
                .filter(found -> found.kind().equals(SimulationModule.CIRCUIT))
                .orElseThrow(() -> new IllegalArgumentException("The board lays out '" + board.circuit()
                        + "', which is no circuit the reader may see."));
        Circuit circuit = SimulationReading.circuit(circuitDocument);
        Map<String, Double> parameters = new LinkedHashMap<>();
        ParameterEvaluation.evaluate(circuit.head().slug(), circuit.parameters())
                .forEach(parameter -> parameters.put(parameter.name(), parameter.value()));
        ComponentFacts facts = new ComponentFacts(catalogue);
        Map<String, Part> byReference = new LinkedHashMap<>();
        circuit.parts().forEach(part -> byReference.put(part.reference(), part));
        List<SeatedPart> seated = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        for (BoardDrawing.Placement placement : board.placements()) {
            Part part = byReference.get(placement.designator());
            if (part == null) {
                notes.add(placement.designator() + " stands on the board but is no part of " + board.circuit() + "; left out.");
                continue;
            }
            Optional<String> footprint = Optional.ofNullable(part.footprint()).filter(slug -> !slug.isBlank())
                    .or(() -> facts.footprintOf(part.component()));
            Optional<FootprintDrawing> drawing = footprint.flatMap(catalogue::find).map(FootprintDrawing::of);
            if (footprint.isPresent() && drawing.isEmpty()) {
                notes.add(placement.designator() + "'s footprint " + footprint.get() + " cannot be read; its holes are not drilled.");
            }
            seated.add(new SeatedPart(placement, part.component(), valueOf(part, facts, parameters), footprint, drawing,
                    facts.padNets(part, footprint)));
        }
        seated.sort(Comparator.comparing(SeatedPart::designator, BY_DESIGNATOR));
        return new BoardFabrication(pcb.slug(), board, facts, seated, notes);
    }

    /** What the files are named after: the pcb's slug. */
    public String name() {
        return name;
    }

    public BoardDrawing board() {
        return board;
    }

    /** What the documents say about the components the parts are: a house reads its supplier's numbers here. */
    public ComponentFacts facts() {
        return facts;
    }

    /** The parts seated on the board, in designator order. */
    public List<SeatedPart> parts() {
        return parts;
    }

    /** What the board could not be told whole: a part it stands that the circuit lacks, a footprint not read. */
    public List<String> notes() {
        return notes;
    }

    /**
     * Which copper is on which net: each pad's as the circuit wires it, each track's, arc's and via's as the pads it
     * reaches are.
     */
    public CopperNets copperNets(PadOutline outlines, int curveSteps) {
        Map<String, FootprintDrawing> footprints = new LinkedHashMap<>();
        Map<String, Map<String, String>> nets = new LinkedHashMap<>();
        for (SeatedPart part : parts) {
            part.drawing().ifPresent(drawing -> footprints.put(part.designator(), drawing));
            nets.put(part.designator(), part.padNets());
        }
        return CopperNets.of(board, footprints, nets, outlines, curveSteps);
    }

    /**
     * A part's value as a person reads it: its value parameter's setting worked out over the circuit's parameters and
     * said with the parameter's unit ({@code 10 kΩ}), its component's default when the part sets none, else the slug.
     */
    private static String valueOf(Part part, ComponentFacts facts, Map<String, Double> parameters) {
        Optional<String> parameterName = facts.valueParameterOf(part.component());
        if (parameterName.isEmpty()) {
            return part.component();
        }
        Optional<EvaluatedParameter> declared = facts.compiled(part.component()).flatMap(compiled -> compiled.parameters()
                .stream().filter(parameter -> parameter.name().equals(parameterName.get())).findFirst());
        String unit = declared.map(EvaluatedParameter::unit).orElse("");
        Optional<Double> set = part.setting(parameterName.get()).map(Setting::value)
                .filter(Formula.class::isInstance).map(Formula.class::cast).flatMap(formula -> {
                    try {
                        return Optional.of(ParameterEvaluation.value(part.reference() + "." + parameterName.get(), formula,
                                parameters));
                    } catch (IllegalArgumentException uncomputed) {
                        return Optional.empty();
                    }
                });
        return set.or(() -> declared.map(EvaluatedParameter::value))
                .map(value -> said(value, unit))
                .orElse(part.component());
    }

    /** A value as a reader sees it: prefixed, its unit in the reader's symbols — {@code 10 kΩ}, {@code 15.9 nF}. */
    private static String said(double value, String unit) {
        if (unit == null || unit.isBlank()) {
            return Quantity.plain(value).written();
        }
        Quantity quantity = Quantity.prefixed(value, unit);
        String written = quantity.writtenUnit();
        return written.isEmpty() ? quantity.written()
                : quantity.written().substring(0, quantity.written().length() - written.length()) + Units.shown(written);
    }

    private static int compareDesignators(String first, String second) {
        String firstLetters = first.replaceAll("\\d.*$", "");
        String secondLetters = second.replaceAll("\\d.*$", "");
        int letters = firstLetters.compareTo(secondLetters);
        if (letters != 0) {
            return letters;
        }
        String firstNumber = first.substring(firstLetters.length()).replaceAll("\\D.*$", "");
        String secondNumber = second.substring(secondLetters.length()).replaceAll("\\D.*$", "");
        if (!firstNumber.isEmpty() && !secondNumber.isEmpty()) {
            int numbers = Long.compare(Long.parseLong(firstNumber), Long.parseLong(secondNumber));
            if (numbers != 0) {
                return numbers;
            }
        }
        return first.compareTo(second);
    }
}
