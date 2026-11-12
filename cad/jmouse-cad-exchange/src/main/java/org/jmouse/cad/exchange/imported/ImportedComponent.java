package org.jmouse.cad.exchange.imported;

import org.jmouse.cad.component.Pin;
import org.jmouse.cad.component.PinBox;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.MetaLineNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.document.Provenance;
import org.jmouse.cad.simulation.SimulationLines;
import org.jmouse.cad.simulation.SimulationModule;
import org.jmouse.cad.simulation.SimulationVocabulary;
import org.jmouse.el.grammar.ClauseBlockNode;
import org.jmouse.el.grammar.ClauseLineNode;
import org.jmouse.el.grammar.LineBuilder;
import org.jmouse.el.grammar.LineGrammar;
import org.jmouse.el.grammar.Nodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The component another product's part stands for when the catalogue has none: its package's pins, the symbol it is
 * drawn with, the footprint its package lands on — a part that can be placed, wired and stored, and that computes
 * nothing until its model is written. What the product's part is, its import says; this writes the document.
 *
 * @param provenance  whose the part is: the drawing it came with says
 * @param designator  the letters its parts are named by ({@code U}), or null
 * @param packageName the package the product names, as it names it, or null
 * @param bubbled     the pins a box draws with an inverting bubble, by name
 * @param footprint   the footprint its package lands on, by slug, or null
 */
public record ImportedComponent(String title, String slug, String summary, Provenance provenance, String designator,
                                String packageName, List<Pin> pins, Set<String> bubbled, Drawing drawing, String footprint) {

    /** How the component is drawn. */
    public sealed interface Drawing {

        /** With a symbol of its own or of the catalogue's, by slug. */
        record Uses(String symbol) implements Drawing {
        }

        /** As our box, its pins placed on its sides by what they are ({@link PinBox}). */
        record Box() implements Drawing {
        }
    }

    /** The side lines of a box, by their side. */
    private static final Map<PinBox.Side, String> SIDES = Map.of(PinBox.Side.LEFT, SimulationVocabulary.LEFT,
            PinBox.Side.RIGHT, SimulationVocabulary.RIGHT, PinBox.Side.TOP, SimulationVocabulary.TOP,
            PinBox.Side.BOTTOM, SimulationVocabulary.BOTTOM);

    public ImportedComponent {
        pins = List.copyOf(pins);
        bubbled = Collections.unmodifiableSet(new LinkedHashSet<>(bubbled));
    }

    /** The component's document, filed as the product it came from files its components. */
    public DocumentNode document(ImportFiling filing) {
        DocumentNode document = new DocumentNode(SimulationModule.COMPONENT, title, slug, null, 1);
        MetaNode meta = MetaNode.filed(summary, null, filing.libraryCategory(provenance), filing.componentTags());
        provenance.writeInto(meta);
        if (designator != null) {
            meta.addExpression(MetaLineNode.of(MetaNode.DESIGNATOR, Nodes.word(designator)));
        }
        if (packageName != null && !packageName.isBlank()) {
            meta.addExpression(MetaLineNode.of("package", Nodes.text(packageName)));
        }
        document.addExpression(meta);
        ClauseBlockNode pinBlock = new ClauseBlockNode("pins", null);
        pinBlock.open();
        pins.forEach(pin -> pinBlock.addExpression(pinLine(pin)));
        document.addExpression(pinBlock);
        document.addExpression(switch (drawing) {
            case Drawing.Uses uses -> uses(uses.symbol());
            case Drawing.Box ignored -> box();
        });
        if (footprint != null) {
            document.addExpression(new ClauseBlockNode("footprint",
                    LineBuilder.line(SimulationLines.FOOTPRINT).slot("slug", Nodes.word(footprint)).build()));
        }
        return document;
    }

    /** {@code name nature [direction] [number] [edge] ["label"]}: an input says no direction, as a pin read so is one. */
    private static ClauseLineNode pinLine(Pin pin) {
        LineBuilder line = LineBuilder.line(SimulationLines.PIN).slot("name", Nodes.name(pin.name()))
                .slot("nature", Nodes.word(pin.nature()));
        if (!pin.direction().equals(SimulationVocabulary.INPUT)) {
            line.slot("direction", Nodes.word(pin.direction()));
        }
        if (!pin.numbers().isEmpty()) {
            line.slot("numbers", Nodes.groups(pin.numbers()));
        }
        if (pin.edge()) {
            line.flag(SimulationVocabulary.EDGE);
        }
        if (!pin.label().isEmpty()) {
            line.ending(Nodes.text(pin.label()));
        }
        return line.build();
    }

    /** Our box, its pins on its sides by what they are, an edge and a bubble said beside a pin's name. */
    private ClauseBlockNode box() {
        ClauseBlockNode box = new ClauseBlockNode("symbol", LineBuilder.line(SimulationLines.SYMBOL).flag("box").build());
        box.open();
        PinBox.sides(pins).forEach((side, placed) -> {
            LineGrammar grammar = SimulationLines.BOX_SIDES.stream().filter(line -> line.word().equals(SIDES.get(side)))
                    .findFirst().orElseThrow();
            List<List<String>> items = placed.stream().map(pin -> {
                List<String> item = new ArrayList<>(List.of(pin.name()));
                if (pin.edge()) {
                    item.add(SimulationVocabulary.EDGE);
                }
                if (bubbled.contains(pin.name())) {
                    item.add(SimulationVocabulary.BUBBLE);
                }
                return (List<String>) item;
            }).toList();
            box.addExpression(LineBuilder.line(grammar).slot("pins", Nodes.groups(items)).build());
        });
        return box;
    }

    /** {@code symbol uses slug}. */
    private static ClauseBlockNode uses(String symbol) {
        return new ClauseBlockNode("symbol", LineBuilder.line(SimulationLines.SYMBOL).clause("uses", Nodes.words(List.of(symbol)))
                .build());
    }
}
