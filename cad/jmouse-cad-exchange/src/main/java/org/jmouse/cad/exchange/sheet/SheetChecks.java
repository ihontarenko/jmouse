package org.jmouse.cad.exchange.sheet;

import org.jmouse.settings.Label;
import org.jmouse.cad.settings.SettingsSection;
import org.jmouse.cad.Point;
import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.circuit.Part;
import org.jmouse.cad.circuit.Placement;
import org.jmouse.cad.circuit.SheetBoundary;
import org.jmouse.cad.component.Pin;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.FindingSeverity;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.cad.simulation.SimulatedComponent;
import org.jmouse.cad.simulation.SimulationModule;
import org.jmouse.cad.simulation.SimulationReading;
import org.jmouse.cad.simulation.SimulationVocabulary;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.jmouse.cad.document.FindingSeverity.NOTICE;
import static org.jmouse.cad.document.FindingSeverity.WARNING;

/**
 * A sheet's electrical rules, as a schematic editor checks them before a board is laid: a pin joined to nothing, a pin
 * marked no-connect that is joined after all, pins that may not share a net ({@code settings/schematic/pin-rules}), inputs nothing drives,
 * power pins nothing powers, a label on nothing. What a pin is comes from its component's own document; a part whose
 * component the reader may not see is left out, never guessed at.
 *
 * <p>The engine's one set of a sheet's checks: the bench shows what they find.</p>
 */
public final class SheetChecks {

    /** Its section of the settings tree (ADR 0004): its file and schema under org/jmouse/cad/settings/schematic/pin-rules. */
    public static final SettingsSection<PinRules> SECTION = new SettingsSection<>("schematic/pin-rules", "Pin rules", PinRules.class);

    /**
     * Which pins may not share a net, and how near a no-connect mark stands to a pin to be on it.
     *
     * @param noConnectReach how near a no-connect mark must stand to a pin to be on it, in the sheet's units
     * @param kinds          how each kind of pin is said, by its word
     * @param conflicts      the kinds that may not meet on a net, and how much their meeting weighs
     */
    public record PinRules(@Label("No connect reach") double noConnectReach,
                           @Label("Kinds") Map<String, String> kinds,
                           @Label("Conflicts") List<Conflict> conflicts) {

        public PinRules {
            kinds = Map.copyOf(kinds);
            conflicts = List.copyOf(conflicts);
            for (Conflict conflict : conflicts) {
                conflict.between().forEach(PinKind::named);
                severityOf(conflict.severity());
            }
            for (PinKind kind : PinKind.values()) {
                if (!kinds.containsKey(kind.word())) {
                    throw new IllegalArgumentException("The pin rules say nothing of " + kind.word() + " pins");
                }
            }
        }

        /**
         * Two kinds of pin that may not meet on a net — the same kind twice when two of it may not —, and its weight.
         *
         * @param between  the two kinds of pin, by their words — the same word twice for two of one kind
         * @param severity how much it weighs: error or warning
         */
        public record Conflict(@Label("Pin kinds") List<String> between,
                               @Label("Severity") String severity) {

            public Conflict {
                between = List.copyOf(between);
                if (between.size() != 2) {
                    throw new IllegalArgumentException("A pin conflict is between two kinds, not " + between);
                }
            }
        }

        private String said(PinKind kind) {
            return kinds.get(kind.word());
        }
    }

    /** What a pin is to the rules of who may share a net. */
    enum PinKind {
        OUTPUT("output"), POWER_OUTPUT("power-output"), POWER_INPUT("power-input"), GROUND("ground");

        private final String word;

        PinKind(String word) {
            this.word = word;
        }

        String word() {
            return word;
        }

        static PinKind named(String word) {
            for (PinKind kind : values()) {
                if (kind.word.equals(word)) {
                    return kind;
                }
            }
            throw new IllegalArgumentException("The pin rules know no kind of pin called '" + word + "'");
        }
    }

    /** A pin on the sheet: whose it is, what a wire names it by, its net — empty when it is open —, what it is and where. */
    private record PinOnSheet(String reference, String terminal, String net, Optional<Pin> pin, Point at) {

        String said() {
            return reference + "." + terminal;
        }

