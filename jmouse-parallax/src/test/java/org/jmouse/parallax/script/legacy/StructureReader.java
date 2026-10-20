package org.jmouse.parallax.script.legacy;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.Block.WeightedMap;
import org.jmouse.parallax.script.ExperienceDocument;
import org.jmouse.parallax.script.SourceText;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Blocks only some kinds have: an iterated function system's maps, a Lindenmayer system's axiom, rules and turtle, a
 * construction's steps, a polytope's vertices and edges, a point list; and the catalogue's own: a collection's items
 * and sections, a course's lessons and steps, an instrument's object kinds, representations and the object it opens.
 */
final class StructureReader {

    private final Reading reading;

    StructureReader(Reading reading) {
        this.reading = reading;
    }

    /** {@code 0.85 : (0.85 x + 0.04 y, -0.04 x + 0.85 y + 1.6)}. */
    Block.Maps maps() {
        return new Block.Maps(reading.blockLines("maps").stream().map(line -> {
            int colon = line.indexOf(0, ":");
            if (colon < 1) {
                throw line.refusal("a map is 'weight : (x expression, y expression)'");
            }
            return new WeightedMap(line.text(0, colon), Values.tuple(line, colon + 1, line.size()));
        }).toList());
    }

    /** {@code axiom "F--F--F"}. */
    Block.Axiom axiom(Token keyword) {
        Reading.Line line = reading.line();
        if (line.size() != 1 || !line.isString(0)) {
            throw reading.refusal(keyword, "an axiom is 'axiom \"symbols\"'");
        }
        return new Block.Axiom(reading.literal(line.get(0)));
    }

    /** {@code F -> "F+F--F+F"}. */
    Block.Rules rules() {
        Map<String, String> rules = new LinkedHashMap<>();
        for (Reading.Line line : reading.blockLines("rules")) {
            int arrow = line.indexOf(0, "->");
            if (arrow < 1 || arrow != line.size() - 2 || !line.isString(line.size() - 1)) {
                throw line.refusal("a rule is 'symbol -> \"replacement\"'");
            }
            rules.put(String.join("", line.words(0, arrow)), reading.literal(line.get(line.size() - 1)));
        }
        return new Block.Rules(rules);
    }

    Block.Turtle turtle(PresentationReader presentation) {
        return new Block.Turtle(presentation.assignments("turtle"));
    }

    /** A construction's statements, each as written. */
    Block.Steps steps() {
        return new Block.Steps(reading.blockLines("steps").stream().map(line -> line.text(0)).toList());
    }

    /** {@code vertices = cartesian(-1, 1, dimension)}. */
    Block.Vertices vertices(Token keyword) {
        Reading.Line line = reading.line();
        if (line.size() < 2 || !line.value(0).equals("=")) {
            throw reading.refusal(keyword, "vertices are 'vertices = expression'");
        }
        return new Block.Vertices(line.text(1));
    }

    /** {@code edges where hamming(first, second) = 1}, or written out: {@code edges = [[0, 1], [1, 2]]}. */
    Block.Edges edges(Token keyword) {
        Reading.Line line = reading.line();
        if (line.size() >= 2 && line.value(0).equals("where")) {
            return new Block.Edges(Optional.of(line.text(1)), Optional.empty());
        }
        if (line.size() >= 2 && line.value(0).equals("=")) {
            return new Block.Edges(Optional.empty(), Optional.of(line.text(1)));
        }
        throw reading.refusal(keyword, "edges are 'edges where condition on first and second' or 'edges = [[0, 1], …]'");
    }

    /** {@code points { (0, 0), (1, 2) }}: points separated by commas or lines. */
    Block.PointList points() {
        List<List<SourceText>> points = new ArrayList<>();
        for (Reading.Line line : reading.blockLines("points")) {
            int start = 0;
            List<Integer> commas = new ArrayList<>(line.commas(0, line.size()));
            commas.add(line.size());
            for (int comma : commas) {
                if (comma > start) {
                    points.add(Values.tuple(line, start, comma));
                }
                start = comma + 1;
            }
        }
        return new Block.PointList(points);
    }

    // ─── catalogue ──────────────────────────────────────────────────────────────────────────────────

    /** {@code items lorenz, rossler, logistic-map}. */
    Block.Items items(Token keyword) {
        List<String> references = reading.line().names(0);
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
        for (Reading.Line line : reading.blockLines("section")) {
            if (!line.value(0).equals("items") || line.isString(0)) {
                throw line.refusal("a section holds 'items reference, reference, …'");
            }
            references.addAll(line.names(1));
        }
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
        Reading.Line head = reading.line();
        if (head.isEmpty() || !head.isString(0)) {
            throw reading.refusal(keyword, "a lesson is 'lesson \"Title\" [as slug] { … }'");
        }
        String title = reading.literal(head.get(0));
        String slug = ExperienceDocument.slugOf(title);
        if (head.size() > 1) {
            if (head.size() < 3 || !head.value(1).equals("as")) {
                throw head.refusal(1, "after a lesson's title comes 'as slug', then '{'");
            }
            slug = head.joinedWord(2);
        }
        reading.expect(BasicToken.T_OPEN_CURLY, "'{' to open lesson \"" + title + "\"");
        LessonFields fields = new LessonFields();
        List<Block.CourseStep> steps = new ArrayList<>();
        while (true) {
            reading.skipBlank();
            if (reading.atEnd()) {
                throw reading.refusal(keyword, "lesson \"" + title + "\" is opened with '{' and never closed");
            }
            if (reading.cursor.consumeIf(BasicToken.T_CLOSE_CURLY)) {
                break;
            }
            if (reading.atWord("step")) {
                steps.add(courseStep(reading.cursor.next()));
                continue;
            }
            Reading.Line line = reading.line();
            if (!line.isEmpty()) {
                fields.read(line);
            }
        }
        return new Block.Lesson(title, slug, fields.summary, fields.minutes, fields.object, steps);
    }

