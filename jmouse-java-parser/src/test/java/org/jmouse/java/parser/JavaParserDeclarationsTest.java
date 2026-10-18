package org.jmouse.java.parser;

import java.util.List;
import java.util.Set;
import org.jmouse.java.parser.model.ConstructorDeclaration;
import org.jmouse.java.parser.model.Declaration;
import org.jmouse.java.parser.model.DeclarationKind;
import org.jmouse.java.parser.model.EnumConstantDeclaration;
import org.jmouse.java.parser.model.FieldDeclaration;
import org.jmouse.java.parser.model.ImportDeclaration;
import org.jmouse.java.parser.model.InitializerDeclaration;
import org.jmouse.java.parser.model.JavaSourceFile;
import org.jmouse.java.parser.model.MethodDeclaration;
import org.jmouse.java.parser.model.Modifier;
import org.jmouse.java.parser.model.RecordComponentDeclaration;
import org.jmouse.java.parser.model.TypeDeclaration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaParserDeclarationsTest {

    private final JavaParser parser = new JavaParser();

    private JavaSourceFile parseComplete(String text) {
        JavaSourceFile file = parser.parse("Test.java", text);
        assertTrue(file.isComplete(), () -> "Unexpected problems: " + file.problems());
        return file;
    }

    private static Declaration find(JavaSourceFile file, String symbolId) {
        return file.declarations().stream()
                .filter(declaration -> declaration.symbolId().value().equals(symbolId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No declaration " + symbolId + " among "
                        + file.declarations().stream().map(declaration -> declaration.symbolId().value()).toList()));
    }

    @Test
    void packageAndImportsAreRead() {
        JavaSourceFile file = parseComplete("""
                package org.example.deep;

                import java.util.List;
                import java.util.*;
                import static java.util.Objects.requireNonNull;
                import static java.util.Map.*;

                class Holder {}
                """);

        assertEquals("org.example.deep", file.packageName());
        assertEquals(List.of(
                new ImportDeclaration("java.util.List", false, false, file.imports().get(0).nameSpan(), file.imports().get(0).span()),
                new ImportDeclaration("java.util", false, true, file.imports().get(1).nameSpan(), file.imports().get(1).span()),
                new ImportDeclaration("java.util.Objects.requireNonNull", true, false, file.imports().get(2).nameSpan(), file.imports().get(2).span()),
                new ImportDeclaration("java.util.Map", true, true, file.imports().get(3).nameSpan(), file.imports().get(3).span())), file.imports());
        assertEquals(3, file.imports().getFirst().span().startLine());
        assertEquals("org.example.deep.Holder", file.types().getFirst().symbolId().value());
    }

    @Test
    void everyTypeKindIsRecognised() {
        JavaSourceFile file = parseComplete("""
                package p;
                public class A {}
                interface B {}
                enum C { X }
                record D(int value) {}
                @interface E {}
                """);

        assertEquals(List.of(DeclarationKind.CLASS, DeclarationKind.INTERFACE, DeclarationKind.ENUM, DeclarationKind.RECORD,
                DeclarationKind.ANNOTATION_TYPE), file.types().stream().map(TypeDeclaration::kind).toList());
    }

    @Test
    void sealedHierarchyAndRelationshipsAreKeptAsWritten() {
        JavaSourceFile file = parseComplete("""
                package shapes;
                public sealed interface Shape extends Comparable<Shape>, java.io.Serializable permits Circle, Square {}
                final class Circle implements Shape {}
                non-sealed class Square extends Object implements Shape, Cloneable {}
                """);

        TypeDeclaration shape = file.types().get(0);
        assertEquals(Set.of(Modifier.PUBLIC, Modifier.SEALED), shape.modifiers());
        assertEquals(List.of("Comparable<Shape>", "java.io.Serializable"), shape.extendedTypes());
        assertEquals(List.of("Circle", "Square"), shape.permittedTypes());
        assertEquals("public sealed interface Shape extends Comparable<Shape>, java.io.Serializable permits Circle, Square", shape.signature());

        TypeDeclaration square = file.types().get(2);
        assertEquals(Set.of(Modifier.NON_SEALED), square.modifiers());
        assertEquals(List.of("Object"), square.extendedTypes());
        assertEquals(List.of("Shape", "Cloneable"), square.implementedTypes());
    }

    @Test
    void nestedTypesAreQualifiedThroughTheirEnclosingTypes() {
        JavaSourceFile file = parseComplete("""
                package p;
                class Outer {
                    static class Inner {
                        interface Deepest { void act(); }
                    }
                    enum Mode { ON, OFF }
                    record Pair(String left, String right) {}
                }
                """);

        TypeDeclaration outer = file.types().getFirst();
        assertEquals(List.of("Inner", "Mode", "Pair"), outer.nestedTypes().stream().map(TypeDeclaration::name).toList());
        assertNotNull(find(file, "p.Outer.Inner.Deepest#act()"));
        assertEquals("p.Outer.Inner.Deepest", find(file, "p.Outer.Inner.Deepest").qualifiedName());
        assertNotNull(find(file, "p.Outer.Mode#OFF"));
        assertNotNull(find(file, "p.Outer.Pair#right"));
    }

    @Test
    void fieldsWithSeveralDeclaratorsBecomeOneDeclarationEach() {
        JavaSourceFile file = parseComplete("""
                class Values {
                    /** Shared comment. */
                    private static final int first = 1, second[] = {2, 3}, third;
                    java.util.Map<String, java.util.List<Integer>> map = new java.util.HashMap<String, java.util.List<Integer>>(), other = null;
                    boolean flag = 1 < 2, flagTwo = 3 > 2;
                }
                """);

        FieldDeclaration first  = (FieldDeclaration) find(file, "Values#first");
        FieldDeclaration second = (FieldDeclaration) find(file, "Values#second");
        FieldDeclaration third  = (FieldDeclaration) find(file, "Values#third");
        assertEquals("int", first.type());
        assertEquals("int[]", second.type());
        assertEquals(first.span(), third.span());
        assertEquals("private static final int second", second.signature());
        assertEquals(Set.of(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL), third.modifiers());
        assertEquals("Values#third", third.javaDoc().owner().symbolId().value());
        assertEquals("Shared comment.", third.javaDoc().description());

        assertEquals("java.util.Map<String, java.util.List<Integer>>", ((FieldDeclaration) find(file, "Values#map")).type());
        assertNotNull(find(file, "Values#other"));
        assertNotNull(find(file, "Values#flag"));
        assertNotNull(find(file, "Values#flagTwo"));
    }

    @Test
    void methodsKeepTheirWholeSignatureAndAnOpaqueBody() {
        String text = """
                package p;
                abstract class Service<K extends Comparable<K>> {
                    @Deprecated(since = "1.0", forRemoval = true)
                    public static <T extends Number & Comparable<T>> java.util.List<? super T> collect(
                            final @SuppressWarnings({"unchecked", "rawtypes"}) Class<T> type, int[] sizes, String... names)
                            throws java.io.IOException, IllegalStateException {
                        return null;
                    }
                    abstract void nothing();
                    native long legacy()[];
                    void receiver(Service<K> this, int value) {}
                }
                """;
        JavaSourceFile file = parseComplete(text);

        MethodDeclaration collect = (MethodDeclaration) find(file, "p.Service#collect(Class,int[],String...)");
        assertEquals("java.util.List<? super T>", collect.returnType());
        assertEquals(List.of("type", "sizes", "names"), collect.parameters().stream().map(parameter -> parameter.name()).toList());
        assertTrue(collect.parameters().get(2).variableArity());
        assertEquals(Set.of(Modifier.FINAL), collect.parameters().get(0).modifiers());
        assertEquals("SuppressWarnings", collect.parameters().get(0).annotations().getFirst().name());
        assertEquals(List.of("java.io.IOException", "IllegalStateException"), collect.thrownTypes());
        assertEquals("T", collect.typeParameters().getFirst().name());
        assertEquals(List.of("Number", "Comparable<T>"), collect.typeParameters().getFirst().bounds());
        assertEquals("Deprecated", collect.annotations().getFirst().name());
        assertEquals("since = \"1.0\", forRemoval = true", collect.annotations().getFirst().arguments());
        assertTrue(collect.signature().startsWith("@Deprecated(since = \"1.0\", forRemoval = true) public static <T extends"));
        assertTrue(collect.signature().endsWith("throws java.io.IOException, IllegalStateException"));
        assertEquals("{\n        return null;\n    }", collect.body().slice(text));
        assertEquals(6, collect.body().startLine());
        assertEquals(8, collect.body().endLine());
        assertEquals(3, collect.span().startLine());

        MethodDeclaration nothing = (MethodDeclaration) find(file, "p.Service#nothing()");
        assertNull(nothing.body());
        assertEquals("long[]", ((MethodDeclaration) find(file, "p.Service#legacy()")).returnType());

        MethodDeclaration receiver = (MethodDeclaration) find(file, "p.Service#receiver(int)");
        assertTrue(receiver.parameters().getFirst().receiver());
    }

    @Test
    void constructorsInitializersAndCompactConstructorsAreRead() {
        JavaSourceFile file = parseComplete("""
                package p;
                class Box {
                    static { System.out.println("static"); }
                    { counter++; }
                    static { }
                    int counter;
                    public <T> Box(T value, java.util.List<String> names) throws Exception { }
                    Box() { this(null, null); }
                }
                record Range(int low, int high) {
                    Range {
                        if (low > high) { throw new IllegalArgumentException(); }
                    }
                    Range(int single) { this(single, single); }
                }
                """);

        InitializerDeclaration firstStatic = (InitializerDeclaration) find(file, "p.Box#<static-initializer>[1]");
        assertTrue(firstStatic.isStatic());
        assertNotNull(find(file, "p.Box#<static-initializer>[2]"));
        assertEquals(DeclarationKind.INITIALIZER, find(file, "p.Box#<initializer>[1]").kind());

        ConstructorDeclaration generic = (ConstructorDeclaration) find(file, "p.Box#Box(T,java.util.List)");
        assertEquals("T", generic.typeParameters().getFirst().name());
        assertEquals(List.of("Exception"), generic.thrownTypes());
        assertNotNull(find(file, "p.Box#Box()"));

        ConstructorDeclaration compact = (ConstructorDeclaration) find(file, "p.Range#Range(int,int)");
        assertTrue(compact.compact());
        assertEquals(DeclarationKind.COMPACT_CONSTRUCTOR, compact.kind());
        assertEquals("Range", compact.signature());
        assertNotNull(find(file, "p.Range#Range(int)"));

        TypeDeclaration range = (TypeDeclaration) find(file, "p.Range");
        assertEquals(List.of("low", "high"), range.recordComponents().stream().map(RecordComponentDeclaration::name).toList());
        assertEquals("record Range(int low, int high)", range.signature());
    }

    @Test
    void enumConstantsKeepArgumentsBodiesAndDocumentation() {
        JavaSourceFile file = parseComplete("""
                package p;
                enum Planet implements Runnable {
                    /** The first. */
                    @Deprecated MERCURY(3.303e+23, 2.4397e6),
                    VENUS(4.869e+24, 6.0518e6) {
                        @Override public void run() { }
                    },
                    EARTH,
                    ;
                    private final double mass;
                    Planet() { this(0, 0); }
                    Planet(double mass, double radius) { this.mass = mass; }
                    public void run() {}
                }
                """);

        EnumConstantDeclaration mercury = (EnumConstantDeclaration) find(file, "p.Planet#MERCURY");
        assertEquals("3.303e+23, 2.4397e6", mercury.arguments());
        assertEquals("The first.", mercury.javaDoc().description());
        assertEquals("Deprecated", mercury.annotations().getFirst().name());
        assertEquals("@Deprecated MERCURY(3.303e+23, 2.4397e6)", mercury.signature());
        EnumConstantDeclaration venus = (EnumConstantDeclaration) find(file, "p.Planet#VENUS");
        assertNotNull(venus.body());
        assertNull(((EnumConstantDeclaration) find(file, "p.Planet#EARTH")).arguments());
        assertNotNull(find(file, "p.Planet#mass"));
        assertNotNull(find(file, "p.Planet#Planet(double,double)"));
        assertNotNull(find(file, "p.Planet#run()"));
    }

    @Test
    void annotationTypeElementsKeepTheirDefaults() {
        JavaSourceFile file = parseComplete("""
                package p;
                @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
                public @interface Marker {
                    String value() default "x";
                    int[] numbers() default {1, 2};
                    Class<?> type() default Object.class;
                    int CONSTANT = 4;
                    @interface Nested {}
                }
                """);

        TypeDeclaration marker = file.types().getFirst();
        assertEquals("java.lang.annotation.Retention", marker.annotations().getFirst().name());
        assertEquals("\"x\"", ((MethodDeclaration) find(file, "p.Marker#value()")).defaultValue());
        assertEquals("{1, 2}", ((MethodDeclaration) find(file, "p.Marker#numbers()")).defaultValue());
        assertEquals("Object.class", ((MethodDeclaration) find(file, "p.Marker#type()")).defaultValue());
        assertEquals("String value()", find(file, "p.Marker#value()").signature());
        assertNotNull(find(file, "p.Marker#CONSTANT"));
        assertEquals(DeclarationKind.ANNOTATION_TYPE, find(file, "p.Marker.Nested").kind());
    }

    @Test
    void interfacesWithDefaultStaticAndPrivateMethods() {
        JavaSourceFile file = parseComplete("""
                interface Greeter {
                    String name();
                    default String greet() { return "Hello, " + name(); }
                    static Greeter of(String name) { return () -> name; }
                    private void helper() {}
                }
                """);

        assertEquals(Set.of(Modifier.DEFAULT), find(file, "Greeter#greet()").modifiers());
        assertEquals(Set.of(Modifier.STATIC), find(file, "Greeter#of(String)").modifiers());
        assertFalse(file.declarations().isEmpty());
    }

    @Test
    void duplicateIdentitiesAreNumbered() {
        JavaSourceFile file = parser.parse("Twice.java", """
                class Twice {
                    void same() {}
                    void same() {}
                }
                """);

        assertNotNull(find(file, "Twice#same()"));
        assertNotNull(find(file, "Twice#same()~2"));
    }

    @Test
    void moduleDeclarationIsReadWithOpaqueDirectives() {
        JavaSourceFile file = parseComplete("""
                /** The module. */
                open module org.example.app {
                    requires java.base;
                    exports org.example.app.api;
                }
                """);

        assertEquals("org.example.app", file.moduleDeclaration().name());
        assertTrue(file.moduleDeclaration().open());
        assertEquals("The module.", file.moduleDeclaration().javaDoc().description());
    }

    @Test
    void spansAreExactLinesColumnsAndOffsets() {
        String text = "package p;\r\nclass Position {\r\n    int value;\r\n}\r\n";
        JavaSourceFile   file  = parseComplete(text);
        FieldDeclaration value = (FieldDeclaration) find(file, "p.Position#value");

        assertEquals(3, value.span().start().line());
        assertEquals(5, value.span().start().column());
        assertEquals(text.indexOf("int value"), value.span().start().offset());
        assertEquals("int value;", value.span().slice(text));
        assertEquals("{\r\n    int value;\r\n}", file.types().getFirst().bodySpan().slice(text));
    }
}