        String key() {
            return itemKey(reference);
        }
    }

    private final Circuit     sheet;
    private final SymbolLooks looks;
    private final CatalogueDocuments catalogue;
    private final PinRules    rules;

    private SheetChecks(Circuit sheet, CatalogueDocuments catalogue, PinRules rules) {
        this.sheet = sheet;
        this.catalogue = catalogue;
        this.looks = new SymbolLooks(catalogue);
        this.rules = rules;
    }

    /** Everything the checks find on a sheet, its parts read from the catalogue. */
    public static List<SheetFinding> of(Circuit sheet, CatalogueDocuments catalogue, PinRules rules) {
        return new SheetChecks(sheet, catalogue, rules).findings();
    }

    private List<SheetFinding> findings() {
        List<PinOnSheet> pins = pins();
        List<Placement.Mark> marks = sheet.layout().stream().filter(Placement.Mark.class::isInstance).map(Placement.Mark.class::cast).toList();
        Set<String> wired = new HashSet<>();
        sheet.layout().stream().filter(Placement.Wire.class::isInstance).map(Placement.Wire.class::cast).forEach(wire -> {
            wired.add(wire.from());
            wired.add(wire.to());
        });
        List<Point> noConnects = marks.stream().filter(mark -> mark.symbol().equals(SimulationVocabulary.NO_CONNECT)).map(Placement.Mark::at).toList();
        Map<String, List<PinOnSheet>> byNet = new LinkedHashMap<>();
        pins.stream().filter(pin -> !pin.net().isEmpty()).forEach(pin -> byNet.computeIfAbsent(pin.net(), ignored -> new ArrayList<>()).add(pin));
        Set<String> named = marks.stream().filter(SheetChecks::namesNet).map(SheetChecks::netOf).collect(Collectors.toSet());
        Set<String> fromElsewhere = fromElsewhere(marks);
        List<SheetFinding> findings = new ArrayList<>();
        for (PinOnSheet pin : pins) {
            boolean joined = !pin.net().isEmpty() && (byNet.get(pin.net()).size() > 1 || named.contains(pin.net()));
            boolean touched = wired.contains(pin.said());
            boolean onPurpose = noConnects.stream().anyMatch(mark -> distance(mark, pin.at()) <= rules.noConnectReach());
            if (!joined && !touched && !onPurpose) {
                findings.add(finding(WARNING, pin.said() + " is joined to nothing: wire it, or mark it no-connect", List.of(pin), Optional.of(pin.at())));
            } else if (joined && onPurpose) {
                findings.add(finding(WARNING, pin.said() + " is marked no-connect, and it is on net " + pin.net(), List.of(pin), Optional.of(pin.at())));
            }
        }
        byNet.forEach((net, onNet) -> findings.addAll(netFindings(net, onNet, fromElsewhere.contains(net))));
        findings.addAll(labelFindings(marks, byNet.keySet()));
        return findings;
    }

    // ─── the pins ──────────────────────────────────────────────────────────────────────────────────────

    /** Every pin each part draws, wired or open, where it stands: a part's by its symbol or its box, a block's on its frame. */
    private List<PinOnSheet> pins() {
        Map<String, List<Placement.PlacedPart>> placed = new LinkedHashMap<>();
        sheet.layout().stream().filter(Placement.PlacedPart.class::isInstance).map(Placement.PlacedPart.class::cast)
                .forEach(item -> placed.computeIfAbsent(item.reference(), ignored -> new ArrayList<>()).add(item));
        List<PinOnSheet> pins = new ArrayList<>();
        placed.forEach((reference, items) -> sheet.part(reference).ifPresent(part -> pins.addAll(pinsOf(part, items))));
        return pins;
    }

