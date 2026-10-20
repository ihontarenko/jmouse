package org.jmouse.parallax.script.engine;

import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.ExperienceDocument;
import org.jmouse.parallax.script.ExperienceReader;
import org.jmouse.parallax.script.Kind;
import org.jmouse.parallax.script.form.DocumentForms;
import org.jmouse.parallax.script.form.DocumentOutline;
import org.jmouse.parallax.script.form.FormEdit;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ObjectDefinition.Circuit;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElement;
import org.jmouse.parallax.object.ObjectDefinition.PlacedItem;
import org.jmouse.parallax.object.ObjectDefinition.SchematicLayout;
import org.jmouse.parallax.object.ObjectDefinition.SchematicWire;
import org.jmouse.parallax.object.ParameterDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * What a canvas drew, written into the text it was drawn from — line by line, through the same edits a form makes
 * ({@link DocumentForms#apply}).
 *
 * <p>A canvas works on a definition: the schematic canvas on a circuit's parts and layout, the drawing canvas on a
 * symbol's lines. Saving one used to write the whole document again from that definition, and every comment, every
 * aligned column and every line the canvas never touched was rewritten. Here a line changes only when what it says
 * changed: a part whose element is equal keeps its text to the character, a moved part changes its one layout line.</p>
 */
final class CanvasTexts {

    private CanvasTexts() {
    }

    /** A head the canvas edits beside its drawing; a null field is left as written. */
    record Head(String title, String summary, String category) {
    }

    // ─── circuit ────────────────────────────────────────────────────────────────────────────────────

    static String circuit(String source, Circuit circuit, List<ParameterDefinition> parameters, Head head) {
        if (!(ExperienceDefinitions.read(source, "document").definition() instanceof Circuit current)) {
            throw new IllegalArgumentException("The schematic canvas draws a circuit; this text is not one.");
        }
        String text = head(source, head);

        Map<String, CircuitElement> before = current.elements().stream()
                .collect(Collectors.toMap(CircuitElement::id, element -> element));
        Map<String, String> parts = new LinkedHashMap<>();
        circuit.elements().forEach(element -> parts.put(element.id(), DefinitionWriter.part(element)));
        Set<String> unchangedParts = circuit.elements().stream()
                .filter(element -> element.equals(before.get(element.id()))).map(CircuitElement::id)
                .collect(Collectors.toSet());
        text = keyedLines(text, "parts", parts, unchangedParts::contains);

        if (circuit.layout() != null) {
            text = layout(text, current.layout(), circuit.layout());
        }
        if (!circuit.boundary().equals(current.boundary())) {
            text = boundary(text, circuit.boundary());
        }
        if (!circuit.probes().equals(current.probes()) || !Objects.equals(circuit.timePerDivision(), current.timePerDivision())) {
            text = probes(text, current, circuit);
        }
        if (!circuit.time().equals(current.time())) {
            text = analysis(text, DefinitionWriter.analysis(circuit.time()));
        }
        return declaredParameters(text, parameters);
    }

    private static String layout(String source, SchematicLayout before, SchematicLayout after) {
        Map<String, Object> was = new HashMap<>();
        if (before != null) {
            before.items().forEach(item -> was.put(item.id(), item));
            before.wires().forEach(wire -> was.put(wire.id(), wire));
        }
        Map<String, String> lines = new LinkedHashMap<>();
        Map<String, Object> now = new HashMap<>();
        for (PlacedItem item : after.items()) {
            lines.put(item.id(), DefinitionWriter.placed(item));
            now.put(item.id(), item);
        }
        for (SchematicWire wire : after.wires()) {
            lines.put(wire.id(), DefinitionWriter.wire(wire));
            now.put(wire.id(), wire);
        }
        return keyedLines(source, "layout", lines, key -> Objects.equals(was.get(key), now.get(key)));
    }

    /**
     * What crosses the sheet's edge, as the canvas drew it with its port and global labels. The {@code ports} block is
     * the sheet's interface and is written whole — its lines start with a direction, so no word keys them —, or goes
     * when the sheet hands nothing to a parent. In {@code nets} only the {@code global} word moves: a net's ground mark
     * and route stay as written.
     */
    private static String boundary(String source, ObjectDefinition.SheetBoundary boundary) {
        String text = source;
        int ports = statementOf(text, "ports");
        if (ports >= 0) {
            text = DocumentForms.apply(text, edit(FormEdit.Action.REMOVE_STATEMENT, ports, 0, Map.of()));
        }
        if (!boundary.ports().isEmpty()) {
            text = ensureBlock(text, "ports");
            for (ObjectDefinition.SheetPort port : boundary.ports()) {
                text = DocumentForms.apply(text, edit(FormEdit.Action.ADD_LINE, statementOf(text, "ports"), 0,
                        Map.of("line", DefinitionWriter.port(port))));
            }
        }

        List<Block.Net> written = ExperienceReader.readOne(text, "document").block(Block.Nets.class)
                .map(Block.Nets::nets).orElse(List.of());
        Map<String, Block.Net> nets = new LinkedHashMap<>();
        written.forEach(net -> nets.put(net.net(), new Block.Net(net.net(), net.ground(), boundary.globals().contains(net.net()),
                net.route())));
        boundary.globals().forEach(global -> nets.putIfAbsent(global, new Block.Net(global, false, true, List.of())));
        // A net line that says nothing once its global word is gone goes with it.
        nets.values().removeIf(net -> !net.ground() && !net.global() && net.route().isEmpty());
        if (nets.isEmpty() && statementOf(text, "nets") < 0) {
            return text;
        }
        Map<String, Block.Net> before = new HashMap<>();
        written.forEach(net -> before.put(net.net(), net));
        Map<String, String> lines = new LinkedHashMap<>();
        nets.forEach((name, net) -> lines.put(name, DefinitionWriter.net(net)));
        text = keyedLines(text, "nets", lines, name -> nets.get(name).equals(before.get(name)));
        int netsStatement = statementOf(text, "nets");
        if (nets.isEmpty() && netsStatement >= 0) {
            text = DocumentForms.apply(text, edit(FormEdit.Action.REMOVE_STATEMENT, netsStatement, 0, Map.of()));
        }
        return text;
    }

    /**
     * The bench's probes — the oscilloscope's channels and the meters —, a line each by its id: a probe as it was keeps
     * its line, comment and alignment included; a changed one is written again; one the bench no longer has goes.
     */
    private static String probes(String source, ObjectDefinition.Circuit current, ObjectDefinition.Circuit circuit) {
        Map<String, Object> was = new HashMap<>();
        current.probes().forEach(probe -> was.put(probe.id(), probe));
        Map<String, String> lines = new LinkedHashMap<>();
        Map<String, Object> now = new HashMap<>();
        for (ObjectDefinition.BenchProbe probe : circuit.probes()) {
            lines.put(probe.id(), DefinitionWriter.probe(probe));
            now.put(probe.id(), probe);
        }
        // The time base is the block's line keyed by its word, kept as written while it stays the same.
        String timeBase = DefinitionWriter.TIME_BASE;
        if (current.timePerDivision() != null) {
            was.put(timeBase, current.timePerDivision());
        }
        if (circuit.timePerDivision() != null) {
            lines.put(timeBase, DefinitionWriter.timeBase(circuit.timePerDivision()));
            now.put(timeBase, circuit.timePerDivision());
        }
        return keyedLines(source, "probes", lines, key -> Objects.equals(was.get(key), now.get(key)));
    }

    /** The span of the transient response: its {@code analysis transient} statement rewritten, or one added. */
    private static String analysis(String source, String written) {
        DocumentOutline outline = DocumentOutline.of(source);
        for (int index = 0; index < outline.statements().size(); index++) {
            DocumentOutline.Statement statement = outline.statements().get(index);
            if (statement.word().equals("analysis") && statement.header().of(source).startsWith("transient")
                    && statement.body().isEmpty()) {
                return DocumentForms.apply(source, edit(FormEdit.Action.SET_STATEMENT, index, 0, Map.of("line", written)));
            }
        }
        return DocumentForms.apply(source, new FormEdit(FormEdit.Action.ADD_STATEMENT, 0, 0, Map.of("line", written),
                "analysis"));
    }

    /** A parameter a new part value names gets its line; a parameter already written keeps its own. */
    private static String declaredParameters(String source, List<ParameterDefinition> parameters) {
        ExperienceDocument document = ExperienceReader.readOne(source, "document");
        Set<String> declared = document.parameters().stream().map(Block.Parameter::name).collect(Collectors.toSet());
        String text = source;
        for (ParameterDefinition parameter : parameters) {
            if (declared.contains(parameter.name())) {
                continue;
            }
            text = ensureBlock(text, "parameters");
            text = DocumentForms.apply(text, edit(FormEdit.Action.ADD_LINE, statementOf(text, "parameters"), 0,
                    Map.of("line", ObjectDocumentWriter.slider(parameter))));
        }
        return text;
    }

    // ─── symbol ─────────────────────────────────────────────────────────────────────────────────────

    /**
     * A drawing's lines in place of the ones written: the n-th line of the drawing replaces the n-th drawing statement
     * when they differ, extra lines are added after the last statement and missing ones removed.
     */
    static String symbol(String source, String drawing, Head head) {
        String text = head(source, head);
        List<String> lines = DefinitionWriter.drawingLines(drawing);
        int position = 0;
        while (true) {
            DocumentOutline outline = DocumentOutline.of(text);
            List<Integer> drawn = drawingStatements(outline);
            if (position < Math.min(lines.size(), drawn.size())) {
                DocumentOutline.Statement statement = outline.statements().get(drawn.get(position));
                if (!statement.span().of(text).strip().replaceAll("\\s+", " ").equals(lines.get(position))) {
                    text = DocumentForms.apply(text, edit(FormEdit.Action.SET_STATEMENT, drawn.get(position), 0,
                            Map.of("line", lines.get(position))));
                }
                position++;
            } else if (position < lines.size()) {
                text = DocumentForms.apply(text, new FormEdit(FormEdit.Action.ADD_STATEMENT, 0, 0,
                        Map.of("line", lines.get(position)), lines.get(position).split(" ", 2)[0]));
                position++;
            } else if (position < drawn.size()) {
                text = DocumentForms.apply(text, edit(FormEdit.Action.REMOVE_STATEMENT, drawn.getLast(), 0, Map.of()));
            } else {
                return text;
            }
        }
    }

    private static List<Integer> drawingStatements(DocumentOutline outline) {
        List<Integer> drawn = new ArrayList<>();
        for (int index = 0; index < outline.statements().size(); index++) {
            if (Kind.DRAWING_LINES.contains(outline.statements().get(index).word())
                    && outline.statements().get(index).body().isEmpty()) {
                drawn.add(index);
            }
        }
        return drawn;
    }

    // ─── shared ─────────────────────────────────────────────────────────────────────────────────────

    /** The title, and the summary and section lines of {@code meta}, where the canvas changed them. */
    private static String head(String source, Head head) {
        String text = source;
        if (head == null) {
            return text;
        }
        DocumentOutline outline = DocumentOutline.of(text);
        if (head.title() != null && !head.title().isBlank() && !outline.title().of(text).equals(head.title().strip())) {
            text = DocumentForms.apply(text, new FormEdit(FormEdit.Action.SET_TITLE, 0, 0, Map.of(), head.title()));
        }
        text = metaLine(text, "summary", head.summary());
        return metaLine(text, "category", head.category() == null ? null : head.category().toLowerCase(Locale.ROOT));
    }

    private static String metaLine(String source, String key, String value) {
        if (value == null || value.isBlank()) {
            return source;
        }
        String text = ensureBlock(source, "meta");
        int meta = statementOf(text, "meta");
        DocumentOutline.Statement statement = DocumentOutline.of(text).statements().get(meta);
        List<DocumentOutline.Line> lines = statement.body().orElseThrow().lines();
        for (int index = 0; index < lines.size(); index++) {
            if (firstWord(lines.get(index).content().of(text)).equals(key)) {
                return DocumentForms.apply(text, edit(FormEdit.Action.SET_LINE, meta, index, Map.of("key", key, "value", value)));
            }
        }
        return DocumentForms.apply(text, edit(FormEdit.Action.ADD_LINE, meta, 0, Map.of("key", key, "value", value)));
    }

    /**
     * A block's lines brought to the wanted ones, keyed by their first word: a line whose key is gone is removed, one
     * whose key is unchanged keeps its text, any other is rewritten or added at the block's end.
     */
    private static String keyedLines(String source, String block, Map<String, String> wanted, Predicate<String> unchanged) {
        String text = ensureBlock(source, block);
        boolean changed = true;
        while (changed) {
            changed = false;
            int statement = statementOf(text, block);
            List<DocumentOutline.Line> lines = DocumentOutline.of(text).statements().get(statement).body().orElseThrow().lines();
            for (int index = 0; index < lines.size(); index++) {
                String key = firstWord(lines.get(index).content().of(text));
                if (!wanted.containsKey(key)) {
                    text = DocumentForms.apply(text, edit(FormEdit.Action.REMOVE_LINE, statement, index, Map.of()));
                    changed = true;
                    break;
                }
            }
        }
        int statement = statementOf(text, block);
        List<DocumentOutline.Line> lines = DocumentOutline.of(text).statements().get(statement).body().orElseThrow().lines();
        Map<String, Integer> written = new HashMap<>();
        for (int index = 0; index < lines.size(); index++) {
            written.put(firstWord(lines.get(index).content().of(text)), index);
        }
        for (Map.Entry<String, String> line : wanted.entrySet()) {
            Integer index = written.get(line.getKey());
            if (index == null) {
                text = DocumentForms.apply(text, edit(FormEdit.Action.ADD_LINE, statement, 0, Map.of("line", line.getValue())));
            } else if (!unchanged.test(line.getKey())) {
                text = DocumentForms.apply(text, edit(FormEdit.Action.SET_LINE, statement, index, Map.of("line", line.getValue())));
            }
        }
        return text;
    }

    private static String ensureBlock(String source, String block) {
        if (statementOf(source, block) >= 0) {
            return source;
        }
        return DocumentForms.apply(source, new FormEdit(FormEdit.Action.ADD_STATEMENT, 0, 0, Map.of(), block));
    }

    private static int statementOf(String source, String word) {
        List<DocumentOutline.Statement> statements = DocumentOutline.of(source).statements();
        for (int index = 0; index < statements.size(); index++) {
            if (statements.get(index).word().equals(word) && statements.get(index).body().isPresent()) {
                return index;
            }
        }
        return -1;
    }

    private static String firstWord(String line) {
        return line.strip().split("\\s+", 2)[0];
    }

    private static FormEdit edit(FormEdit.Action action, int statement, int line, Map<String, String> values) {
        return new FormEdit(action, statement, line, values, null);
    }
}
