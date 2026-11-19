package org.jmouse.java.index;

import static org.junit.jupiter.api.Assertions.*;

import org.jmouse.core.matcher.TextMatchers;
import org.jmouse.el.writer.SourceWritingLimitException;
import org.jmouse.java.bytecode.JavaBytecodeParser;
import org.jmouse.java.bytecode.node.constant.JavaUtf8ConstantNode;
import org.jmouse.java.index.binary.BinaryProjectionLimits;
import org.jmouse.java.index.binary.ClassFileIndexer;
import org.jmouse.java.index.binary.ClassFileProjectionConfiguration;
import org.jmouse.java.parser.model.TypeReferenceRole;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

class BinaryIndexProjectionTest {
    @Test
    void validJvmNamesThatAreNotJavaIdentifiersAreIndexedWithoutSourceGrammarRestrictions()
            throws Exception {
        byte[] bytes;
        try (InputStream input =
                Bridge.class.getResourceAsStream("BinaryIndexProjectionTest$Bridge.class")) {
            bytes = input.readAllBytes();
        }
        JavaUtf8ConstantNode name =
                new JavaBytecodeParser()
                        .parse("Bridge.class", bytes).requireComplete().pool().entries().stream()
                                .filter(JavaUtf8ConstantNode.class::isInstance)
                                .map(JavaUtf8ConstantNode.class::cast)
                                .filter(constant -> constant.value().equals("value"))
                                .findFirst()
                                .orElseThrow();
        byte[] replacement = "odd(x".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(
                replacement,
                0,
                bytes,
                Math.toIntExact(name.span().end()) - replacement.length,
                replacement.length);
        ClassFileIndexer.IndexedClass indexed =
                new ClassFileIndexer().index("Unusual.class", bytes);
        List<MemberSymbol> methods =
                indexed.file().members().stream()
                        .filter(member -> member.name().equals("odd(x"))
                        .toList();
        assertEquals(2, methods.size());
        methods.forEach(method -> assertEquals("odd(x", method.nameSpan().slice(indexed.text())));
        assertTrue(indexed.file().problems().isEmpty());
    }

    static final class Dollar$Literal {}

    static final class Holder<T extends Number> {
        Map<String, List<T>> values;
    }

    @Test
    void literalDollarSignsAndGenericArgumentPositionsSurviveTheProjection() throws Exception {
        ClassFileIndexer indexer = new ClassFileIndexer();
        try (InputStream input =
                Dollar$Literal.class.getResourceAsStream(
                        "BinaryIndexProjectionTest$Dollar$Literal.class")) {
            ClassFileIndexer.IndexedClass indexed =
                    indexer.index("Dollar.class", input.readAllBytes());
            assertEquals(
                    "org.jmouse.java.index.BinaryIndexProjectionTest.Dollar$Literal",
                    indexed.file().types().getFirst().qualifiedName());
            assertEquals(
                    "Dollar$Literal",
                    indexed.file().types().getFirst().nameSpan().slice(indexed.text()));
        }
        try (InputStream input =
                Holder.class.getResourceAsStream("BinaryIndexProjectionTest$Holder.class")) {
            ClassFileIndexer.IndexedClass indexed =
                    indexer.index("Holder.class", input.readAllBytes());
            IndexedReference argument =
                    indexed.file().references().stream()
                            .filter(
                                    reference ->
                                            reference.name().equals("T")
                                                    && reference.role()
                                                            == TypeReferenceRole.TYPE_ARGUMENT)
                            .findFirst()
                            .orElseThrow();
            assertEquals("T", argument.span().slice(indexed.text()));
            assertEquals(
                    argument,
                    indexed.file().referenceAt(argument.span().start().offset()).orElseThrow());
        }
    }

    @Test
    void memberMatchersAndOutputLimitsAreConfiguredWithoutChangingTheParser() throws Exception {
        byte[] bytes;
        try (InputStream input =
                Bridge.class.getResourceAsStream("BinaryIndexProjectionTest$Bridge.class")) {
            bytes = input.readAllBytes();
        }
        ClassFileProjectionConfiguration defaults = ClassFileProjectionConfiguration.standard();
        ClassFileProjectionConfiguration filtered =
                defaults.withMembers(member -> TextMatchers.same("value").matches(member.name()));
        assertEquals(
                2,
                new ClassFileIndexer(filtered)
                        .index("Bridge.class", bytes)
                        .file()
                        .members()
                        .size());
        BinaryProjectionLimits limits = defaults.limits();
        ClassFileProjectionConfiguration bounded =
                defaults.withLimits(
                        new BinaryProjectionLimits(
                                32, limits.maximumCachedNameCharacters(), limits.maximumWork()));
        assertThrows(
                SourceWritingLimitException.class,
                () -> new ClassFileIndexer(bounded).index("Bridge.class", bytes));
    }

    interface Generic<T> {
        T value();
    }

    static final class Bridge implements Generic<String> {
        @Override
        public String value() {
            return "value";
        }
    }

    @Test
    void binaryProjectionRetainsBridgeMethodsAndUsesRealViewSpansWithoutReparsingJava()
            throws Exception {
        byte[] bytes;
        try (InputStream input =
                Bridge.class.getResourceAsStream("BinaryIndexProjectionTest$Bridge.class")) {
            bytes = input.readAllBytes();
        }
        ClassFileIndexer.IndexedClass indexed = new ClassFileIndexer().index("Bridge.class", bytes);
        List<MemberSymbol> methods =
                indexed.file().members().stream()
                        .filter(member -> member.name().equals("value"))
                        .toList();
        assertEquals(2, methods.size());
        assertEquals(2, methods.stream().map(MemberSymbol::symbolId).distinct().count());
        indexed.file()
                .declarations()
                .forEach(
                        declaration ->
                                assertEquals(
                                        declaration.name(),
                                        declaration.nameSpan().slice(indexed.text())));
        assertTrue(indexed.file().problems().isEmpty());
    }
}
