package org.jmouse.parallax.script.reader;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.Block.WeightedMap;
import org.jmouse.parallax.script.ExperienceDocument;
import org.jmouse.parallax.script.SourceText;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Blocks only some kinds have: an iterated function system's maps, a Lindenmayer system's axiom, rules and turtle, a
 * construction's steps, a polytope's vertices and edges, a point list; and the catalogue's own: a collection's items
 * and sections, a course's lessons and steps, an instrument's object kinds, representations and the object it opens.
 */
final class StructureReader {

    private final Reading reading;
    private final JmxValues values;
    private final PresentationReader presentation;

    /**
     * @param presentation reads the assignments and the camera a turtle or a lesson's step writes, as a preset does
     */
    StructureReader(Reading reading, PresentationReader presentation) {
        this.reading = reading;
        this.values = new JmxValues(reading);
        this.presentation = presentation;
    }

    /** Each line of a block of lines, read where it stands once the block's structure is known. */
    private void eachLine(String block, Consumer<Token> line) {
        List<TokenCursor.Savepoint> starts = reading.lineStarts(block);
        TokenCursor.Savepoint after = reading.cursor.savepoint();
        for (TokenCursor.Savepoint start : starts) {
            reading.cursor.restore(start);
            line.accept(reading.cursor.current());
        }
        reading.cursor.restore(after);
    }

    /** {@code 0.85 : (0.85 x + 0.04 y, -0.04 x + 0.85 y + 1.6)}. */
    Block.Maps maps() {
        List<WeightedMap> maps = new ArrayList<>();
        eachLine("maps", first -> {
            if (reading.lineView().offsetOf(0, ":") < 1) {
                throw reading.refusal(first, "a map is 'weight : (x expression, y expression)'");
            }
            SourceText weight = values.expression(Set.of(":"));
            reading.cursor.next();
            maps.add(new WeightedMap(weight, values.tuple(Set.of())));
        });
        return new Block.Maps(maps);
    }

    /** {@code axiom "F--F--F"}. */
    Block.Axiom axiom(Token keyword) {
        TokenCursor line = reading.lineView();
        if (line.remaining() != 1 || !reading.isString(line.lookAt(0))) {
            throw reading.refusal(keyword, "an axiom is 'axiom \"symbols\"'");
        }
        return new Block.Axiom(reading.literal(reading.cursor.next()));
    }

    /** {@code F -> "F+F--F+F"}. */
    Block.Rules rules() {
        Map<String, String> rules = new LinkedHashMap<>();
        eachLine("rules", first -> {
            TokenCursor line = reading.lineView();
            int arrow = line.offsetOf(0, "->");
            int length = line.remaining();
            if (arrow < 1 || arrow != length - 2 || !reading.isString(line.lookAt(length - 1))) {
                throw reading.refusal(first, "a rule is 'symbol -> \"replacement\"'");
            }
            StringBuilder symbol = new StringBuilder();
            for (int offset = 0; offset < arrow; offset++) {
                symbol.append(reading.isString(line.lookAt(offset)) ? reading.literal(line.lookAt(offset)) : line.valueAt(offset));
            }
            rules.put(symbol.toString(), reading.literal(line.lookAt(length - 1)));
        });
        return new Block.Rules(rules);
    }

    Block.Turtle turtle() {
        return new Block.Turtle(presentation.assignments("turtle"));
    }

    /**
     * A construction's statements, each its whole line as written — {@code A = point(ax, 0) "A"} is a statement of the
     * construction's own language, labelled, not one expression of jMF's.
     */
    Block.Steps steps() {
        List<SourceText> steps = new ArrayList<>();
        eachLine("steps", first -> {
            TokenCursor line = reading.lineView();
            steps.add(reading.text(first.offset(), line.lookAt(line.remaining()).offset()));
            reading.skipLine();
        });
        return new Block.Steps(steps);
    }

    /** {@code vertices = cartesian(-1, 1, dimension)}. */
    Block.Vertices vertices(Token keyword) {
        if (reading.lineView().remaining() < 2 || !reading.cursor.checkValueAt(0, "=")) {
            throw reading.refusal(keyword, "vertices are 'vertices = expression'");
        }
        reading.cursor.next();
        return new Block.Vertices(restOfLine());
    }

    /** {@code edges where hamming(first, second) = 1}, or written out: {@code edges = [[0, 1], [1, 2]]}. */
    Block.Edges edges(Token keyword) {
        boolean twoTokensAtLeast = reading.lineView().remaining() >= 2;
        if (twoTokensAtLeast && reading.cursor.checkValueAt(0, "where")) {
            reading.cursor.next();
            return new Block.Edges(Optional.of(restOfLine()), Optional.empty());
        }
        if (twoTokensAtLeast && reading.cursor.checkValueAt(0, "=")) {
            reading.cursor.next();
            return new Block.Edges(Optional.empty(), Optional.of(restOfLine()));
        }
        throw reading.refusal(keyword, "edges are 'edges where condition on first and second' or 'edges = [[0, 1], …]'");
    }

