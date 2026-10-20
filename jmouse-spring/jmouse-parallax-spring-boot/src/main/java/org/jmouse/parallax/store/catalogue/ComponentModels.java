package org.jmouse.parallax.store.catalogue;

import org.jmouse.parallax.owner.ParallaxOwner;
import org.jmouse.parallax.script.ExperienceDocument;
import org.jmouse.parallax.circuit.elaboration.SheetHierarchy;
import org.jmouse.parallax.component.ComponentDocuments;
import org.jmouse.parallax.script.engine.DefinedObject;
import org.jmouse.parallax.script.engine.ExperienceDefinitions;
import org.jmouse.parallax.component.ComponentModel;
import org.jmouse.parallax.object.ComponentLibrary;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ObjectKind;
import org.jmouse.parallax.object.ParameterDefinition;
import org.jmouse.parallax.jpa.catalogue.MathematicalObject;
import org.jmouse.parallax.jpa.catalogue.ObjectStore;
import org.jmouse.parallax.problem.ParallaxProblem;
import org.jmouse.parallax.store.support.ParallaxJson;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves component references within a circuit's catalogue or workspace boundary: {@code lm358} is the head revision
 * of that object's text, {@code lm358@3} its third revision — of any kind, every revision being kept in
 * {@link ObjectStore} — and a family or a function library comes from {@link SharedDocuments}.
 *
 * <p>It also answers how a part is drawn. A component that says {@code symbol uses diode-symbol { a anode }} is drawn
 * from that symbol document — its paths, its pins at their positions, its name and value labels — with each port
 * standing for the component pin the line maps it to; the symbols themselves are listed for the pages and editors that
 * show them.</p>
 */
public class ComponentModels {

    /** What a sheet placed as a block is, among the kinds a palette groups by. */
    public static final String SHEET_KIND = "sheet";

    /** What a sheet's block is called on its parent before its number, as KiCad names a sheet symbol. */
    private static final String SHEET_DESIGNATOR = "S";

    private final ObjectStore objects;
    private final ObjectAccess objectAccess;
    private final ObjectDocuments objectDocuments;
    private final ParallaxJson json;
    private final SharedDocuments sharedDocuments;

    public ComponentModels(ObjectStore objects, ObjectAccess objectAccess,
                           ObjectDocuments objectDocuments, ParallaxJson json, SharedDocuments sharedDocuments) {
        this.objects = objects;
        this.objectAccess = objectAccess;
        this.objectDocuments = objectDocuments;
        this.json = json;
        this.sharedDocuments = sharedDocuments;
    }

    /** The text of one revision of an object; a component's text is its model. */
    private Optional<String> sourceAt(MathematicalObject object, int revision) {
        return objects.sourceAt(object.getId(), revision);
    }

    public record PinView(String name, java.util.Map<String, Integer> numbers, boolean shared, String nature,
                          boolean output, String description) { }
    public record ParameterView(String name, double value, boolean perUnit, String description) { }
    /** A text parameter: its words, the only words a part may write when it lists them, and what it is. */
    public record TextView(String name, String value, List<String> choices, String description) { }

