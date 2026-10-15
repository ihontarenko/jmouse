package org.jmouse.formula;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Turns a gradient tape into JVM code, in the background, the way a JIT tiers a method up.
 *
 * <p>A tape starts interpreted: one switch per instruction, values and adjoints in arrays. A circuit evaluates the
 * same few tapes millions of times, so each is written out once as straight-line Java — every instruction a local
 * variable, the backward pass unrolled after it, the common functions called directly with their derivatives in closed
 * form — compiled in memory with the platform's compiler and defined beside this class. The compiled code replaces the
 * interpretation the moment it is ready; until then, and wherever no compiler is present (a runtime without the JDK's
 * {@code java.compiler} module), the tape keeps interpreting, with the same results.</p>
 *
 * <p>Compiled classes are kept by their source, so a model compiled once serves every circuit and every rebuild of it.
 * The generated code refers to nothing but the JDK: functions it does not inline are handed to it as plain
 * {@link ToDoubleFunction}s.</p>
 */
final class TapeCompiler {

    private static final Logger LOGGER = Logger.getLogger(TapeCompiler.class.getName());
    private static final String PACKAGE = TapeCompiler.class.getPackageName();
    /** Larger tapes stay interpreted: a JVM method over about 8000 bytes of bytecode is never compiled to native code. */
    private static final int LARGEST_TAPE = 220;
    private static final int BATCH = 64;

    private static final JavaCompiler COMPILER = locate();
    private static final Map<String, Class<?>> COMPILED = new ConcurrentHashMap<>();
    private static final LinkedBlockingQueue<Request> QUEUE = new LinkedBlockingQueue<>();

    /** A tape waiting for its code: its source, and what to call when the code is there. */
    record Request(String source, String name, Object[] arguments, Consumer<Supplier<ToDoubleBiFunction<double[], double[]>>> install) {
    }

    static {
        if (COMPILER != null) {
            Thread worker = new Thread(TapeCompiler::compileQueuedTapes, "jmf-tape-compiler");
            worker.setDaemon(true);
            worker.setPriority(Thread.NORM_PRIORITY - 1);
            worker.start();
        }
    }

    private TapeCompiler() {
    }

    static boolean available() {
        return COMPILER != null;
    }

    static int largestTape() {
        return LARGEST_TAPE;
    }

    /**
     * Queues a tape's source; {@code install} receives the compiled function's maker, on the compiler's thread. A tape compiled
     * before — the same model in a circuit solved again, after a slider moved — is installed at once, on the caller's
     * thread: queued, every new solver ran its models interpreted until the compiler's thread came round to it.
     */
    static void submit(Request request) {
        if (COMPILER == null) {
            return;
        }
        Class<?> known = COMPILED.get(request.source());
        if (known != null) {
            install(request, known);
            return;
        }
        QUEUE.add(request);
    }

    private static JavaCompiler locate() {
        try {
            return ToolProvider.getSystemJavaCompiler();
        } catch (RuntimeException | LinkageError absent) {
            return null;
        }
    }

    private static void compileQueuedTapes() {
        while (true) {
            try {
                List<Request> batch = new ArrayList<>();
                batch.add(QUEUE.take());
                QUEUE.drainTo(batch, BATCH - 1);
                compile(batch);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            } catch (RuntimeException | LinkageError failure) {
                LOGGER.log(Level.FINE, "A tape stays interpreted", failure);
            }
        }
    }

    private static void compile(List<Request> batch) {
        Map<String, String> pending = new HashMap<>();
        for (Request request : batch) {
            if (!COMPILED.containsKey(request.source())) {
                pending.put(request.name(), request.source());
            }
        }
        if (!pending.isEmpty()) {
            Map<String, byte[]> classes = compileSources(pending);
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            for (Map.Entry<String, String> entry : pending.entrySet()) {
                byte[] bytes = classes.get(PACKAGE + "." + entry.getKey());
                if (bytes != null) {
                    try {
                        COMPILED.putIfAbsent(entry.getValue(), lookup.defineClass(bytes));
                    } catch (IllegalAccessException | LinkageError refused) {
                        LOGGER.log(Level.FINE, "A tape class was refused", refused);
                    }
                }
            }
        }
        for (Request request : batch) {
            Class<?> type = COMPILED.get(request.source());
            if (type != null) {
                install(request, type);
            }
        }
    }

