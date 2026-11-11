package org.jmouse.cad.packages.symbol;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.MetaLineNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.document.PathTextNode;
import org.jmouse.cad.document.TechnicalClass;
import org.jmouse.cad.packages.LibraryFiling;
import org.jmouse.cad.packages.MeasuredPath;
import org.jmouse.cad.packages.PackageTable;
import org.jmouse.cad.schematic.DrawingGrammar;
import org.jmouse.cad.schematic.SchematicModule;
import org.jmouse.el.grammar.ClauseBlockNode;
import org.jmouse.el.grammar.ClauseLineNode;
import org.jmouse.el.grammar.LineBuilder;
import org.jmouse.el.grammar.Nodes;
import org.jmouse.el.node.Expression;
import org.jmouse.sheet.Sheet;
import org.jmouse.sheet.SheetEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Connector symbols drawn from their contact count, as {@code connector-symbols.sht} lists them: a single-row header, a
 * double-row one numbered as a ribbon cable counts, a screw terminal block — each optionally inside its shield. Contacts
 * stand on the pitch, the first at −pitch × ⌊(n − 1) / 2⌋, so every contact lands on the sheet's grid and the drawing
 * stays near its origin. Each symbol is built as the tree of its document.
 */
public final class ConnectorSymbols {

    /** The pitch every contact stands on: 2.54 mm, in sheet units. */
    private static final int PITCH = 40;

    /** What a drawing is high beyond its contacts' span. */
    private static final int MARGIN = 60;

    /** A single-row contact: its pin's end, the lead's start, and the contact's square — its outer edge and half its side. */
    private static final int SINGLE_PIN = 40;
    private static final int SINGLE_LEAD = -4;
    private static final int SINGLE_CONTACT = -8;
    private static final int SINGLE_CONTACT_HALF = 2;
    private static final int SINGLE_WIDTH = 60;

    /** A double-row or screw body: its half width, how far it stands past the outer contacts, and the pins' reach. */
    private static final int BODY_HALF = 20;
    private static final int BODY_MARGIN = 10;
    private static final int DOUBLE_PIN = 60;
    private static final int DOUBLE_WIDTH = 120;

    /** A screw clamp: the wire's end, the screw's radius, and half the slot across it. */
    private static final int SCREW_PIN = -40;
    private static final int SCREW_RADIUS = 6;
    private static final int SCREW_SLOT = 4;
    private static final int SCREW_WIDTH = 80;

    /** Where the labels stand from the outer contacts: above the first, below the last. */
    private static final int SINGLE_NAME = 16;
    private static final int SINGLE_VALUE = 20;
    private static final int BLOCK_NAME = 24;
    private static final int BLOCK_VALUE = 28;
    private static final int SHIELD_VALUE = 16;

    /** A shield: its box's sides around a single or double row, how far past the outer contacts, its pin's name and label. */
    private static final int SHIELD_SINGLE_LEFT = -14;
    private static final int SHIELD_SINGLE_RIGHT = 30;
    private static final int SHIELD_DOUBLE_HALF = 34;
    private static final int SHIELD_MARGIN = 14;
    private static final String SHIELD_PIN = "shield";
    private static final String SHIELD_LABEL = "SH";

    /** What a contact's pin is named: {@code pin1}. */
    private static final String PIN = "pin";

    private enum Style {
        SINGLE_ROW("single-row"), DOUBLE_ROW("double-row"), SCREW("screw");

        private final String word;

        Style(String word) {
            this.word = word;
        }

        static Style of(String word) {
            for (Style style : values()) {
                if (style.word.equals(word)) {
                    return style;
                }
            }
            throw new IllegalArgumentException("No connector style '" + word + "'");
        }
    }

    /** A symbol of the library: its document, and the folder it is filed in. */
    public record ConnectorSymbol(String folder, DocumentNode document) {
    }

    private ConnectorSymbols() {
    }

    /** Every symbol the table lists: each family at every contact count of its range. */
    public static List<ConnectorSymbol> generate(Sheet table) {
        LibraryFiling filing = LibraryFiling.of(table);
        String note = "Generated from symbol-standards/" + table.slug() + ".sht; regenerate, never edit.";
        List<ConnectorSymbol> symbols = new ArrayList<>();
        for (SheetEntry entry : table.entries()) {
            PackageTable family = new PackageTable(entry);
            Style style = Style.of(family.word("style"));
            boolean shielded = "yes".equals(family.optionalWord("shielded").orElse("no"));
            for (int contacts : family.counts("pins")) {
                Map<String, String> words = Map.of("pins", String.valueOf(contacts));
                String slug = PackageTable.named(family.word("name"), words) + "-symbol";
                String code = TechnicalClass.SYMBOL.code(PackageTable.designated(family.word("designation"), words));
                symbols.add(new ConnectorSymbol(family.word("folder"),
                        symbol(style, shielded, slug, code, family.title(), contacts, filing, note)));
            }
        }
        return symbols;
    }

