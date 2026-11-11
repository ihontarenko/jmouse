package org.jmouse.cad.library;

import org.jmouse.cad.board.BoardModule;
import org.jmouse.cad.board.BoardVocabulary;
import org.jmouse.cad.board.drawing.BoardDrawing;
import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.circuit.Part;
import org.jmouse.cad.component.Component;
import org.jmouse.cad.component.Drawing;
import org.jmouse.cad.component.Family;
import org.jmouse.cad.component.Seat;
import org.jmouse.cad.document.DeclarationParser;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.schematic.SchematicModule;
import org.jmouse.cad.simulation.SimulationLines;
import org.jmouse.cad.simulation.SimulationModule;
import org.jmouse.cad.simulation.SimulationReading;
import org.jmouse.cad.simulation.SimulationVocabulary;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * What a catalogue keeps of a document besides its text, read from it: where it is filed, what lists show of it, and
 * every other document it names — what "used by" is the reverse of. A catalogue derives it from the head revision on
 * every save and edits none of it on its own.
 *
 * <p>Each reference is named by the word the text names it with — {@code extends}, {@code use}, {@code symbol},
 * {@code portrait}, {@code footprint}, {@code parts}, {@code frame}, {@code circuit}, {@code see_also} —, so a store
 * keeps them as the language writes them and learns no vocabulary of its own.</p>
 *
 * @param slug       its slug
 * @param title      its title
 * @param kind       the word its head opens with
 * @param summary    {@code meta { summary … }}, or null
 * @param category   {@code meta { category … }}, or null when it is filed nowhere
 * @param tags       {@code meta { tags … }}, in order
 * @param featured   {@code meta { featured true }}
 * @param references every other document it names, each way once, in the order the text names them
 */
public record DocumentIndex(String slug, String title, String kind, String summary, String category, List<String> tags,
                            boolean featured, List<Reference> references) {

    /** What separates a slug from the revision it pins: {@code resistor@3}. */
    public static final String REVISION_MARK = "@";

    /** The word {@code featured} is written with: {@code featured yes}, as every shipped document writes it. */
    private static final String YES = "yes";

    /** The kinds a reference of each relation may name; {@code see_also} names any. */
    private static final Map<String, Set<String>> NAMED_KINDS = Map.of(
            DeclarationParser.EXTENDS, Set.of(SimulationModule.COMPONENT.word()),
            DeclarationParser.USE, Set.of(SimulationModule.FAMILY.word(), SimulationModule.FUNCTIONS.word()),
            SimulationLines.SYMBOL.word(), Set.of(SchematicModule.SYMBOL.word()),
            SimulationLines.PORTRAIT.word(), Set.of(SchematicModule.PORTRAIT.word()),
            SimulationLines.FOOTPRINT.word(), Set.of(BoardModule.FOOTPRINT.word()),
            SimulationLines.FRAME.word(), Set.of(SchematicModule.FRAME.word()),
            SimulationVocabulary.PARTS, Set.of(SimulationModule.COMPONENT.word(), SimulationModule.CIRCUIT.word()),
            BoardVocabulary.CIRCUIT, Set.of(SimulationModule.CIRCUIT.word()));

    public DocumentIndex {
        tags = List.copyOf(tags);
        references = List.copyOf(references);
    }

    /**
     * One document named, and how.
     *
     * @param slug     the document named, without the revision it may pin
     * @param relation the word the text names it with
     */
    public record Reference(String slug, String relation) {
    }

    /** The index of a document the language has read; refuses one the engine cannot read. */
    public static DocumentIndex of(DocumentNode document) {
        Optional<MetaNode> meta = document.block(MetaNode.class);
        References references = new References(document.slug());

        String kind = document.kind().word();
        if (kind.equals(SimulationModule.COMPONENT.word())) {
            component(SimulationReading.component(document), references);
        } else if (kind.equals(SimulationModule.FAMILY.word())) {
            family(SimulationReading.family(document), references);
        } else if (kind.equals(SimulationModule.CIRCUIT.word())) {
            circuit(SimulationReading.circuit(document), references);
        } else if (kind.equals(BoardModule.PCB.word())) {
            references.add(BoardDrawing.of(document).circuit(), BoardVocabulary.CIRCUIT);
        }
        meta.ifPresent(found -> found.words(MetaNode.SEE_ALSO)
                .forEach(slug -> references.add(slug, MetaNode.SEE_ALSO)));

        return new DocumentIndex(document.slug(), document.title(), kind,
                                 meta.flatMap(found -> found.text(MetaNode.SUMMARY)).orElse(null),
                                 meta.flatMap(found -> found.text(MetaNode.CATEGORY)).orElse(null),
                                 meta.map(found -> found.words(MetaNode.TAGS)).orElse(List.of()),
                                 meta.flatMap(found -> found.text(MetaNode.FEATURED)).map(YES::equals).orElse(false),
                                 references.all());
    }

    /**
     * Whether a reference of this relation may name a document of that kind — a {@code symbol} names a symbol, a part a
     * component or a sheet —; a relation that says nothing of kinds ({@code see_also}) names any.
     */
    public static boolean names(String relation, String kind) {
        Set<String> kinds = NAMED_KINDS.get(relation);
        return kinds == null || kinds.contains(kind);
    }

    /** The slug a reference names, without the revision it may pin: {@code resistor@3} is {@code resistor}. */
    public static String slugOf(String reference) {
        int mark = reference.indexOf(REVISION_MARK);
        if (mark < 0) {
            return reference;
        }
        return reference.substring(0, mark);
    }

    private static void component(Component component, References references) {
        component.bases().forEach(base -> references.add(base, DeclarationParser.EXTENDS));
        component.uses().forEach(used -> references.add(used, DeclarationParser.USE));
        drawing(component.symbol(), SimulationLines.SYMBOL.word(), references);
        drawing(component.portrait(), SimulationLines.PORTRAIT.word(), references);
        for (Seat seat : component.seats()) {
            references.add(seat.footprint(), SimulationLines.FOOTPRINT.word());
        }
        parts(component.parts(), references);
    }

    private static void family(Family family, References references) {
        family.uses().forEach(used -> references.add(used, DeclarationParser.USE));
    }

    private static void circuit(Circuit circuit, References references) {
        parts(circuit.parts(), references);
        references.add(circuit.frame(), SimulationLines.FRAME.word());
    }

    private static void parts(List<Part> parts, References references) {
        for (Part part : parts) {
            references.add(part.component(), SimulationVocabulary.PARTS);
            references.add(part.symbol(), SimulationLines.SYMBOL.word());
            references.add(part.footprint(), SimulationLines.FOOTPRINT.word());
        }
    }

    private static void drawing(Drawing drawing, String relation, References references) {
        if (drawing == null) {
            return;
        }
        drawing.uses().forEach(used -> references.add(used, relation));
    }

    /** The references met so far, each way once, none to the document itself. */
    private static final class References {

        private final String           self;
        private final Set<Reference>   met = new LinkedHashSet<>();

        private References(String self) {
            this.self = self;
        }

        private void add(String reference, String relation) {
            if (reference == null || reference.isBlank()) {
                return;
            }
            String slug = slugOf(reference.strip());
            if (!slug.equals(self)) {
                met.add(new Reference(slug, relation));
            }
        }

        private List<Reference> all() {
            return new ArrayList<>(met);
        }
    }
}
