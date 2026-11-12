package org.jmouse.cad.exchange.pcb;

import org.jmouse.cad.pcb.checks.PcbFinding;
import org.jmouse.cad.pcb.checks.PcbChecks;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.cad.pcb.geometry.CopperNets;
import org.jmouse.cad.pcb.geometry.PadOutline;
import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.circuit.Part;
import org.jmouse.cad.Setting;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.cad.simulation.elaboration.SheetUnfolding;
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
 * A pcb as what its fabrication files are written from: the pcb, the circuit it lays out, each seated part with its
 * value, its footprint and the footprint's drawing. Read once from the documents; the plain set ({@link GerberSet}) and a
 * pcb house's own are both written from it.
 */
public final class PcbFabrication {

    /** Designators in the order a person reads them: by letters, then by number — R2 before R10. */
    public static final Comparator<String> BY_DESIGNATOR = PcbFabrication::compareDesignators;

    private final String                        name;
    private final PcbDrawing                  pcb;
    private final ComponentFacts                facts;
    private final List<SeatedPart>              parts;
    private final List<String>                  notes;

    /**
     * A part as it stands on the pcb.
     *
     * @param value     its value as a person reads it — {@code 10 kΩ} —, else its component's slug
     * @param footprint the footprint's slug; empty when neither the part nor its component names one
     * @param drawing   the footprint's drawing; empty when it cannot be read
     * @param padNets   the net each of its pads carries, by pad number, as the circuit wires it
     */
    public record SeatedPart(PcbDrawing.Placement placement, String component, String value, Optional<String> footprint,
                             Optional<FootprintDrawing> drawing, Map<String, String> padNets) {

        public String designator() {
            return placement.designator();
        }

        /** What is printed on the pcb and listed for a person. */
        public String printed() {
            return placement.printed();
        }

        /**
         * Whether there is nothing to buy or to place for it, drilled, etched and printed all the same: it is only the
         * pcb's, or a footprint of no pads — a mark printed on the board.
         */
        public boolean nothingToMount() {
            return placement.ownedByPcb() || drawing.map(found -> found.pads().isEmpty()).orElse(false);
        }

        /** Whether any of its pads goes through a hole: it is mounted through the pcb, not on its surface. */
        public boolean throughHole() {
            return drawing.stream().flatMap(found -> found.pads().stream()).anyMatch(FootprintDrawing.Pad::isPlatedThrough);
        }
    }

    private PcbFabrication(String name, PcbDrawing pcb, ComponentFacts facts, List<SeatedPart> parts, List<String> notes) {
        this.name = name;
        this.pcb = pcb;
        this.facts = facts;
        this.parts = List.copyOf(parts);
        this.notes = List.copyOf(notes);
    }