    /**
     * A component as an editor and a schematic need it.
     *
     * @param symbol          a parametric symbol's lines ({@code box}, {@code left d, clk}); empty for a symbol document
     * @param symbolReference the symbol document it is drawn with by default, if the reader may see it
     * @param symbolVariants  every symbol its text names, the default first
     * @param symbolDrawing   that symbol's drawing JSON: size, paths, pins with positions, name and value labels
     * @param symbolPorts     each port of that symbol → the component pin it stands for
     * @param measures        what an instrument reads, figure by figure, with its unit; empty for a component that is none
     * @param leads           how each pin's lead looks on a sheet — its colour and its stripes —, as the component writes it
     * @param displayUnits    the unit of each display figure written beside the part, by figure
     * @param displayCaptions what such a figure is of, written after it ("of the rated voltage"), by figure
     * @param designator      what a part of it is called on a sheet before its number ({@code meta designator R}), or null
     * @param sheetParameters a sheet's sliders as the sheet declares them — range, step, unit —, which each of its blocks
     *                        sets on its own; empty for a component
     */
    public record ModelView(String name, String kind, List<String> units, List<PinView> pins,
                            List<ParameterView> parameters, List<TextView> texts, List<String> symbol, String symbolReference,
                            List<String> symbolVariants, String symbolDrawing, Map<String, String> symbolPorts,
                            List<MeasureView> measures, Map<String, LeadView> leads, Map<String, String> displayUnits,
                            Map<String, String> displayCaptions,
                            String designator, List<ParameterDefinition> sheetParameters) { }
    /** A lead's look: one of the lead colours, and the colour of its stripes or null. */
    public record LeadView(String colour, String stripes) { }
    /** One figure an instrument reads ({@code reading = plus - minus unit V signal}): its name, its unit, whether it is a signal. */
    public record MeasureView(String name, String unit, boolean signal) { }
    public record ReferenceView(String reference, String name, String kind, String origin,
                                ParallaxOwner owner, String status) { }

    /** A symbol document as a shelf or a palette shows it: its drawing, ready to draw. */
    public record SymbolView(String slug, String title, String summary, String origin, ParallaxOwner owner,
                             String drawing) { }

    /**
     * References an editor may offer, with their scope for the circuit being edited: every component, pinned to its
     * revision, and every circuit somebody owns as a sheet — by its slug alone, since the sheets of one project follow
     * each other's latest text. A sheet is offered only beside circuits of its own owner.
     */
    public List<ReferenceView> list() {
        List<ReferenceView> result = new ArrayList<>();
        objects.findByKind(ObjectKind.COMPONENT).stream()
                .filter(objectAccess::canRead)
                .forEach(object -> result.add(new ReferenceView(object.getSlug() + "@" + object.getRevision(),
                        object.getTitle(), "component", CatalogueViews.Origin.of(object).name(), CatalogueViews.ownerOf(object),
                        object.getStatus().name())));
        objects.findByKind(ObjectKind.CIRCUIT).stream()
                .filter(object -> !object.isCatalogue() && objectAccess.canRead(object))
                .forEach(object -> result.add(new ReferenceView(object.getSlug(), object.getTitle(), SHEET_KIND,
                        CatalogueViews.Origin.of(object).name(), CatalogueViews.ownerOf(object), object.getStatus().name())));
        return result;
    }

    /** Every symbol document the reader may see, with its drawing, by title. */
    @Transactional(readOnly = true)
    public List<SymbolView> symbols() {
        return objects.findByKind(ObjectKind.SYMBOL).stream()
                .filter(objectAccess::canRead)
                .sorted(Comparator.comparing(MathematicalObject::getTitle))
                .map(symbol -> new SymbolView(symbol.getSlug(), symbol.getTitle(), symbol.getSummary(),
                        CatalogueViews.Origin.of(symbol).name(), CatalogueViews.ownerOf(symbol), drawingOf(symbol)))
                .toList();
    }

