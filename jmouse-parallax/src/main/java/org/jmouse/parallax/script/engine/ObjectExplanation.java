package org.jmouse.parallax.script.engine;

import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.Capability;
import org.jmouse.parallax.script.ExperienceDocument;
import org.jmouse.parallax.script.ExperienceReader;
import org.jmouse.parallax.script.Kind;
import org.jmouse.parallax.script.form.DocumentForm;
import org.jmouse.parallax.script.form.DocumentForms;
import org.jmouse.parallax.script.form.DocumentOutline;
import org.jmouse.parallax.script.form.FormField;
import org.jmouse.parallax.object.ObjectKind;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * What a page shows about an object besides its picture: its structure — the inputs, parameters, state and outputs as
 * written — and everything derived from that structure, each with the one line of the text it follows from
 * (DECISIONS §15). Nothing here is editable; a reader changes the structure and the derived changes with it.
 *
 * <p>This is the one place an object's kind becomes words for a reader: which engine computes it, how the page presents
 * it, and why. A page renders the explanation and never branches on a kind itself.</p>
 *
 * @param structure    the blocks a reader sets and reads — inputs, parameters, state, outputs — as form lines
 * @param engine       how it is computed
 * @param presentation how the page presents it: {@code dataset}, {@code instrument}, {@code drawing} or {@code part}
 * @param view         the representation it is shown in first
 * @param space        how many dimensions it lives in, and which names of the structure make them — why a page draws
 *                     it in Space or on the Plane
 * @param capabilities what it can do, each with the line that gives it
 */