    /**
     * @param document  the pcb's document
     * @param catalogue the documents it names — its circuit, the components, the footprints —, as the reader may see them
     * @throws IllegalArgumentException when its circuit cannot be read
     */
    public static PcbFabrication of(DocumentNode document, CatalogueDocuments catalogue) {
        PcbDrawing pcb = PcbDrawing.of(document);
        Map<String, Double> parameters = new LinkedHashMap<>();
        Map<String, Part> byReference = new LinkedHashMap<>();
        // A pcb that lays out no circuit — a board of its own parts alone: a logo, a panel's frame, a footprint shown on a
        // piece of pcb — seats what it places itself and nothing else.
        if (!pcb.circuit().isBlank()) {
            DocumentNode circuitDocument = catalogue.find(pcb.circuit())
                    .filter(found -> found.kind().equals(SimulationModule.CIRCUIT))
                    .orElseThrow(() -> new IllegalArgumentException("The pcb lays out '" + pcb.circuit()
                            + "', which is no circuit the reader may see."));
            // A pcb lays out the design whole: the parts of the sheets it places, as a bench runs them.
            Circuit circuit;
            try {
                circuit = SheetUnfolding.of(SimulationReading.circuit(circuitDocument), catalogue);
            } catch (IllegalArgumentException unreadable) {
                throw new IllegalArgumentException("The pcb lays out '" + pcb.circuit() + "', which cannot be unfolded: "
                        + unreadable.getMessage(), unreadable);
            }
            ParameterEvaluation.evaluate(circuit.head().slug(), circuit.parameters())
                    .forEach(parameter -> parameters.put(parameter.name(), parameter.value()));
            circuit.parts().forEach(part -> byReference.put(part.reference(), part));
        }
        ComponentFacts facts = new ComponentFacts(catalogue);
        List<SeatedPart> seated = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        for (PcbDrawing.Placement placement : pcb.placements()) {
            if (placement.ownedByPcb()) {
                seated.add(ownedByPcb(placement, catalogue, notes));
                continue;
            }
            Part part = byReference.get(placement.designator());
            if (part == null) {
                notes.add(placement.designator() + (pcb.circuit().isBlank() ? " stands on a pcb that lays out no circuit and names no footprint of its own"
                        : " stands on the pcb but is no part of " + pcb.circuit()) + "; left out.");
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
        return new PcbFabrication(document.slug(), pcb, facts, seated, notes);
    }

    /** A part only the pcb has, on the footprint it names itself, its pads on the nets it says. */
    private static SeatedPart ownedByPcb(PcbDrawing.Placement placement, CatalogueDocuments catalogue, List<String> notes) {
        String slug = placement.footprint().orElseThrow();
        Optional<FootprintDrawing> drawing = catalogue.find(slug).map(FootprintDrawing::of);
        if (drawing.isEmpty()) {
            notes.add(placement.designator() + "'s footprint " + slug + " cannot be read; its holes are not drilled.");
        }
        return new SeatedPart(placement, "", "", Optional.of(slug), drawing, placement.nets());
    }

    /**
     * What the pcb's checks find, as a pcb house would check it: its copper worked out as the artwork works it out, held to
     * the values its rules give.
     *
     * @param values the values the pcb is held to, by rule word
     */
    public List<PcbFinding> checks(PcbArtwork.Settings settings, Map<String, Double> values) {
        return checks(settings, values, copperNets(new PadOutline(settings.curveSteps(), settings.cornerSteps()), settings.curveSteps()));
    }

    /** The same, the copper's nets already worked out — as the artwork works them out once for everything it makes. */
    public List<PcbFinding> checks(PcbArtwork.Settings settings, Map<String, Double> values, CopperNets nets) {
        Map<String, Optional<String>> named = new LinkedHashMap<>();
        Map<String, Map<String, String>> partNets = new LinkedHashMap<>();
        for (SeatedPart part : parts) {
            named.put(part.designator(), part.footprint());
            partNets.put(part.designator(), part.padNets());
        }
        PcbChecks.Subject subject = new PcbChecks.Subject(pcb, footprints(), named, partNets, Optional.of(values));
        return PcbChecks.of(subject, nets, settings.forChecks());
    }

    /** What the files are named after: the pcb's slug. */
    public String name() {
        return name;
    }

    public PcbDrawing pcb() {
        return pcb;
    }

    /** What the documents say about the components the parts are: a house reads its supplier's numbers here. */
    public ComponentFacts facts() {
        return facts;
    }

    /** The parts seated on the pcb, in designator order. */
    public List<SeatedPart> parts() {
        return parts;
    }

    /** What the pcb could not be told whole: a part it stands that the circuit lacks, a footprint not read. */
    public List<String> notes() {
        return notes;
    }

    /**
     * Which copper is on which net: each pad's as the circuit wires it, each track's, arc's and via's as the pads it
     * reaches are.
     */
    public CopperNets copperNets(PadOutline outlines, int curveSteps) {
        Map<String, Map<String, String>> nets = new LinkedHashMap<>();
        for (SeatedPart part : parts) {
            nets.put(part.designator(), part.padNets());
        }
        return CopperNets.of(pcb, footprints(), nets, outlines, curveSteps);
    }

    /** Each seated part's footprint drawing, by designator, in designator order; a part whose drawing is not read has none. */
    public Map<String, FootprintDrawing> footprints() {
        Map<String, FootprintDrawing> footprints = new LinkedHashMap<>();
        for (SeatedPart part : parts) {
            part.drawing().ifPresent(drawing -> footprints.put(part.designator(), drawing));
        }
        return footprints;
    }

    /**
     * A part's value as a person reads it: its value parameter's setting worked out over the circuit's parameters and
     * said with the parameter's unit ({@code 10 kΩ}), its component's default when the part sets none; what the part says in
     * words goes before all that; with nothing to say, its component's title.
     */
    private static String valueOf(Part part, ComponentFacts facts, Map<String, Double> parameters) {
        Optional<String> words = part.valueInWords();
        if (words.isPresent()) {
            return words.get();
        }
        Optional<String> parameterName = facts.valueParameterOf(part.component());
        if (parameterName.isEmpty()) {
            return titleOf(part, facts);
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
                .orElseGet(() -> titleOf(part, facts));
    }

    /** What a part with no value to say is called: its component's title, or its slug where the component cannot be read. */
    private static String titleOf(Part part, ComponentFacts facts) {
        return facts.component(part.component()).map(component -> component.head().title()).filter(title -> !title.isBlank()).orElse(part.component());
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