    /** Metadata for the editor and schematic, subject to the same reader access as the model object. */
    @Transactional(readOnly = true)
    public ModelView describe(String reference) {
        Optional<String> source;
        int separator = reference.lastIndexOf('@');
        if (separator < 0) {
            source = objects.findBySlug(reference).filter(objectAccess::canRead)
                    .flatMap(model -> sourceAt(model, model.getRevision()));
        } else {
            int revision;
            try {
                revision = Integer.parseInt(reference.substring(separator + 1));
            } catch (NumberFormatException exception) {
                throw ParallaxProblem.notFound("The component model");
            }
            source = objects.findBySlug(reference.substring(0, separator))
                    .filter(objectAccess::canRead)
                    .flatMap(model -> sourceAt(model, revision));
        }
        String text = source.orElseThrow(() -> ParallaxProblem.notFound("The component model"));
        if (SheetHierarchy.isSheetSource(text)) {
            return sheetBlock(text, reference);
        }
        // Read as the editor reads it, components included: a part that `extends diode` takes its displays, pins and
        // parameters from the diode, and through the shared documents alone the base was not found and silently left
        // out — the 1N4148 reached the sheet without its "overvoltage … of VRRM".
        ComponentModel model = ComponentDocuments.read(text, reference, forEditing());
        ExperienceDocument document = ComponentSources.readComponent(text);
        List<String> variants = ComponentSources.symbolsOf(document);
        MathematicalObject symbol = variants.isEmpty() ? null : readableSymbol(variants.getFirst()).orElse(null);
        String drawing = symbol == null ? null : drawingOf(symbol);
        Map<String, String> ports = drawing == null ? Map.of()
                : ComponentSources.pinsByPort(document, pinNames(model), portsOf(drawing));
        return new ModelView(model.name(), model.kind(), model.units(),
                model.pins().stream().map(pin -> new PinView(pin.name(), pin.numbers(), pin.shared(), pin.nature().name(),
                        pin.output(), pin.description())).toList(),
                model.parameters().stream().map(parameter -> new ParameterView(parameter.name(), parameter.value(),
                        parameter.perUnit(), parameter.description())).toList(),
                model.texts().entrySet().stream().map(written -> new TextView(written.getKey(), written.getValue().value(),
                        written.getValue().choices(), written.getValue().description())).toList(), model.symbol(),
                symbol == null ? null : symbol.getSlug(), variants, drawing, ports,
                model.measures().entrySet().stream().map(measure -> new MeasureView(measure.getKey(), measure.getValue().unit(), measure.getValue().signal())).toList(),
                model.leads().entrySet().stream().collect(java.util.stream.Collectors.toMap(Map.Entry::getKey,
                        lead -> new LeadView(lead.getValue().colour(), lead.getValue().stripes()), (one, other) -> one, java.util.LinkedHashMap::new)),
                model.displayUnits(), model.displayCaptions(), model.headerValue("designator").orElse(null), List.of());
    }

    /**
     * A sheet as a part on its parent sheet: a box whose pins are the sheet's ports — what it takes in on the left,
     * what it gives out on the right; with outputs named nowhere, the second half of its ports — and whose parameters
     * are the sheet's sliders, which the block may set.
     */
    private static ModelView sheetBlock(String text, String reference) {
        DefinedObject sheet = ExperienceDefinitions.read(text, reference);
        if (!(sheet.definition() instanceof ObjectDefinition.Circuit circuit)) {
            throw ParallaxProblem.notFound("The component model");
        }
        List<ObjectDefinition.SheetPort> ports = circuit.boundary().ports();
        List<String> left = new ArrayList<>();
        List<String> right = new ArrayList<>();
        boolean outputsNamed = ports.stream().anyMatch(port -> port.direction().equals("output"));
        for (int index = 0; index < ports.size(); index++) {
            ObjectDefinition.SheetPort port = ports.get(index);
            boolean out = outputsNamed ? port.direction().equals("output") : index >= (ports.size() + 1) / 2;
            (out ? right : left).add(port.net());
        }
        List<String> symbol = new ArrayList<>(List.of("box"));
        if (!left.isEmpty()) {
            symbol.add("left " + String.join(", ", left));
        }
        if (!right.isEmpty()) {
            symbol.add("right " + String.join(", ", right));
        }
        List<PinView> pins = ports.stream().map(port -> new PinView(port.net(), Map.of(), false, switch (port.direction()) {
            case "power" -> "POWER";
            case "ground" -> "GROUND";
            default -> "ANALOG";
        }, port.direction().equals("output"), null)).toList();
        List<ParameterView> parameters = sheet.parameters().stream()
                .map(parameter -> new ParameterView(parameter.name(), parameter.defaultValue(), false, parameter.label())).toList();
        return new ModelView(sheet.title(), SHEET_KIND, List.of(), pins, parameters, List.of(), symbol, null, List.of(), null, Map.of(),
                List.of(), Map.of(), Map.of(), Map.of(), SHEET_DESIGNATOR, sheet.parameters());
    }