public record ObjectExplanation(List<DocumentForm.StatementForm> structure, Derived engine, Derived presentation,
                                Derived view, Derived space, List<Derived> capabilities) {

    /** Blocks the structure panel shows, in its order. */
    private static final List<String> STRUCTURE = List.of("inputs", "parameters", "state", "outputs");

    public ObjectExplanation {
        structure = List.copyOf(structure);
        capabilities = List.copyOf(capabilities);
    }

    /**
     * One derived fact.
     *
     * @param value   what it is, as a stable word: an engine's name, {@code run}, {@code dataset}
     * @param label   the same in words for a reader
     * @param because the line of the text it follows from, as written; empty when it is a default
     * @param line    that line's number, or 0
     */
    public record Derived(String value, String label, String because, int line) {
    }

    static ObjectExplanation of(String source, DefinedObject defined) {
        ExperienceDocument document = ExperienceReader.readOne(source, "document");
        DocumentOutline outline = DocumentOutline.of(source);
        Lines lines = new Lines(source, outline);
        List<DocumentForm.StatementForm> structure = DocumentForms.form(source).statements().stream()
                .filter(statement -> STRUCTURE.contains(statement.word()))
                .sorted(Comparator.comparingInt(statement -> STRUCTURE.indexOf(statement.word())))
                .toList();
        Set<Capability> capabilities = document.capabilities();
        ObjectKind kind = defined.definition().objectKind();
        return new ObjectExplanation(structure, engine(kind, document, capabilities, lines),
                presentation(document, lines), view(defined, lines),
                space(defined.definition().ambientDimension(), structure), capabilities(capabilities, lines));
    }

    // ─── engine ─────────────────────────────────────────────────────────────────────────────────────

    private static Derived engine(ObjectKind kind, ExperienceDocument document, Set<Capability> capabilities, Lines lines) {
        boolean random = capabilities.contains(Capability.RESAMPLE);
        boolean exact = document.parameters().stream().anyMatch(parameter -> parameter.has(Block.ParameterFlag.EXACT));
        String label = switch (kind) {
            case EXPLICIT_CURVE -> "Explicit sampler: the output is evaluated across the input's range";
            case EXPLICIT_SURFACE -> "Height-field sampler: the output is evaluated on a grid of both inputs";
            case IMPLICIT_CURVE -> "Contour tracer: the points where the equation holds, found cell by cell";
            case IMPLICIT_SURFACE, IMPLICIT_HYPERSURFACE -> "Marching cubes: the level set of the equation in space";
            case PARAMETRIC_CURVE -> "Parametric sampler: each coordinate evaluated along the input";
            case PARAMETRIC_SURFACE -> "Parametric sampler: each coordinate evaluated on the input square";
            case VECTOR_FIELD -> "Field sampler: the vector evaluated at grid points";
            case DIFFERENTIAL_SYSTEM -> random ? "Euler–Maruyama: sample paths of a stochastic derivative"
                    : "ODE integrator: adaptive Runge–Kutta from the declared state";
            case DISCRETE_MAP -> random ? "Random iteration: sample paths, one draw per step"
                    : "Iterated map: the state stepped from where it starts";
            case COMPLEX_MAPPING -> "Complex sampler: modulus and argument over the plane";
            case ESCAPE_TIME_FRACTAL -> "Escape-time iterator: every point's orbit until it leaves the bound";
            case NEWTON_FRACTAL -> "Newton basins: every point iterated to the root it falls into";
            case ITERATED_FUNCTION_SYSTEM -> "Chaos game: random affine maps by weight";
            case LINDENMAYER_SYSTEM -> "Rewriting, then a turtle: the axiom grown by its rules and drawn";
            case MATRIX -> "Linear algebra: determinant, rank, eigenvalues";
            case AFFINE_TRANSFORMATION, NONLINEAR_TRANSFORMATION -> "Mapping: a grid carried through the map";
            case POINT_SET -> "Point set: the points as written";
            case GEOMETRIC_CONSTRUCTION -> "Construction: each step from the ones before";
            case POLYTOPE -> "Polytope skeleton: vertices by rule, edges by condition";
            case PROBABILITY_DISTRIBUTION -> "Density engine: the density, its cumulative and moments, sampling";
            case REGION -> exact ? "Exact grid: membership decided over integers with exact rationals"
                    : "Grid membership: the points where the condition holds";
            case SEQUENCE -> "Sampled at integers: one term per value of the input";
            case VALUE -> "Evaluation: one number";
            case CIRCUIT -> "Nodal analysis: Newton–Raphson for each step, trapezoidal transient";
            case PROGRAM -> "Program: the run block executed with the formulas";
            case COMPONENT -> "Component model: its equations and behaviour, checked on a bench";
            case SYMBOL -> "Drawing: paths, pins and labels";
            case GRAPH -> "Workbench: each line computed in its pane, on one clock";
            case FAMILY -> "Family: the member computed once for every value of its index";
        };
        Optional<Line> because = lines.first("equations", line -> true)
                .or(() -> lines.first("outputs", line -> true))
                .or(() -> lines.firstOf(List.of("run", "parts", "maps", "rules", "points", "steps", "vertices", "pins")))
                .or(lines::firstDrawing);
        return derived(kind.name().toLowerCase(Locale.ROOT), label, because);
    }

    // ─── presentation ───────────────────────────────────────────────────────────────────────────────

    private static Derived presentation(ExperienceDocument document, Lines lines) {
        if (document.kind() == Kind.GRAPH) {
            return derived("graph", "A graph: its lines on their panes, moved by one clock", lines.firstOf(List.of("lines", "panes")));
        }
        if (document.kind().role() == Kind.Role.DRAWING) {
            return derived("drawing", "A drawing: the symbol as every sheet draws it", lines.firstDrawing());
        }
        if (document.kind() == Kind.COMPONENT) {
            return derived("part", "A part: its pins and the symbol it is drawn with",
                    lines.firstOf(List.of("symbol", "pins")));
        }
        if (document.block(Block.Parts.class).isPresent()) {
            return derived("instrument", "An instrument: the circuit under measurement, with probes",
                    lines.statement("analysis").or(() -> lines.first("parts", line -> true)));
        }
        return derived("dataset", "Computed data, drawn by its shape on its canvas",
                lines.first("view", line -> line.contains("primary")).or(() -> lines.first("outputs", line -> true)));
    }

    private static Derived view(DefinedObject defined, Lines lines) {
        return defined.representations().stream().filter(DefinedObject.Representation::primary).findFirst()
                .map(primary -> derived(primary.kind().name(), primary.kind().name(),
                        lines.first("view", line -> line.contains("primary"))))
                .orElse(new Derived("", "Chosen by the definition", "", 0));
    }

    // ─── space ──────────────────────────────────────────────────────────────────────────────────────

    private static final List<String> COUNTS = List.of("no", "one", "two", "three", "four", "five", "six");

    /**
     * The dimensions an object lives in and the names that make them: the state's variables for a system that evolves
     * ({@code x, y, z} — lines 17–19), otherwise its inputs and its outputs.
     */
    private static Derived space(int dimension, List<DocumentForm.StatementForm> structure) {
        String count = dimension < COUNTS.size() ? COUNTS.get(dimension) : String.valueOf(dimension);
        String dimensions = count + (dimension == 1 ? " dimension" : " dimensions");
        Optional<DocumentForm.StatementForm> state = block(structure, "state");
        if (state.isPresent() && !state.get().lines().isEmpty()) {
            return named(dimension, dimensions + ": the state's " + names(state.get(), "name"), state.get());
        }
        Optional<DocumentForm.StatementForm> inputs = block(structure, "inputs");
        Optional<DocumentForm.StatementForm> outputs = block(structure, "outputs");
        List<String> parts = new ArrayList<>();
        inputs.filter(block -> !block.lines().isEmpty()).ifPresent(block -> parts.add(plural("input", block) + " " + names(block, "names")));
        outputs.filter(block -> !block.lines().isEmpty()).ifPresent(block -> parts.add(plural("output", block) + " " + names(block, "name")));
        if (parts.isEmpty()) {
            return new Derived(String.valueOf(dimension), dimensions, "", 0);
        }
        DocumentForm.StatementForm first = inputs.filter(block -> !block.lines().isEmpty()).orElseGet(outputs::orElseThrow);
        return named(dimension, dimensions + ": " + String.join(" and ", parts), first);
    }

    private static Derived named(int dimension, String label, DocumentForm.StatementForm block) {
        int first = block.lines().getFirst().line();
        int last = block.lines().getLast().line();
        String lines = first == last ? "line " + first : "lines " + first + "–" + last;
        return new Derived(String.valueOf(dimension), label + " (" + lines + ")", names(block, block.word().equals("inputs") ? "names" : "name"), first);
    }

    private static Optional<DocumentForm.StatementForm> block(List<DocumentForm.StatementForm> structure, String word) {
        return structure.stream().filter(statement -> statement.word().equals(word)).findFirst();
    }

    private static String plural(String word, DocumentForm.StatementForm block) {
        return block.lines().size() == 1 ? word : word + "s";
    }

    private static String names(DocumentForm.StatementForm block, String field) {
        return String.join(", ", block.lines().stream().map(line -> line.fields().stream()
                .filter(candidate -> candidate.schema().name().equals(field)).findFirst()
                .map(FormField::value).orElse("")).filter(name -> !name.isBlank()).toList());
    }

    // ─── capabilities ───────────────────────────────────────────────────────────────────────────────

    private static List<Derived> capabilities(Set<Capability> capabilities, Lines lines) {
        List<Derived> derived = new ArrayList<>();
        for (Capability capability : Capability.values()) {
            if (!capabilities.contains(capability)) {
                continue;
            }
            Optional<Line> because = switch (capability) {
                case PLAY -> lines.first("parameters", line -> line.matches(".*\\banimate\\b.*"))
                        .or(() -> lines.first("inputs", line -> line.matches("\\(?t\\b.*")));
                case RUN -> lines.first("equations", line -> line.startsWith("d(") || line.matches("\\w+_next\\b.*"))
                        .or(() -> lines.statement("analysis")).or(() -> lines.statement("behaviour"));
                case INTERACT -> lines.first("parameters", line -> line.matches(".*\\blive\\b.*"))
                        .or(() -> lines.first("parts", line -> true));
                case RESAMPLE -> lines.anywhere(line -> line.matches(".*\\b(random_\\w*|white_noise)\\s*\\(.*"));
                case PROBE -> lines.first("parts", line -> true);
                case SWEEP -> lines.first("parameters", line -> line.matches(".*\\bin\\b.*\\.\\..*"));
                case CHECK -> lines.statement("expect");
                case EXECUTE -> lines.statement("run");
            };
            derived.add(derived(capability.keyword(), describe(capability), because));
        }
        return derived;
    }

    private static String describe(Capability capability) {
        return switch (capability) {
            case PLAY -> "Play: time is an input, any frame is computed directly";
            case RUN -> "Run: time evolves the state; it steps forward and resets";
            case INTERACT -> "Interact: values change while it runs";
            case RESAMPLE -> "New sample: it draws on randomness";
            case PROBE -> "Probe: its nodes can be measured";
            case SWEEP -> "Sweep: a parameter with a range";
            case CHECK -> "Checks: it carries expectations";
            case EXECUTE -> "Execute: it is a program";
        };
    }

    private static Derived derived(String value, String label, Optional<Line> because) {
        return because.map(line -> new Derived(value, label, line.text(), line.number()))
                .orElse(new Derived(value, label, "", 0));
    }

    // ─── lines of the text ──────────────────────────────────────────────────────────────────────────

    private record Line(String text, int number) {
    }

    /** The text's lines by the statement they belong to, as the outline finds them. */
    private record Lines(String source, DocumentOutline outline) {

        Optional<Line> first(String block, Predicate<String> wanted) {
            for (DocumentOutline.Statement statement : outline.statements()) {
                if (!statement.word().equals(block) || statement.body().isEmpty()) {
                    continue;
                }
                for (DocumentOutline.Line line : statement.body().get().lines()) {
                    String text = line.content().of(source).strip();
                    if (wanted.test(text)) {
                        return Optional.of(new Line(text, numberOf(line.content().start())));
                    }
                }
            }
            return Optional.empty();
        }

        /** A statement's own first line: {@code analysis transient for 0.01 s}, {@code expect "…" {}. */
        Optional<Line> statement(String word) {
            return outline.statements().stream().filter(statement -> statement.word().equals(word)).findFirst()
                    .map(statement -> new Line(firstLineOf(statement.span().of(source)), numberOf(statement.span().start())));
        }

        Optional<Line> firstOf(List<String> words) {
            for (String word : words) {
                Optional<Line> found = first(word, line -> true).or(() -> statement(word));
                if (found.isPresent()) {
                    return found;
                }
            }
            return Optional.empty();
        }

        Optional<Line> firstDrawing() {
            return outline.statements().stream().filter(statement -> Kind.DRAWING_LINES.contains(statement.word())
                            && statement.body().isEmpty()).findFirst()
                    .map(statement -> new Line(statement.span().of(source).strip(), numberOf(statement.span().start())));
        }

        Optional<Line> anywhere(Predicate<String> wanted) {
            for (DocumentOutline.Statement statement : outline.statements()) {
                if (statement.body().isEmpty()) {
                    continue;
                }
                for (DocumentOutline.Line line : statement.body().get().lines()) {
                    String text = line.content().of(source).strip();
                    if (wanted.test(text)) {
                        return Optional.of(new Line(text, numberOf(line.content().start())));
                    }
                }
                String body = source.substring(statement.body().get().open() + 1, statement.body().get().close());
                String[] bodyLines = body.split("\n", -1);
                for (int index = 0; index < bodyLines.length; index++) {
                    if (wanted.test(bodyLines[index].strip())) {
                        return Optional.of(new Line(bodyLines[index].strip(),
                                numberOf(statement.body().get().open() + 1) + index));
                    }
                }
            }
            return Optional.empty();
        }

        private int numberOf(int offset) {
            int line = 1;
            for (int index = 0; index < offset && index < source.length(); index++) {
                if (source.charAt(index) == '\n') {
                    line++;
                }
            }
            return line;
        }

        private static String firstLineOf(String text) {
            String first = text.strip().split("\n", 2)[0].strip();
            return first.endsWith("{") ? first.substring(0, first.length() - 1).strip() : first;
        }
    }
}