    private static DocumentNode symbol(Style style, boolean shielded, String slug, String code, String family, int contacts,
                                       LibraryFiling filing, String note) {
        int first = -PITCH * ((contacts - 1) / 2);
        int last = first + (contacts - 1) * PITCH;
        int shieldPin = last + PITCH;
        String drawn = shielded ? "inside its shield, as drawn on a schematic" : "as drawn on a schematic";
        String counted = contacts + (contacts == 1 ? " contact" : " contacts");
        MeasuredPath path = MeasuredPath.path();
        List<Expression> pins = new ArrayList<>();
        String title;
        String summary;
        int width;
        int labelX;
        int nameY;
        int valueY;
        switch (style) {
            case SINGLE_ROW -> {
                for (int index = 0; index < contacts; index++) {
                    int y = first + index * PITCH;
                    path.moveTo(SINGLE_LEAD, y).horizontalTo(SINGLE_PIN)
                            .box(SINGLE_CONTACT, y - SINGLE_CONTACT_HALF, SINGLE_LEAD, y + SINGLE_CONTACT_HALF);
                    pins.add(pin(index + 1, SINGLE_PIN, y));
                }
                title = family + ", " + counted;
                summary = "A single-row pin header of " + contacts + ", " + drawn
                        + ": each contact a pin leaving to the right, numbered as on the board";
                width = SINGLE_WIDTH;
                labelX = SINGLE_LEAD;
                nameY = first - SINGLE_NAME;
                valueY = last + SINGLE_VALUE;
            }
            case DOUBLE_ROW -> {
                path.box(-BODY_HALF, first - BODY_MARGIN, BODY_HALF, last + BODY_MARGIN);
                for (int index = 0; index < contacts; index++) {
                    int y = first + index * PITCH;
                    path.moveTo(-DOUBLE_PIN, y).horizontalTo(-BODY_HALF).moveTo(BODY_HALF, y).horizontalTo(DOUBLE_PIN);
                    pins.add(pin(2 * index + 1, -DOUBLE_PIN, y));
                    pins.add(pin(2 * index + 2, DOUBLE_PIN, y));
                }
                title = family + ", 2 x " + contacts;
                summary = "A two-row pin header of " + 2 * contacts + ", " + drawn
                        + ": odd contacts on the left, even on the right, numbered as on the board";
                width = DOUBLE_WIDTH;
                labelX = 0;
                nameY = first - BLOCK_NAME;
                valueY = last + BLOCK_VALUE;
            }
            default -> {
                path.box(-BODY_HALF, first - BODY_MARGIN, BODY_HALF, last + BODY_MARGIN);
                for (int index = 0; index < contacts; index++) {
                    int y = first + index * PITCH;
                    path.moveTo(SCREW_PIN, y).horizontalTo(-SCREW_RADIUS).circle(0, y, SCREW_RADIUS)
                            .moveTo(-SCREW_SLOT, y + SCREW_SLOT).lineTo(SCREW_SLOT, y - SCREW_SLOT);
                    pins.add(pin(index + 1, SCREW_PIN, y));
                }
                title = family + ", " + counted;
                summary = "A terminal block of " + contacts + " screw clamps, " + drawn
                        + ": each wire comes in from the left under its screw";
                width = SCREW_WIDTH;
                labelX = 0;
                nameY = first - BLOCK_NAME;
                valueY = last + BLOCK_VALUE;
            }
        }
        int height = PITCH * (contacts - 1) + MARGIN;
        DocumentNode document = new DocumentNode(SchematicModule.SYMBOL, title, slug, true, null, 1);
        document.addExpression(meta(summary, code, filing));
        document.addExpression(LineBuilder.line(DrawingGrammar.SIZE)
                .slot("extent", Nodes.extent(width, shielded ? height + PITCH : height)).comment(note).build());
        document.addExpression(path(path));
        pins.forEach(document::addExpression);
        if (shielded) {
            int left = style == Style.DOUBLE_ROW ? -SHIELD_DOUBLE_HALF : SHIELD_SINGLE_LEFT;
            int right = style == Style.DOUBLE_ROW ? SHIELD_DOUBLE_HALF : SHIELD_SINGLE_RIGHT;
            document.addExpression(path(MeasuredPath.path().box(left, first - SHIELD_MARGIN, right, last + SHIELD_MARGIN)
                    .moveTo(0, last + SHIELD_MARGIN).verticalTo(shieldPin)));
            document.addExpression(pin(SHIELD_PIN, 0, shieldPin, SHIELD_LABEL));
            valueY = shieldPin + SHIELD_VALUE;
        }
        document.addExpression(label("name", labelX, nameY));
        document.addExpression(label("value", labelX, valueY));
        return document;
    }

    private static MetaNode meta(String summary, String code, LibraryFiling filing) {
        MetaNode meta = new MetaNode();
        meta.addExpression(MetaLineNode.of(MetaNode.SUMMARY, Nodes.text(summary)));
        meta.addExpression(MetaLineNode.of(MetaNode.CODE, Nodes.text(code)));
        meta.addExpression(MetaLineNode.of(MetaNode.CATEGORY, Nodes.word(filing.category())));
        meta.addExpression(MetaLineNode.of(MetaNode.TAGS, Nodes.words(filing.tags())));
        meta.addExpression(MetaLineNode.of(MetaNode.AUTHORS, Nodes.text(filing.authors())));
        meta.addExpression(MetaLineNode.of(MetaNode.LICENSE, Nodes.text(filing.license())));
        return meta;
    }

    /** A path with no name and no properties: the drawing's strokes. */
    private static ClauseBlockNode path(MeasuredPath path) {
        return new ClauseBlockNode("path", LineBuilder.line(DrawingGrammar.element("path")).ending(new PathTextNode(path.build())).build());
    }

    /** A contact's pin, labelled by its number. */
    private static ClauseLineNode pin(int number, int x, int y) {
        return pin(PIN + number, x, y, String.valueOf(number));
    }

    private static ClauseLineNode pin(String name, int x, int y, String label) {
        return LineBuilder.line(DrawingGrammar.PIN).slot("name", Nodes.name(name)).clause("at", Nodes.point(x, y))
                .ending(Nodes.text(label)).build();
    }

    private static ClauseLineNode label(String subject, int x, int y) {
        return LineBuilder.line(DrawingGrammar.LABEL).slot("subject", Nodes.word(subject)).clause("at", Nodes.point(x, y)).build();
    }
}