    /**
     * Why a symbol cannot draw a component — see {@link ComponentSources#bindingProblem} — or empty when every pin of
     * the component lands on a port of the symbol.
     */
    Optional<String> bindingProblem(String source, MathematicalObject symbol) {
        ExperienceDocument component = ComponentSources.readComponent(source);
        ComponentModel model = ComponentDocuments.read(source, component.slug(), forEditing());
        return ComponentSources.bindingProblem(component, pinNames(model), symbol.getSlug(), portsOf(drawingOf(symbol)));
    }

    /** A model's pins, those of the base it extends included. */
    private static Set<String> pinNames(ComponentModel model) {
        return model.pins().stream().map(pin -> pin.name()).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public ComponentLibrary forCircuit(MathematicalObject circuit) {
        return library(model -> visibleFrom(circuit, model));
    }

    /**
     * The components a text being edited may use, before it belongs anywhere: every one the reader may read, and the
     * shared library. A saved circuit is checked again against {@link #forCircuit}, which is narrower.
     */
    public ComponentLibrary forEditing() {
        return library(objectAccess::canRead);
    }

    private ComponentLibrary library(java.util.function.Predicate<MathematicalObject> visible) {
        ComponentLibrary components = reference -> {
            int separator = reference.lastIndexOf('@');
            if (separator < 0) {
                // A bare slug is a component's head — or a sheet of the same project, placed as a block: a project's
                // sheets follow each other's latest text, since they are edited together.
                return objects.findBySlug(reference)
                        .filter(model -> placeable(model) && visible.test(model))
                        .flatMap(model -> sourceAt(model, model.getRevision()));
            }
            if (separator < 1 || separator == reference.length() - 1) {
                return Optional.empty();
            }
            int revision;
            try {
                revision = Integer.parseInt(reference.substring(separator + 1));
            } catch (NumberFormatException exception) {
                return Optional.empty();
            }
            return objects.findBySlug(reference.substring(0, separator))
                    .filter(visible)
                    .flatMap(model -> sourceAt(model, revision));
        };
        return reference -> components.source(reference).or(() -> sharedDocuments.source(reference));
    }

    /** A symbol document by its slug, if it is one and the reader may see it. */
    private Optional<MathematicalObject> readableSymbol(String slug) {
        return objects.findBySlug(slug)
                .filter(candidate -> candidate.getKind() == ObjectKind.SYMBOL && objectAccess.canRead(candidate));
    }

    /** A symbol object's drawing, read from its head text. */
    private String drawingOf(MathematicalObject symbol) {
        ObjectDefinition definition = objectDocuments.cached(symbol.getSourceChecksum(),
                () -> sourceAt(symbol, symbol.getRevision()).orElseThrow(), symbol.getSlug()).definition();
        return ((ObjectDefinition.Symbol) definition).drawing();
    }

    /** The names of the pins a drawing places, in the order it draws them. */
    private List<String> portsOf(String drawing) {
        Map<?, ?> read = json.read(drawing, Map.class);
        List<String> ports = new ArrayList<>();
        if (read.get("pins") instanceof List<?> pins) {
            for (Object pin : pins) {
                if (pin instanceof Map<?, ?> written && written.get("name") instanceof String name) {
                    ports.add(name);
                }
            }
        }
        return ports;
    }

    /** What a part may name by its slug alone: a component, or a circuit somebody owns — a sheet of their project. */
    private static boolean placeable(MathematicalObject model) {
        return model.getKind() == ObjectKind.COMPONENT || (model.getKind() == ObjectKind.CIRCUIT && !model.isCatalogue());
    }

    private boolean visibleFrom(MathematicalObject circuit, MathematicalObject model) {
        if (model.isCatalogue()) {
            return model.getStatus() == MathematicalObject.Status.PUBLISHED;
        }
        return !circuit.isCatalogue() && model.owner().equals(circuit.owner());
    }
}