    private List<PinOnSheet> pinsOf(Part part, List<Placement.PlacedPart> items) {
        Optional<DocumentNode> placesSheet = catalogue.find(part.component()).filter(found -> found.kind().equals(SimulationModule.CIRCUIT));
        if (placesSheet.isPresent()) {
            return blockPins(part, items.getFirst(), SimulationReading.circuit(placesSheet.get()));
        }
        Optional<SymbolLook> found = looks.look(part.component(), part.symbol());
        if (found.isEmpty()) {
            return List.of();
        }
        SymbolLook look = found.get();
        Map<String, String> nets = look.wiring(part);
        List<PinOnSheet> pins = new ArrayList<>();
        for (PartPins.PlacedUnit unit : PartPins.of(part, look, items).placed()) {
            unit.pinsOnSheet().forEach((terminal, at) -> pins.add(new PinOnSheet(part.reference(), terminal, nets.getOrDefault(terminal, ""),
                    Optional.ofNullable(look.pins().get(SymbolLook.bare(look.pinOf(terminal)))), at)));
        }
        return pins;
    }

    /** A block's ports, each the pin its sheet makes of it; none when the block does not fit its sheet, which a run refuses. */
    private static List<PinOnSheet> blockPins(Part block, Placement.PlacedPart item, Circuit placed) {
        BlockFrame frame;
        try {
            frame = BlockFrame.of(block, item, placed);
        } catch (IllegalArgumentException misfit) {
            return List.of();
        }
        SimulatedComponent component = SimulatedComponent.sheet(placed);
        return frame.ports().stream().map(port -> new PinOnSheet(block.reference(), port.port().net(),
                frame.wired().getOrDefault(port.port().net(), ""), component.pin(port.port().net()),
                SheetGeometry.onSheet(item, port.lead()))).toList();
    }

    // ─── the nets ──────────────────────────────────────────────────────────────────────────────────────

    /** Pins that may not share the net, inputs nothing drives on it, power pins nothing powers. */
    private List<SheetFinding> netFindings(String net, List<PinOnSheet> onNet, boolean comesFromElsewhere) {
        List<SheetFinding> findings = new ArrayList<>();
        for (PinRules.Conflict conflict : rules.conflicts()) {
            PinKind first = PinKind.named(conflict.between().get(0));
            PinKind second = PinKind.named(conflict.between().get(1));
            List<PinOnSheet> ofFirst = onNet.stream().filter(pin -> kindOf(pin).filter(first::equals).isPresent()).toList();
            List<PinOnSheet> ofSecond = onNet.stream().filter(pin -> kindOf(pin).filter(second::equals).isPresent()).toList();
            boolean same = first == second;
            if (same ? ofFirst.size() > 1 : !ofFirst.isEmpty() && !ofSecond.isEmpty()) {
                List<PinOnSheet> met = same ? ofFirst : Stream.concat(ofFirst.stream(), ofSecond.stream()).toList();
                String said = same ? ofFirst.size() + " × " + rules.said(first) : rules.said(first) + " and " + rules.said(second);
                findings.add(finding(severityOf(conflict.severity()), "Net " + net + " joins " + said + ": " + saidAll(met), met, Optional.empty()));
            }
        }
        if (net.equals(Circuit.GROUND)) {
            return findings;
        }
        List<PinOnSheet> inputs = onNet.stream().filter(pin -> pin.pin().filter(model -> model.digital() && !model.output()).isPresent()).toList();
        boolean driven = comesFromElsewhere || onNet.stream().anyMatch(pin -> pin.pin()
                .filter(model -> model.output() || model.power() || model.ground() || analog(model)).isPresent());
        if (!inputs.isEmpty() && !driven) {
            findings.add(finding(WARNING, "Net " + net + " has inputs and nothing driving it: " + saidAll(inputs), inputs, Optional.empty()));
        }
        List<PinOnSheet> unpowered = onNet.stream().filter(pin -> kindOf(pin).filter(PinKind.POWER_INPUT::equals).isPresent()).toList();
        boolean powered = comesFromElsewhere || onNet.stream().anyMatch(pin -> kindOf(pin).filter(PinKind.POWER_OUTPUT::equals).isPresent()
                || pin.pin().filter(model -> analog(model) || model.ground()).isPresent());
        if (!unpowered.isEmpty() && !powered) {
            findings.add(finding(WARNING, "Net " + net + " takes power and nothing on it gives any: " + saidAll(unpowered), unpowered, Optional.empty()));
        }
        return findings;
    }

