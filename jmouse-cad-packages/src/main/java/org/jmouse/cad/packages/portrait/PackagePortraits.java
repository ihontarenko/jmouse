package org.jmouse.cad.packages.portrait;

import org.jmouse.cad.document.PathTextNode;
import org.jmouse.cad.document.TechnicalClass;
import org.jmouse.cad.packages.LibraryFiling;
import org.jmouse.cad.packages.MeasuredPath;
import org.jmouse.cad.packages.PackageTable;
import org.jmouse.cad.schematic.DrawingGrammar;
import org.jmouse.el.grammar.ClauseBlockNode;
import org.jmouse.el.grammar.ClauseLineNode;
import org.jmouse.el.grammar.LineBuilder;
import org.jmouse.el.grammar.Nodes;
import org.jmouse.el.grammar.FormulaNode;
import org.jmouse.el.grammar.AssignedNode;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Trivia;
import org.jmouse.sheet.Sheet;
import org.jmouse.sheet.SheetEntry;
import org.jmouse.sheet.SheetValue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.jmouse.cad.packages.Measures.exact;

/**
 * Packages as they are held, generated: every member of a package table whose form the portrait rules draw — two even
 * rows of leads —, drawn from above, pin 1 at the bottom left beside the notch and the dot, counted along the bottom
 * and back along the top. A pin is named by its number, as a footprint's pad is, so a part is wired to it by its pins'
 * package numbers; its code is its package's designation as a portrait's, {@code PT.SOIC:8-P1.27-W3.9} beside
 * {@code FP.SOIC:8-P1.27-W3.9}.
 */
public final class PackagePortraits {

    private static final String PATH  = "path";
    private static final String GLYPH = "glyph";

    /** How a form's leads leave the body seen from above. */
    private enum Leads {
        /** A dual in-line lead: its wide shoulder out to the row, where it bends down, then the narrow lead into the hole. */
        THROUGH_HOLE("dual-inline"),
        /** A gull wing: one flat lead from the body out to its foot on the board. */
        GULL_WING("gull-wing");

        private final String form;

        Leads(String form) {
            this.form = form;
        }

        static Optional<Leads> of(String form) {
            for (Leads leads : values()) {
                if (leads.form.equals(form)) {
                    return Optional.of(leads);
                }
            }
            return Optional.empty();
        }
    }

    private PackagePortraits() {
    }

    /** Every portrait the tables give: each member of each outline of a form the rules draw. */
    public static List<PackagePortrait> generate(PortraitRules rules, LibraryFiling filing, List<Sheet> tables) {
        List<PackagePortrait> portraits = new ArrayList<>();
        for (Sheet table : tables) {
            Optional<String> form = table.field("form").map(field -> field.value() instanceof SheetValue.Word(String word) ? word : "");
            Optional<Leads> leads = form.flatMap(Leads::of);
            Optional<String> mounting = form.flatMap(rules::mounting);
            if (leads.isEmpty() || mounting.isEmpty()) {
                continue;
            }
            for (SheetEntry entry : table.entries()) {
                // An outline that places its leads one by one (SOT-23) is not two even rows.
                if (entry.value("left").isPresent()) {
                    continue;
                }
                PackageTable outline = new PackageTable(entry);
                for (Map.Entry<Integer, Double> member : outline.members("members").entrySet()) {
                    portraits.add(portrait(table, outline, leads.get(), mounting.get(), member.getKey(), member.getValue(),
                            rules, filing));
                }
            }
        }
        return portraits;
    }

    private static PackagePortrait portrait(Sheet table, PackageTable outline, Leads leads, String mounting, int count,
                                            double bodyLength, PortraitRules rules, LibraryFiling filing) {
        double grid = rules.grid();
        double pitch = outline.length("pitch");
        double scale = grid / pitch;
        int perRow = count / 2;
        double halfBody = outline.length("body-width") * scale / 2;
        double halfLength = bodyLength * scale / 2;
        // Where the leads end across the body: the row of holes, or the middle of the span tip to tip.
        double reach = (leads == Leads.THROUGH_HOLE ? outline.length("row")
                : (outline.span("span").minimum() + outline.span("span").maximum()) / 2) * scale / 2;
        double tip = Math.ceil(Math.max(reach, halfBody + rules.shortestLead()) / grid) * grid;
        // Pins stand on the grid: with an even count a row cannot be centred on the origin, so the body moves half a pitch.
        double first = -Math.floor(perRow / 2.0) * grid;
        double centre = first + (perRow - 1) * grid / 2;
        List<Expression> statements = new ArrayList<>();
        MeasuredPath legs = MeasuredPath.path();
        for (int index = 0; index < perRow; index++) {
            double x = first + index * grid;
            leg(legs, leads, outline, scale, x, halfBody, reach, tip, 1);
            leg(legs, leads, outline, scale, x, halfBody, reach, tip, -1);
        }
        ClauseBlockNode leadsElement = path("leads", legs, rules.style("leads"));
        leadsElement.addLeadingTrivia(List.of(Trivia.blank()));
        statements.add(leadsElement);
        for (int index = 0; index < perRow; index++) {
            statements.add(pin(index + 1, first + index * grid, tip));
        }
        for (int index = 0; index < perRow; index++) {
            statements.add(pin(perRow + index + 1, first + (perRow - 1 - index) * grid, -tip));
        }
        double corner = Math.min(halfBody, halfLength) * rules.corner();
        statements.add(path("body", roundedRectangle(centre, halfLength, halfBody, corner), rules.style("body")));
        double left = centre - halfLength;
        if (leads == Leads.THROUGH_HOLE) {
            double notch = halfBody * rules.notch();
            statements.add(path("notch", MeasuredPath.path().moveTo(left, -notch).arcTo(notch, false, true, left, notch).close(),
                    rules.style("mark")));
        }
        double dot = halfBody * rules.dot();
        double inset = rules.dotInset() * dot;
        statements.add(path("pin_one_dot", MeasuredPath.path().circle(left + inset, halfBody - inset, dot), rules.style("mark")));
        statements.add(marking(centre, 2 * halfLength * rules.markingWidth(), halfBody * rules.markingHeight(), rules));
        String name = outline.word("name");
        String suffix = outline.optionalWord("suffix").map(word -> "-" + word).orElse("");
        String title = name.toUpperCase() + "-" + count + outline.optionalWord("suffix").map(word -> ", " + word).orElse("");
        String summary = count + "-lead " + outline.title() + " from above: pin 1 at the bottom left, beside the "
                + (leads == Leads.THROUGH_HOLE ? "notch and " : "") + "dot";
        String note = "Generated from footprint-standards/" + table.slug() + ".sht#" + outline.id() + "; regenerate, never edit.";
        Map<String, String> words = new LinkedHashMap<>(outline.words());
        words.put("leads", String.valueOf(count));
        String code = TechnicalClass.PORTRAIT.code(PackageTable.designated(outline.word("designation"), words));
        return new PackagePortrait(name + "-" + count + suffix + "-portrait", code, title, summary, outline.word("folder"),
                mounting, outline.standard(), filing, note, Math.ceil(2 * Math.max(Math.abs(left), centre + halfLength)),
                2 * tip, rules.markingInput(), statements);
    }