    /** {@code points { (0, 0), (1, 2) }}: points separated by commas or lines, each its coordinates. */
    Block.PointList points() {
        List<List<SourceText>> points = new ArrayList<>();
        eachLine("points", first -> {
            while (!reading.atLineEnd()) {
                if (reading.cursor.isCurrent(BasicToken.T_COMMA)) {
                    reading.cursor.next();
                    continue;
                }
                points.add(values.tuple(Set.of(",")));
            }
        });
        return new Block.PointList(points);
    }

    /** The rest of the line, read as one expression of jMF's: a construction's statement, a condition, a list. */
    private SourceText restOfLine() {
        SourceText expression = values.expression(JmxValues.LINE);
        reading.requireLineEnd();
        return expression;
    }

    // ─── catalogue ──────────────────────────────────────────────────────────────────────────────────

    /** {@code items lorenz, rossler, logistic-map}. */
    Block.Items items(Token keyword) {
        List<String> references = names();
        if (references.isEmpty()) {
            throw reading.refusal(keyword, "items are 'items reference, reference, …'");
        }
        return new Block.Items(references);
    }

    /**
     * {@code section "Ordinary" { items line, parabola }}: a titled part of a collection. Its {@code items} may run
     * over several lines, each opening with the word.
     */
    Block.Section section(Token keyword) {
        Token title = reading.expect(BasicToken.T_STRING, "the section's title in quotes: 'section \"Ordinary\" { items … }'");
        List<String> references = new ArrayList<>();
        eachLine("section", first -> {
            if (!reading.cursor.checkValueAt(0, "items")) {
                throw reading.refusal(first, "a section holds 'items reference, reference, …'");
            }
            reading.cursor.next();
            references.addAll(names());
        });
        if (references.isEmpty()) {
            throw reading.refusal(keyword, "section \"" + reading.literal(title) + "\" holds no items");
        }
        return new Block.Section(reading.literal(title), references);
    }

    /**
     * {@code lesson "The road to chaos" as logistic-road-to-chaos { summary "…"; minutes 10; object logistic-map;
     * step "Orbit" { … } }}.
     */
    Block.Lesson lesson(Token keyword) {
        TokenCursor cursor = reading.cursor;
        TokenCursor head = reading.lineView();
        if (head.remaining() == 0 || !reading.isString(head.lookAt(0))) {
            throw reading.refusal(keyword, "a lesson is 'lesson \"Title\" [as slug] { … }'");
        }
        String title = reading.literal(cursor.next());
        String slug = ExperienceDocument.slugOf(title);
        if (!reading.atLineEnd()) {
            TokenCursor rest = reading.lineView();
            if (rest.remaining() < 2 || !rest.checkValueAt(0, "as")) {
                throw reading.refusal(cursor.current(), "after a lesson's title comes 'as slug', then '{'");
            }
            cursor.next();
            slug = reading.joinedWord();
            reading.skipLine();
        }
        LessonFields fields = new LessonFields();
        List<Block.CourseStep> steps = new ArrayList<>();
        reading.body("'{' to open lesson \"" + title + "\"",
                () -> reading.refusal(keyword, "lesson \"" + title + "\" is opened with '{' and never closed"),
                () -> {
                    if (reading.cursor.isCurrent(BasicToken.T_OPEN_CURLY)) {
                        throw reading.refusal("'lesson' holds lines, not a nested '{ … }'");
                    }
                    if (reading.atWord("step")) {
                        steps.add(courseStep(reading.cursor.next()));
                    } else {
                        fields.read();
                    }
                });
        return new Block.Lesson(title, slug, fields.summary, fields.minutes, fields.object, steps);
    }

    /** What a lesson says about itself, gathered line by line; each is written once. */
    private final class LessonFields {
        Optional<String>  summary = Optional.empty();
        Optional<Integer> minutes = Optional.empty();
        Optional<String>  object  = Optional.empty();

        void read() {
            Token first = reading.cursor.current();
            String key = first.value();
            switch (key) {
                case "summary" -> {
                    requireUnwritten(first, key, summary);
                    reading.cursor.next();
                    summary = Optional.of(text(first));
                    reading.skipLine();
                }
                case "minutes" -> {
                    requireUnwritten(first, key, minutes);
                    reading.cursor.next();
                    minutes = Optional.of(wholeNumber(first));
                }
                case "object" -> {
                    requireUnwritten(first, key, object);
                    reading.cursor.next();
                    object = Optional.of(word(first));
                    reading.skipLine();
                }
                default -> throw reading.refusal(first, "a lesson holds summary, minutes, object and its steps, not '"
                        + key + "'");
            }
        }
    }

    /**
     * {@code step "Contours" { object wave; view contours; values A = 1, k = 1; narration "…"; task "…" answer "…" }}.
     * One line may hold several fields — {@code object wave view contours} — since {@code view}, {@code values} and
     * {@code camera} run to the end of the line and the others take one value.
     */
    Block.CourseStep courseStep(Token keyword) {
        Token title = reading.expect(BasicToken.T_STRING, "the step's title in quotes");
        StepFields fields = new StepFields();
        eachLine("step", first -> {
            while (!reading.atLineEnd()) {
                fields.read(first);
            }
        });
        return new Block.CourseStep(reading.literal(title), fields.object, fields.view, fields.values, fields.camera,
                fields.narration, fields.task);
    }