    /**
     * A pin's kind to the rules; none for a pin that drives nothing and takes no power, one that only pulls its line one way
     * — an open drain shares its line —, and a bus line, which both talks and listens: those may meet anything.
     */
    private static Optional<PinKind> kindOf(PinOnSheet pin) {
        return pin.pin().flatMap(model -> {
            if (model.pullsOneWay() || model.bidirectional()) {
                return Optional.empty();
            }
            if (model.ground()) {
                return Optional.of(PinKind.GROUND);
            }
            if (model.power()) {
                return Optional.of(model.output() ? PinKind.POWER_OUTPUT : PinKind.POWER_INPUT);
            }
            return model.output() ? Optional.of(PinKind.OUTPUT) : Optional.empty();
        });
    }

    /** Whether a pin carries an analog signal: it sets its net's voltage, as a source or a passive part does. */
    private static boolean analog(Pin pin) {
        return !pin.digital() && !pin.power() && !pin.ground();
    }

    // ─── the marks ─────────────────────────────────────────────────────────────────────────────────────

    /** A label a net is named by on nothing else: it joins nothing. */
    private static List<SheetFinding> labelFindings(List<Placement.Mark> marks, Set<String> pinNets) {
        List<Placement.Mark> labels = marks.stream().filter(SheetChecks::labels).filter(mark -> mark.net() != null).toList();
        List<SheetFinding> findings = new ArrayList<>();
        for (Placement.Mark label : labels) {
            boolean others = labels.stream().anyMatch(other -> other != label && other.net().equals(label.net()));
            if (!pinNets.contains(label.net()) && !others) {
                findings.add(new SheetFinding(NOTICE, "Label " + label.net() + " is on nothing else: it joins nothing",
                        List.of(itemKey(label.reference())), Optional.of(label.at())));
            }
        }
        return findings;
    }

    /** The nets that may be driven and powered on another sheet: a port's or a global label's, and the sheet's own ports and global nets. */
    private Set<String> fromElsewhere(List<Placement.Mark> marks) {
        Set<String> nets = new HashSet<>();
        marks.stream().filter(mark -> mark.symbol().equals(SimulationVocabulary.PORT) || mark.symbol().equals(SimulationVocabulary.GLOBAL))
                .filter(mark -> mark.net() != null).forEach(mark -> nets.add(mark.net()));
        SheetBoundary boundary = sheet.boundary();
        boundary.ports().forEach(port -> nets.add(port.net()));
        boundary.nets().stream().filter(SheetBoundary.Net::global).forEach(net -> nets.add(net.name()));
        return nets;
    }

    /** Whether a mark names a net: a ground, a common, a supply, any label. */
    private static boolean namesNet(Placement.Mark mark) {
        return labels(mark) || mark.symbol().equals(SimulationVocabulary.GROUND) || mark.symbol().equals(SimulationVocabulary.COMMON)
                || mark.symbol().equals(SimulationVocabulary.POWER);
    }

    /** Whether a mark is a label: of this sheet, a port of it, or global. */
    private static boolean labels(Placement.Mark mark) {
        return mark.symbol().equals(SimulationVocabulary.LABEL) || mark.symbol().equals(SimulationVocabulary.PORT)
                || mark.symbol().equals(SimulationVocabulary.GLOBAL);
    }

    /** The net a mark names; a ground or a common with none names the ground. */
    private static String netOf(Placement.Mark mark) {
        return mark.net() == null ? Circuit.GROUND : mark.net();
    }

    // ─── words ─────────────────────────────────────────────────────────────────────────────────────────

    private static SheetFinding finding(FindingSeverity severity, String message, List<PinOnSheet> about, Optional<Point> at) {
        return new SheetFinding(severity, message, about.stream().map(PinOnSheet::key).distinct().toList(), at);
    }

    private static String saidAll(List<PinOnSheet> pins) {
        return pins.stream().map(PinOnSheet::said).collect(Collectors.joining(", "));
    }

    private static String itemKey(String reference) {
        return "item:" + reference;
    }

    private static FindingSeverity severityOf(String word) {
        return FindingSeverity.valueOf(word.toUpperCase(Locale.ROOT));
    }

    private static double distance(Point first, Point second) {
        return Math.hypot(first.x() - second.x(), first.y() - second.y());
    }
}
