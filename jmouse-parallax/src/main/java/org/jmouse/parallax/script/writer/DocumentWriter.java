package org.jmouse.parallax.script.writer;

import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.ExperienceDocument;
import org.jmouse.parallax.script.Kind;
import org.jmouse.parallax.script.SourceText;

import java.util.List;

/**
 * Writes a jMX document from what it holds: the {@link ExperienceDocument} a reader builds, or one built by hand — a
 * KiCad symbol, a stub component — is written the same way, block by block, every line in the grammar its line
 * parser reads.
 *
 * <p>The tree is the truth and the text its rendering: a caller builds the document, never its text. An expression is
 * written as the reader kept it — the {@code SourceText} the author wrote —; a number the model holds as a number is
 * written so it reads back to the same {@code double}; a text is quoted so it reads back whole. Reading what this
 * writes gives back the same document, positions apart. What the model does not hold is not written: comments, and
 * the order of lines it keeps apart — a layout's wires among its parts, a drawing's plain paths among its elements.</p>
 */
public final class DocumentWriter {

    private DocumentWriter() {
    }

    /** One document, ending with a line break. */
    public static String write(ExperienceDocument document) {
        SourceLines lines = new SourceLines();
        lines.block(head(document), body -> {
            document.base().ifPresent(base -> body.line("extends " + base));
            if (!document.units().isEmpty()) {
                body.line("units " + Values.names(document.units()));
            }
            if (!document.uses().isEmpty()) {
                body.line("use " + Values.names(document.uses()));
            }
            PresentationWriter.meta(document.meta(), body);
            for (Block block : document.blocks()) {
                if (document.kind() == Kind.SYMBOL && block instanceof Block.Symbol drawing) {
                    // A symbol document is its drawing: its lines stand at the top of its body, not in a block.
                    SymbolWriter.drawing(drawing, body);
                } else if (block instanceof Block.Footprint land) {
                    // A footprint document is its pads and outlines, at the top of its body, as a symbol is its drawing.
                    FootprintWriter.drawing(land, body);
                } else {
                    block(block, body);
                }
            }
        });
        return lines.text();
    }

    /** Several documents of one file, a blank line between them. */
    public static String write(List<ExperienceDocument> documents) {
        return String.join("\n", documents.stream().map(DocumentWriter::write).toList());
    }

    /** {@code kind "Title" as slug [type role]}. */
    private static String head(ExperienceDocument document) {
        return document.kind().keyword() + " " + Values.text(document.title()) + " as " + document.slug()
                + document.type().map(type -> " type " + type).orElse("");
    }

    /** One block, by what it is; a new kind of block is a compile error here until it is written. */
    private static void block(Block block, SourceLines lines) {
        switch (block) {
            case Block.Parameters parameters ->
                    lines.block("parameters", inner -> parameters.parameters().forEach(parameter -> inner.line(DeclarationWriter.parameter(parameter))));
            case Block.Inputs inputs ->
                    lines.block("inputs", inner -> inputs.inputs().forEach(input -> inner.line(DeclarationWriter.input(input))));
            case Block.Outputs outputs ->
                    lines.block("outputs", inner -> outputs.outputs().forEach(output -> inner.line(DeclarationWriter.output(output))));
            case Block.Formulas formulas -> lines.verbatim("formulas", formulas.definitions().map(SourceText::text).orElse(""));
            case Block.State state ->
                    lines.block("state", inner -> state.variables().forEach(variable -> inner.line(DeclarationWriter.state(variable))));
            case Block.Equations equations ->
                    lines.block("equations", inner -> equations.equations().forEach(equation -> inner.line(DeclarationWriter.equation(equation))));
            case Block.Heat heat -> DeclarationWriter.heat(heat, lines);
            case Block.Image image -> DeclarationWriter.image(image, lines);
            case Block.TypeDefinition type -> lines.line(DeclarationWriter.type(type));
            case Block.Display display -> DeclarationWriter.display(display, lines);
            case Block.Measure measure -> DeclarationWriter.measure(measure, lines);
            case Block.Leads leads -> DeclarationWriter.leads(leads, lines);
            case Block.Pins pins -> DeclarationWriter.pins(pins, lines);
            case Block.Nodes nodes -> lines.block("nodes", inner -> nodes.nodes().forEach(node -> inner.line(DeclarationWriter.node(node))));
            case Block.Behaviour behaviour -> lines.verbatim("behaviour", behaviour.source().text());
            case Block.Run run -> lines.verbatim("run", run.source().text());
            case Block.View view -> PresentationWriter.view(view, lines);
            case Block.Symbol symbol -> SymbolWriter.symbol(symbol, lines);
            case Block.Expectation expectation -> CircuitWriter.expectation(expectation, lines);
            case Block.Bench bench -> CircuitWriter.sharedBench(bench, lines);
            case Block.Parts parts -> CircuitWriter.parts("parts", parts.parts(), lines);
            case Block.Nets nets -> CircuitWriter.nets(nets, lines);
            case Block.Layout layout -> CircuitWriter.layout(layout, lines);
            case Block.Ports ports -> CircuitWriter.ports(ports, lines);
            case Block.Probes probes -> CircuitWriter.probes(probes, lines);
            case Block.Analysis analysis -> lines.line(CircuitWriter.analysis(analysis));
            case Block.Sheet sheet -> lines.line(CircuitWriter.sheet(sheet));
            case Block.Footprint land -> FootprintWriter.drawing(land, lines);
            case Block.FootprintUse use -> lines.line("footprint " + use.footprint());
            case Block.Maps maps -> StructureWriter.maps(maps, lines);
            case Block.Axiom axiom -> lines.line("axiom " + Values.text(axiom.axiom()));
            case Block.Rules rules -> StructureWriter.rules(rules, lines);
            case Block.Turtle turtle -> StructureWriter.turtle(turtle, lines);
            case Block.Steps steps -> StructureWriter.steps(steps, lines);
            case Block.Vertices vertices -> lines.line("vertices = " + vertices.expression().text());
            case Block.Edges edges -> lines.line(StructureWriter.edges(edges));
            case Block.PointList points -> StructureWriter.points(points, lines);
            case Block.Items items -> lines.line("items " + Values.names(items.references()));
            case Block.Section section -> StructureWriter.section(section, lines);
            case Block.Lesson lesson -> StructureWriter.lesson(lesson, lines);
            case Block.CourseStep step -> StructureWriter.courseStep(step, lines);
            case Block.ObjectKinds kinds -> lines.line("objects " + Values.names(kinds.kinds()));
            case Block.Representations representations ->
                    lines.line("representations " + Values.names(representations.representations()));
            case Block.Opens opens -> lines.line("opens " + opens.reference());
            case Block.Clock clock -> lines.line(GraphWriter.clock(clock));
            case Block.Panes panes -> GraphWriter.panes(panes, lines);
            case Block.Lines graphLines -> GraphWriter.lines(graphLines, lines);
            case Block.GraphCheck check -> GraphWriter.check(check, lines);
        }
    }
}