    /** What a lesson says about itself, gathered line by line; each is written once. */
    private final class LessonFields {
        Optional<String>  summary = Optional.empty();
        Optional<Integer> minutes = Optional.empty();
        Optional<String>  object  = Optional.empty();

        void read(Reading.Line line) {
            String key = line.value(0);
            switch (key) {
                case "summary" -> {
                    requireUnwritten(line, key, summary);
                    summary = Optional.of(text(line, 1));
                }
                case "minutes" -> {
                    requireUnwritten(line, key, minutes);
                    minutes = Optional.of(wholeNumber(line, 1));
                }
                case "object" -> {
                    requireUnwritten(line, key, object);
                    object = Optional.of(word(line, 1));
                }
                default -> throw line.refusal("a lesson holds summary, minutes, object and its steps, not '" + key + "'");
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
        for (Reading.Line line : reading.blockLines("step")) {
            int index = 0;
            while (index < line.size()) {
                index = fields.read(line, index);
            }
        }
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

        /** Reads the field that starts at {@code index}; returns where the next one starts. */
        int read(Reading.Line line, int index) {
            String key = line.value(index);
            switch (key) {
                case "object" -> {
                    requireUnwritten(line, key, object);
                    object = Optional.of(word(line, index + 1));
                    return line.joinedEnd(index + 1);
                }
                case "view" -> {
                    requireUnwritten(line, key, view);
                    if (index + 1 >= line.size()) {
                        throw line.refusal(index, "'view' names a representation: 'view contours'");
                    }
                    view = Optional.of(new Block.Representation(line.value(index + 1), false, line.text(index + 2)));
                    return line.size();
                }
                case "values" -> {
                    if (!values.isEmpty()) {
                        throw line.refusal(index, "'values' is written twice in this step");
                    }
                    values.putAll(PresentationReader.assignments(line, index + 1, "values"));
                    return line.size();
                }
                case "camera" -> {
                    requireUnwritten(line, key, camera);
                    camera = Optional.of(PresentationReader.camera(line, index + 1));
                    return line.size();
                }
                case "narration" -> {
                    requireUnwritten(line, key, narration);
                    narration = Optional.of(text(line, index + 1));
                    return index + 2;
                }
                case "task" -> {
                    requireUnwritten(line, key, task);
                    String question = text(line, index + 1);
                    if (line.value(index + 2).equals("answer") && !line.isString(index + 2)) {
                        task = Optional.of(new Block.Task(question, Optional.of(text(line, index + 3))));
                        return index + 4;
                    }
                    task = Optional.of(new Block.Task(question, Optional.empty()));
                    return index + 2;
                }
                default -> throw line.refusal(index, "a step holds object, view, values, camera, narration and task, "
                        + "not '" + key + "'");
            }
        }
    }

    /** {@code objects explicit_surface, parametric_surface}. */
    Block.ObjectKinds objectKinds(Token keyword) {
        List<String> kinds = reading.line().names(0);
        if (kinds.isEmpty()) {
            throw reading.refusal(keyword, "objects are 'objects kind, kind, …'");
        }
        return new Block.ObjectKinds(kinds);
    }

    /** {@code representations surface, normals, curvature}. */
    Block.Representations representations(Token keyword) {
        List<String> representations = reading.line().names(0);
        if (representations.isEmpty()) {
            throw reading.refusal(keyword, "representations are 'representations name, name, …'");
        }
        return new Block.Representations(representations);
    }

    /** {@code opens wave}. */
    Block.Opens opens(Token keyword) {
        Reading.Line line = reading.line();
        if (line.size() < 1 || line.joinedEnd(0) != line.size()) {
            throw reading.refusal(keyword, "'opens' names one object: 'opens wave'");
        }
        return new Block.Opens(line.joinedWord(0));
    }

    // ─── values inside a catalogue line ─────────────────────────────────────────────────────────────

    private String text(Reading.Line line, int index) {
        if (!line.isString(index)) {
            throw line.refusal(Math.max(0, index - 1), "'" + line.value(index - 1) + "' takes a text in quotes");
        }
        return reading.literal(line.get(index));
    }

    private static String word(Reading.Line line, int index) {
        if (index >= line.size() || line.isString(index)) {
            throw line.refusal(Math.max(0, index - 1), "'" + line.value(index - 1) + "' names an object: 'object wave'");
        }
        return line.joinedWord(index);
    }

    private static int wholeNumber(Reading.Line line, int index) {
        if (index + 1 != line.size() || !line.value(index).matches("\\d+")) {
            throw line.refusal(Math.max(0, index - 1), "'" + line.value(index - 1) + "' takes a whole number");
        }
        return Integer.parseInt(line.value(index));
    }

    private static void requireUnwritten(Reading.Line line, String key, Optional<?> field) {
        if (field.isPresent()) {
            throw line.refusal("'" + key + "' is written twice here");
        }
    }
}