    /** What a step says, gathered field by field; each is written once. */
    private final class StepFields {
        Optional<String>               object    = Optional.empty();
        Optional<Block.Representation> view      = Optional.empty();
        Map<String, SourceText>        values    = new LinkedHashMap<>();
        Optional<Block.Camera>         camera    = Optional.empty();
        Optional<String>               narration = Optional.empty();
        Optional<Block.Task>           task      = Optional.empty();

        /** Reads the field the cursor stands on; the line's first token is where a field written twice is refused. */
        void read(Token first) {
            TokenCursor cursor = reading.cursor;
            Token keyword = cursor.next();
            String key = keyword.value();
            switch (key) {
                case "object" -> {
                    requireUnwritten(first, key, object);
                    object = Optional.of(word(keyword));
                }
                case "view" -> {
                    requireUnwritten(first, key, view);
                    if (reading.atLineEnd()) {
                        throw reading.refusal(keyword, "'view' names a representation: 'view contours'");
                    }
                    String name = cursor.next().value();
                    int start = reading.cursor.endOfRead();
                    TokenCursor rest = reading.lineView();
                    SourceText settings = reading.text(start, rest.lookAt(rest.remaining()).offset());
                    reading.skipLine();
                    view = Optional.of(new Block.Representation(name, false, settings));
                }
                case "values" -> {
                    if (!values.isEmpty()) {
                        throw reading.refusal(keyword, "'values' is written twice in this step");
                    }
                    values.putAll(presentation.assignmentsOnLine("values"));
                }
                case "camera" -> {
                    requireUnwritten(first, key, camera);
                    camera = Optional.of(presentation.camera());
                }
                case "narration" -> {
                    requireUnwritten(first, key, narration);
                    narration = Optional.of(text(keyword));
                }
                case "task" -> {
                    requireUnwritten(first, key, task);
                    String question = text(keyword);
                    if (!reading.atLineEnd() && cursor.checkValueAt(0, "answer")) {
                        Token answer = cursor.next();
                        task = Optional.of(new Block.Task(question, Optional.of(text(answer))));
                    } else {
                        task = Optional.of(new Block.Task(question, Optional.empty()));
                    }
                }
                default -> throw reading.refusal(keyword, "a step holds object, view, values, camera, narration and task, "
                        + "not '" + key + "'");
            }
        }
    }

    /** {@code objects explicit_surface, parametric_surface}. */
    Block.ObjectKinds objectKinds(Token keyword) {
        List<String> kinds = names();
        if (kinds.isEmpty()) {
            throw reading.refusal(keyword, "objects are 'objects kind, kind, …'");
        }
        return new Block.ObjectKinds(kinds);
    }

    /** {@code representations surface, normals, curvature}. */
    Block.Representations representations(Token keyword) {
        List<String> representations = names();
        if (representations.isEmpty()) {
            throw reading.refusal(keyword, "representations are 'representations name, name, …'");
        }
        return new Block.Representations(representations);
    }

    /** {@code opens wave}. */
    Block.Opens opens(Token keyword) {
        if (reading.atLineEnd()) {
            throw reading.refusal(keyword, "'opens' names one object: 'opens wave'");
        }
        String object = reading.joinedWord();
        if (!reading.atLineEnd()) {
            throw reading.refusal(keyword, "'opens' names one object: 'opens wave'");
        }
        return new Block.Opens(object);
    }

    // ─── values inside a catalogue line ─────────────────────────────────────────────────────────────

    /** Names separated by commas, each one word, to the end of the line. */
    private List<String> names() {
        List<String> names = new ArrayList<>();
        while (!reading.atLineEnd()) {
            if (reading.cursor.isCurrent(BasicToken.T_COMMA)) {
                reading.cursor.next();
                continue;
            }
            names.add(reading.joinedWord());
        }
        return names;
    }

    /** The text in quotes after a key. */
    private String text(Token key) {
        if (reading.atLineEnd() || !reading.cursor.isCurrent(BasicToken.T_STRING)) {
            throw reading.refusal(key, "'" + key.value() + "' takes a text in quotes");
        }
        return reading.literal(reading.cursor.next());
    }

    /** The object a key names: one word. */
    private String word(Token key) {
        if (reading.atLineEnd() || reading.cursor.isCurrent(BasicToken.T_STRING)) {
            throw reading.refusal(key, "'" + key.value() + "' names an object: 'object wave'");
        }
        return reading.joinedWord();
    }

    /** The whole number after a key, alone on the rest of its line. */
    private int wholeNumber(Token key) {
        TokenCursor rest = reading.lineView();
        if (rest.remaining() != 1 || !rest.valueAt(0).matches("\\d+")) {
            throw reading.refusal(key, "'" + key.value() + "' takes a whole number");
        }
        return Integer.parseInt(reading.cursor.next().value());
    }

    private void requireUnwritten(Token first, String key, Optional<?> field) {
        if (field.isPresent()) {
            throw reading.refusal(first, "'" + key + "' is written twice here");
        }
    }
}