    /** A pin where a sheet wires it, named by its number in the package. */
    private static ClauseLineNode pin(int number, double x, double y) {
        return LineBuilder.line(DrawingGrammar.PIN).slot("name", Nodes.name(String.valueOf(number)))
                .clause("at", Nodes.point(exact(x), exact(y))).build();
    }

    /** A named path, painted as its style says. */
    private static ClauseBlockNode path(String name, MeasuredPath path, List<PortraitRules.Property> style) {
        ClauseLineNode head = LineBuilder.line(DrawingGrammar.element(PATH)).slot("name", Nodes.name(name))
                .ending(new PathTextNode(path.build())).build();
        return element(PATH, head, style.stream().map(property -> property(property.name(), new FormulaNode(property.written())))
                .toList());
    }

    /** What the part prints on its body — its own words, from its component's binding —, across the body's middle. */
    private static ClauseBlockNode marking(double centre, double width, double height, PortraitRules rules) {
        ClauseLineNode head = LineBuilder.line(DrawingGrammar.element(GLYPH)).slot("name", Nodes.name("printed_marking"))
                .clause("at", Nodes.point(exact(centre), 0)).clause("size", Nodes.extent(exact(width), exact(height))).build();
        List<ClauseLineNode> properties = new ArrayList<>(rules.style("marking").stream()
                .map(property -> property(property.name(), new FormulaNode(property.written()))).toList());
        properties.add(property("text", Nodes.formula(Nodes.name(rules.markingInput()))));
        return element(GLYPH, head, properties);
    }

    private static ClauseBlockNode element(String shape, ClauseLineNode head, List<ClauseLineNode> properties) {
        ClauseBlockNode element = new ClauseBlockNode(shape, head);
        element.open();
        properties.forEach(element::addExpression);
        return element;
    }

    /** {@code fill = silver}. */
    private static ClauseLineNode property(String name, FormulaNode value) {
        return LineBuilder.line(DrawingGrammar.PROPERTY).slot("name", Nodes.name(name)).slot("value", new AssignedNode(value)).build();
    }

    /** One lead from the body's edge out to its pin on the given side ({@code 1} the bottom, {@code -1} the top). */
    private static void leg(MeasuredPath legs, Leads leads, PackageTable outline, double scale, double x, double halfBody,
                            double reach, double tip, int side) {
        if (leads == Leads.THROUGH_HOLE) {
            double shoulder = outline.length("shoulder") * scale / 2;
            double lead = outline.length("lead") * scale / 2;
            strip(legs, x, shoulder, halfBody * side, reach * side);
            strip(legs, x, lead, reach * side, tip * side);
            return;
        }
        PackageTable.Span width = outline.span("lead");
        strip(legs, x, (width.minimum() + width.maximum()) / 2 * scale / 2, halfBody * side, tip * side);
    }

    /** A strip along the y axis, {@code halfWidth} either side of {@code x}, from {@code from} to {@code to}. */
    private static void strip(MeasuredPath legs, double x, double halfWidth, double from, double to) {
        legs.box(x - halfWidth, from, x + halfWidth, to);
    }

    private static MeasuredPath roundedRectangle(double centre, double halfLength, double halfWidth, double corner) {
        double left = centre - halfLength;
        double right = centre + halfLength;
        return MeasuredPath.path().moveTo(left + corner, -halfWidth).horizontalTo(right - corner)
                .arcTo(corner, false, true, right, -halfWidth + corner).verticalTo(halfWidth - corner)
                .arcTo(corner, false, true, right - corner, halfWidth).horizontalTo(left + corner)
                .arcTo(corner, false, true, left, halfWidth - corner).verticalTo(-halfWidth + corner)
                .arcTo(corner, false, true, left + corner, -halfWidth).close();
    }
}
