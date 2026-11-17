package org.jmouse.j2js.teavm.processing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The processor run by a real compilation: what it writes for exports, what it refuses and why. */
class TeaVmExportProcessorTest {

    @TempDir
    Path output;

    @Test
    void aClassOfExportsGetsAModuleClassThatCallsThem() throws IOException {
        Compilation compilation = compile("""
                package sample;

                import org.jmouse.j2js.Export;

                public final class Calculator {

                    @Export
                    public static double sum(double first, double second) {
                        return first + second;
                    }

                    @Export
                    public static void reset() {
                    }

                    public static double notExported() {
                        return 0;
                    }
                }
                """);

        assertTrue(compilation.succeeded(), compilation.errors().toString());

        String module = Files.readString(output.resolve("sample/CalculatorExports.java"));

        assertTrue(module.contains("import org.teavm.jso.JSExport;"), module);
        assertTrue(unindented(module).contains("""
                @JSExport
                public static double sum(double first, double second) {
                return Calculator.sum(first, second);
                }"""), module);
        assertTrue(unindented(module).contains("""
                @JSExport
                public static void reset() {
                Calculator.reset();
                }"""), module);
        assertFalse(module.contains("notExported"), module);
        assertTrue(module.contains("public static void main(String[] arguments)"), module);
    }

    @Test
    void theModuleIsDeclaredForTypeScriptByItsJava() throws IOException {
        Compilation compilation = compile("""
                package sample;

                import org.jmouse.j2js.Export;

                public final class Shapes {

                    /**
                     * The area of a circle.
                     *
                     * @param radius its radius
                     */
                    @Export
                    public static double area(double radius) {
                        return radius;
                    }

                    @Export
                    public static boolean[] flags(String[] names, int count) {
                        return new boolean[0];
                    }
                }
                """, "-Aj2js.module=shapes");

        assertTrue(compilation.succeeded(), compilation.errors().toString());
        assertEquals("""
                // What a page may call of sample.Shapes: written by the TeaVM binding of jmouse-j2js from its @Export methods.

                /**
                 * The area of a circle.
                 *
                 * @param radius its radius
                 */
                export function area(radius: number): number;

                export function flags(names: string[], count: number): boolean[];
                """, Files.readString(output.resolve("j2js/shapes.d.ts")));
    }

    @Test
    void textAndArraysCrossAndAreWrittenAsInAnySource() throws IOException {
        Compilation compilation = compile("""
                package sample;

                import org.jmouse.j2js.Export;

                public final class Texts {

                    @Export
                    public static String[] split(String text, int[] at) {
                        return new String[0];
                    }
                }
                """);

        assertTrue(compilation.succeeded(), compilation.errors().toString());
        assertTrue(Files.readString(output.resolve("sample/TextsExports.java")).contains("public static String[] split(String text, int[] at)"));
    }

    @Test
    void whatCannotCrossIsRefusedWhileTheClassCompilesWithTheMethodNamed() {
        Compilation compilation = compile("""
                package sample;

                import java.util.List;
                import org.jmouse.j2js.Export;

                public final class Wrong {

                    @Export
                    public static long count(List<String> names) {
                        return names.size();
                    }

                    @Export
                    public double notStatic() {
                        return 0;
                    }
                }
                """);

        assertFalse(compilation.succeeded());
        assertEquals(List.of(
                "@Export: its result is long, which does not cross to a page — numbers, truth values, text and arrays of them do; anything structured crosses as JSON text",
                "@Export: parameter 'names' is java.util.List, which does not cross to a page — numbers, truth values, text and arrays of them do; anything structured crosses as JSON text",
                "@Export: an exported method is public static: a page calls it on no object"
        ), compilation.errors());
    }

    /** A source with every line's indentation taken off: what it says, line by line, wherever it stands. */
    private static String unindented(String source) {
        return source.lines().map(String::strip).collect(Collectors.joining("\n"));
    }

    /** What a compilation of one source, with the processor and this test's class path, came to. */
    private record Compilation(boolean succeeded, List<String> errors) {
    }

    private Compilation compile(String source, String... more) {
        JavaCompiler                        compiler    = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        String                              className   = source.replaceAll("(?s).*public final class (\\w+).*", "$1");
        JavaFileObject                      file        = new SimpleJavaFileObject(URI.create("string:///sample/" + className + ".java"), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return source;
            }
        };

        List<String> options = new java.util.ArrayList<>(List.of(
                "-classpath", System.getProperty("java.class.path"),
                "-d", output.toString(),
                "-s", output.toString(),
                "-proc:full"));
        options.addAll(List.of(more));

        JavaCompiler.CompilationTask task = compiler.getTask(null, null, diagnostics, options, null, List.of(file));
        task.setProcessors(List.of(new TeaVmExportProcessor()));

        boolean succeeded = task.call();
        List<String> errors = diagnostics.getDiagnostics().stream()
                .filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR)
                .map(diagnostic -> diagnostic.getMessage(null))
                .toList();

        return new Compilation(succeeded, errors);
    }
}