    /**
     * Hands the tape a way to build its function from the compiled class — one instance for each copy of the tape, since
     * an instance keeps its own invariant values. The first is built here, so a class that cannot be is never handed on.
     */
    private static void install(Request request, Class<?> type) {
        Constructor<?> constructor = type.getDeclaredConstructors()[0];
        Supplier<ToDoubleBiFunction<double[], double[]>> instances = () -> instantiate(constructor, request.arguments());
        try {
            instances.get();
            request.install().accept(instances);
        } catch (RuntimeException failure) {
            LOGGER.log(Level.FINE, "A compiled tape could not be built", failure);
        }
    }

    @SuppressWarnings("unchecked")
    private static ToDoubleBiFunction<double[], double[]> instantiate(Constructor<?> constructor, Object[] arguments) {
        try {
            return (ToDoubleBiFunction<double[], double[]>) constructor.newInstance(arguments);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static Map<String, byte[]> compileSources(Map<String, String> sources) {
        List<JavaFileObject> units = new ArrayList<>();
        sources.forEach((name, source) -> units.add(new SimpleJavaFileObject(
                URI.create("string:///" + PACKAGE.replace('.', '/') + "/" + name + ".java"), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return source;
            }
        }));
        Map<String, ByteArrayOutputStream> outputs = new HashMap<>();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager standard = COMPILER.getStandardFileManager(diagnostics, null, null);
        JavaFileManager memory = new ForwardingJavaFileManager<>(standard) {
            @Override
            public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) {
                return new SimpleJavaFileObject(URI.create("bytes:///" + className.replace('.', '/') + kind.extension), kind) {
                    @Override
                    public OutputStream openOutputStream() {
                        return outputs.computeIfAbsent(className, ignored -> new ByteArrayOutputStream());
                    }
                };
            }
        };
        boolean compiled = COMPILER.getTask(null, memory, diagnostics, List.of("-proc:none", "-g:none", "-nowarn"), null, units).call();
        if (!compiled) {
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                LOGGER.log(Level.FINE, "Tape source refused: {0}", diagnostic.getMessage(null));
            }
        }
        Map<String, byte[]> classes = new HashMap<>();
        outputs.forEach((name, bytes) -> classes.put(name, bytes.toByteArray()));
        return classes;
    }

    // ─── writing a tape out ───────────────────────────────────────────────────────────────────────────

    /**
     * The class every tape is written into: {@code %1$s} the package, {@code %2$s} the class name, {@code %3$s} the
     * invariant instructions' fields, {@code %4$s} the body of {@code applyAsDouble}.
     */
    private static final String CLASS = """
            package %1$s;
            public final class %2$s implements java.util.function.ToDoubleBiFunction<double[], double[]> {
              private final java.util.function.ToDoubleFunction<double[]>[] calls;
              private final java.util.function.ToDoubleFunction<double[]>[][] partials;
            %3$s  public %2$s(java.util.function.ToDoubleFunction<double[]>[] calls, java.util.function.ToDoubleFunction<double[]>[][] partials) {
                this.calls = calls;
                this.partials = partials;
              }
              public double applyAsDouble(double[] x, double[] g) {
            %4$s  }
              private static double integerPower(double base, int exponent) {
                double product = 1;
                double factor = exponent < 0 ? 1 / base : base;
                for (int remaining = Math.abs(exponent); remaining > 0; remaining--) {
                  product *= factor;
                }
                return product;
              }
              private static double realPower(double base, double exponent) {
                if (base < 0 && exponent != Math.rint(exponent)) {
                  double reciprocal = 1 / exponent;
                  if (Math.abs(reciprocal - Math.rint(reciprocal)) < 1e-12 && ((long) Math.rint(reciprocal)) %% 2 != 0) {
                    return -Math.pow(-base, exponent);
                  }
                }
                return Math.pow(base, exponent);
              }
              private static double truth(double first, double second, boolean holds) {
                return Double.isNaN(first) || Double.isNaN(second) ? Double.NaN : holds ? 1 : 0;
              }
            }
            """;

    /** Recomputes the invariant instructions: {@code %1$s} when, {@code %2$s} the statements. */
    private static final String INVARIANT_REFRESH = """
            if (%1$s) {
              primed = true;
            %2$s}
            """;

    /**
     * How one instruction is written: the expression of its value, and what its adjoint owes each of its inputs. Both
     * read {@code {0}}, {@code {1}}, {@code {2}} — the inputs' values — {@code {value}} — its own — and
     * {@code {adjoint}}; {@code preamble} is a statement the accumulations need first.
     */
    private record Template(String value, String preamble, List<Accumulation> accumulations) {

        Template(String value, Accumulation... accumulations) {
            this(value, null, List.of(accumulations));
        }
    }

    /** {@code operator} {@code term} into the adjoint of input {@code input}, only while {@code condition} holds. */
    private record Accumulation(int input, String operator, String term, String condition) {

        Accumulation when(String holds) {
            return new Accumulation(input, operator, term, holds);
        }
    }

    private static Accumulation add(int input, String term) {
        return new Accumulation(input, "+=", term, null);
    }

    private static Accumulation subtract(int input, String term) {
        return new Accumulation(input, "-=", term, null);
    }

    private static final String ADJOINT = "{adjoint}";

    /** The instructions written the same way every time, by operation — or, for a call, by the function called. */
    private static final Map<String, Template> TEMPLATES = Map.ofEntries(
            Map.entry("NEGATE", new Template("-{0}", subtract(0, ADJOINT))),
            Map.entry("ADD", new Template("{0} + {1}", add(0, ADJOINT), add(1, ADJOINT))),
            Map.entry("SUBTRACT", new Template("{0} - {1}", add(0, ADJOINT), subtract(1, ADJOINT))),
            Map.entry("MULTIPLY", new Template("{0} * {1}", add(0, "{adjoint} * {1}"), add(1, "{adjoint} * {0}"))),
            Map.entry("DIVIDE", new Template("{0} / {1}", add(0, "{adjoint} / {1}"), subtract(1, "{adjoint} * {value} / {1}"))),
            Map.entry("POWER", realPower()),
            Map.entry("AND", new Template("truth({0}, {1}, {0} != 0 && {1} != 0)")),
            Map.entry("OR", new Template("truth({0}, {1}, {0} != 0 || {1} != 0)")),
            Map.entry("NOT", new Template("truth({0}, 0, {0} == 0)")),
            // A NaN condition takes the first branch backwards, as the interpreted tape does.
            Map.entry("SELECT", new Template("Double.isNaN({0}) ? Double.NaN : {0} != 0 ? {1} : {2}",
                    add(1, ADJOINT).when("{0} != 0"), add(2, ADJOINT).when("!({0} != 0)"))),
            Map.entry("SIN", new Template("Math.sin({0})", add(0, "{adjoint} * Math.cos({0})"))),
            Map.entry("COS", new Template("Math.cos({0})", add(0, "{adjoint} * -Math.sin({0})"))),
            Map.entry("TAN", new Template("Math.tan({0})", add(0, "{adjoint} * (1 + {value} * {value})"))),
            Map.entry("SINH", new Template("Math.sinh({0})", add(0, "{adjoint} * Math.cosh({0})"))),
            Map.entry("COSH", new Template("Math.cosh({0})", add(0, "{adjoint} * Math.sinh({0})"))),
            Map.entry("TANH", new Template("MathematicalFunction.hyperbolicTangent({0})", add(0, "{adjoint} * (1 - {value} * {value})"))),
            Map.entry("EXP", new Template("Math.exp({0})", add(0, "{adjoint} * {value}"))),
            Map.entry("LN", new Template("Math.log({0})", add(0, "{adjoint} * (1 / {0})"))),
            Map.entry("SQRT", new Template("Math.sqrt({0})", add(0, "{adjoint} * (0.5 / {value})"))),
            Map.entry("ABS", new Template("Math.abs({0})", add(0, "{adjoint} * Math.signum({0})"))),
            Map.entry("ATAN", new Template("Math.atan({0})", add(0, "{adjoint} * (1 / (1 + {0} * {0}))"))),
            Map.entry("MIN", new Template("Math.min({0}, {1})", add(0, ADJOINT).when("{0} <= {1}"), add(1, ADJOINT).when("!({0} <= {1})"))),
            Map.entry("MAX", new Template("Math.max({0}, {1})", add(0, ADJOINT).when("{0} >= {1}"), add(1, ADJOINT).when("!({0} >= {1})"))),
            Map.entry("POW", realPower()));

    private static Template realPower() {
        return new Template("realPower({0}, {1})", add(0, "{adjoint} * {1} * realPower({0}, {1} - 1)"),
                add(1, "{adjoint} * {value} * Math.log({0})").when("{0} > 0"));
    }

    /**
     * The Java source of a tape. {@code callSlots} collects the instructions that call a function through an
     * interface — their forward implementation in {@code calls}, their derivatives by argument in {@code partials}.
     */
    static String source(String name, GradientCompiler.TapeView tape, List<String> callSlots) {
        int result = tape.result();
        Template[] templates = new Template[result + 1];
        for (int index = 0; index <= result; index++) {
            templates[index] = template(tape, index, callSlots);
        }
        List<String> body = new ArrayList<>();
        String fields = writeInvariants(tape, templates, body);
        for (int index = 0; index <= result; index++) {
            if (!tape.invariant(index)) {
                body.add("double v%d = %s;".formatted(index, value(tape, templates, index)));
            }
        }
        body.add("if (g == null || g.length == 0) {\n  return v%d;\n}".formatted(result));
        for (int index = 0; index <= result; index++) {
            if (!tape.invariant(index)) {
                body.add("double a%d = %d;".formatted(index, index == result ? 1 : 0));
            }
        }
        for (int index = result; index >= 0; index--) {
            if (!tape.invariant(index)) {
                List<String> backward = backward(tape, templates, index);
                if (!backward.isEmpty()) {
                    body.add("if (a%d != 0) {\n%s}".formatted(index, String.join("\n", backward).indent(2)));
                }
            }
        }
        int[] targets = tape.targetInstructions();
        for (int target = 0; target < targets.length; target++) {
            boolean reached = targets[target] >= 0 && !tape.invariant(targets[target]);
            body.add("g[%d] = %s;".formatted(target, reached ? "a" + targets[target] : "0"));
        }
        body.add("return v%d;".formatted(result));
        return CLASS.formatted(PACKAGE, name, fields, String.join("\n", body).indent(4));
    }

    /**
     * The invariant instructions as fields, and the statement that computes them again only when an invariant argument
     * they read has changed — compared bit for bit, so a NaN parameter is no reason to recompute every time and a -0 is
     * not a 0. Returns the fields; the statement goes into {@code body}.
     */
    private static String writeInvariants(GradientCompiler.TapeView tape, Template[] templates, List<String> body) {
        List<String> fields = new ArrayList<>();
        List<String> compute = new ArrayList<>();
        for (int index = 0; index <= tape.result(); index++) {
            if (tape.invariant(index)) {
                fields.add("private double v%d;".formatted(index));
                compute.add("v%d = %s;".formatted(index, value(tape, templates, index)));
            }
        }
        if (compute.isEmpty()) {
            return "";
        }
        Set<Integer> positions = new LinkedHashSet<>();
        for (int index = 0; index <= tape.result(); index++) {
            if (tape.invariant(index) && tape.operation(index).equals("ARGUMENT")) {
                positions.add(tape.arguments()[index]);
            }
        }
        List<String> changed = new ArrayList<>(List.of("!primed"));
        List<String> hold = new ArrayList<>();
        fields.addFirst("private boolean primed;");
        for (int position : positions) {
            fields.add("private long h%d;".formatted(position));
            changed.add("Double.doubleToRawLongBits(x[%1$d]) != h%1$d".formatted(position));
            hold.add("h%1$d = Double.doubleToRawLongBits(x[%1$d]);".formatted(position));
        }
        hold.addAll(compute);
        body.add(INVARIANT_REFRESH.formatted(String.join(" || ", changed), String.join("\n", hold).indent(2)).stripTrailing());
        return String.join("\n", fields).indent(2);
    }

    /** How an instruction is written: from the table, or — for what carries its own constant — made for it. */
    private static Template template(GradientCompiler.TapeView tape, int index, List<String> callSlots) {
        String operation = tape.operation(index);
        String key = operation.equals("CALL") ? tape.function(index) : operation;
        Template known = TEMPLATES.get(key);
        if (known != null) {
            return known;
        }
        return switch (operation) {
            case "CONSTANT" -> new Template(literal(tape.constants()[index]));
            case "ARGUMENT" -> new Template("x[%d]".formatted(tape.arguments()[index]));
            case "INTEGER_POWER" -> {
                int exponent = (int) tape.constants()[index];
                if (exponent == 0) {
                    yield new Template("1");
                }
                yield new Template(integerPower("{0}", exponent),
                        add(0, "{adjoint} * %d * %s".formatted(exponent, integerPower("{0}", exponent - 1))));
            }
            case "COMPARE" -> new Template("truth({0}, {1}, {0} %s {1})".formatted(tape.relation(index)));
            case "CALL" -> interfaceCall(tape.inputs()[index].length, index, slot(callSlots, index));
            default -> throw new IllegalStateException(operation);
        };
    }

    /** A function not written in place: called through {@code calls}, differentiated through {@code partials}. */
    private static Template interfaceCall(int arity, int index, int slot) {
        List<String> operands = new ArrayList<>();
        List<Accumulation> accumulations = new ArrayList<>();
        for (int argument = 0; argument < arity; argument++) {
            operands.add("{" + argument + "}");
            accumulations.add(add(argument, "{adjoint} * partials[%d][%d].applyAsDouble(at%d)".formatted(slot, argument, index)));
        }
        String at = "new double[] {" + String.join(", ", operands) + "}";
        return new Template("calls[%d].applyAsDouble(%s)".formatted(slot, at), "double[] at%d = %s;".formatted(index, at),
                accumulations);
    }

    private static String value(GradientCompiler.TapeView tape, Template[] templates, int index) {
        return fill(templates[index].value(), tape, index);
    }

    /** The statements of an instruction's backward pass; an input that is invariant has no adjoint to add to. */
    private static List<String> backward(GradientCompiler.TapeView tape, Template[] templates, int index) {
        int[] inputs = tape.inputs()[index];
        List<String> statements = new ArrayList<>();
        for (Accumulation accumulation : templates[index].accumulations()) {
            int input = inputs[accumulation.input()];
            if (tape.invariant(input)) {
                continue;
            }
            String statement = "a%d %s %s;".formatted(input, accumulation.operator(), fill(accumulation.term(), tape, index));
            statements.add(accumulation.condition() == null ? statement
                    : "if (%s) {\n  %s\n}".formatted(fill(accumulation.condition(), tape, index), statement));
        }
        String preamble = templates[index].preamble();
        if (preamble != null && !statements.isEmpty()) {
            statements.addFirst(fill(preamble, tape, index));
        }
        return statements;
    }

    /** A template with its placeholders replaced by the instruction's own variables. */
    private static String fill(String template, GradientCompiler.TapeView tape, int index) {
        int[] inputs = tape.inputs()[index];
        String text = template.replace("{value}", "v" + index).replace(ADJOINT, "a" + index);
        for (int input = 0; input < inputs.length; input++) {
            text = text.replace("{" + input + "}", "v" + inputs[input]);
        }
        return text;
    }

    private static String literal(double value) {
        return "Double.longBitsToDouble(0x%sL)".formatted(Long.toHexString(Double.doubleToRawLongBits(value)));
    }

    private static String integerPower(String base, int exponent) {
        return switch (exponent) {
            case 0 -> "1";
            case 1 -> base;
            case 2 -> base + " * " + base;
            case 3 -> base + " * " + base + " * " + base;
            default -> "integerPower(%s, %d)".formatted(base, exponent);
        };
    }

    private static int slot(List<String> callSlots, int index) {
        String key = String.valueOf(index);
        int position = callSlots.indexOf(key);
        if (position < 0) {
            callSlots.add(key);
            position = callSlots.size() - 1;
        }
        return position;
    }
}
