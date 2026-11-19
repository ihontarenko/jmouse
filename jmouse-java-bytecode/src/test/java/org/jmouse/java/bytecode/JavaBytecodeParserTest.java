package org.jmouse.java.bytecode;

import static org.junit.jupiter.api.Assertions.*;

import org.jmouse.el.node.NodeLifecycle;
import org.jmouse.java.bytecode.node.JavaClassFileNode;
import org.jmouse.java.bytecode.node.attribute.JavaBootstrapMethodsAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaCodeAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaStackMapTableAttributeNode;
import org.jmouse.java.bytecode.node.constant.JavaUtf8ConstantNode;
import org.jmouse.java.bytecode.node.instruction.JavaBranchInstructionNode;
import org.jmouse.java.bytecode.node.instruction.JavaConstantInstructionNode;
import org.jmouse.java.bytecode.vocabulary.JvmOpcode;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

class JavaBytecodeParserTest {
    @Test
    void obsoleteSubroutinesAreRejectedInModernClassfileVersions() throws Exception {
        byte[] bytes;
        try (InputStream input =
                Fixture.class.getResourceAsStream("JavaBytecodeParserTest$Fixture.class")) {
            bytes = input.readAllBytes();
        }
        JavaClassFileNode root =
                new JavaBytecodeParser().parse("Fixture.class", bytes).requireComplete();
        JavaConstantInstructionNode call =
                root.methods().stream()
                        .flatMap(method -> method.attributes().stream())
                        .filter(JavaCodeAttributeNode.class::isInstance)
                        .map(JavaCodeAttributeNode.class::cast)
                        .flatMap(code -> code.instructions().stream())
                        .filter(JavaConstantInstructionNode.class::isInstance)
                        .map(JavaConstantInstructionNode.class::cast)
                        .filter(instruction -> instruction.opcode() == JvmOpcode.INVOKESPECIAL)
                        .findFirst()
                        .orElseThrow();
        int offset = Math.toIntExact(call.span().start());
        bytes[offset] = (byte) JvmOpcode.JSR.code();
        bytes[offset + 1] = 0;
        bytes[offset + 2] = 0;
        assertThrows(
                IllegalArgumentException.class,
                () -> new JavaBytecodeParser().parse("obsolete-subroutine.class", bytes));
    }

    @Test
    void stackMapFramesMustStartAtAnInstructionBoundary() throws Exception {
        byte[] bytes;
        try (InputStream input =
                BranchFixture.class.getResourceAsStream(
                        "JavaBytecodeParserTest$BranchFixture.class")) {
            bytes = input.readAllBytes();
        }
        JavaClassFileNode root =
                new JavaBytecodeParser().parse("BranchFixture.class", bytes).requireComplete();
        JavaStackMapTableAttributeNode stackMap =
                root.methods().stream()
                        .filter(method -> method.name().equals("choose"))
                        .flatMap(method -> method.attributes().stream())
                        .filter(JavaCodeAttributeNode.class::isInstance)
                        .map(JavaCodeAttributeNode.class::cast)
                        .flatMap(code -> code.attributes().stream())
                        .filter(JavaStackMapTableAttributeNode.class::isInstance)
                        .map(JavaStackMapTableAttributeNode.class::cast)
                        .findFirst()
                        .orElseThrow();
        bytes[Math.toIntExact(stackMap.entries().getFirst().span().start())] = 2;
        assertThrows(
                IllegalArgumentException.class,
                () -> new JavaBytecodeParser().parse("broken-stack-map.class", bytes));
    }

    @Test
    void bootstrapMethodsRequireAMethodHandleRatherThanAnArbitraryPoolEntry() throws Exception {
        byte[] bytes;
        try (InputStream input =
                ComplexFixture.class.getResourceAsStream(
                        "JavaBytecodeParserTest$ComplexFixture.class")) {
            bytes = input.readAllBytes();
        }
        JavaClassFileNode root =
                new JavaBytecodeParser().parse("ComplexFixture.class", bytes).requireComplete();
        JavaBootstrapMethodsAttributeNode table =
                root.attributes().stream()
                        .filter(JavaBootstrapMethodsAttributeNode.class::isInstance)
                        .map(JavaBootstrapMethodsAttributeNode.class::cast)
                        .findFirst()
                        .orElseThrow();
        int operand = Math.toIntExact(table.entries().getFirst().span().start());
        int wrongIndex =
                root.pool().entries().stream()
                        .filter(JavaUtf8ConstantNode.class::isInstance)
                        .findFirst()
                        .orElseThrow()
                        .index();
        bytes[operand] = (byte) (wrongIndex >>> Byte.SIZE);
        bytes[operand + 1] = (byte) wrongIndex;
        assertThrows(
                IllegalArgumentException.class,
                () -> new JavaBytecodeParser().parse("broken-bootstrap.class", bytes));
    }

    @Test
    void realJdkClassesExerciseTheSamePublicParserWithoutHostClassLoading() throws Exception {
        String[] resources = {
            "/java/lang/Object.class",
            "/java/lang/String.class",
            "/java/lang/Class.class",
            "/java/lang/Record.class",
            "/java/lang/invoke/MethodHandles.class",
            "/java/lang/invoke/MethodHandle.class",
            "/java/util/ArrayList.class",
            "/java/util/HashMap.class",
            "/java/util/HashMap$TreeNode.class",
            "/java/util/LinkedHashMap.class",
            "/java/util/Collections.class",
            "/java/util/Optional.class",
            "/java/util/stream/Collectors.class",
            "/java/util/concurrent/ConcurrentHashMap.class",
            "/java/util/concurrent/CompletableFuture.class",
            "/java/math/BigInteger.class",
            "/java/lang/module/ModuleDescriptor.class",
            "/java/lang/annotation/Retention.class"
        };
        JavaBytecodeParser parser = new JavaBytecodeParser();
        for (String resource : resources) {
            try (InputStream input = Object.class.getResourceAsStream(resource)) {
                assertNotNull(input, resource);
                JavaClassFileNode root =
                        parser.parse(resource, input.readAllBytes()).requireComplete();
                assertEquals(
                        resource.substring(1, resource.length() - ".class".length()),
                        root.internalName());
            }
        }
    }

    @Test
    void invocationOperandsMustReferenceTheRequiredConstantKind() throws Exception {
        byte[] bytes;
        try (InputStream input =
                Fixture.class.getResourceAsStream("JavaBytecodeParserTest$Fixture.class")) {
            bytes = input.readAllBytes();
        }
        JavaClassFileNode root =
                new JavaBytecodeParser().parse("Fixture.class", bytes).requireComplete();
        JavaConstantInstructionNode call =
                root.methods().stream()
                        .flatMap(method -> method.attributes().stream())
                        .filter(JavaCodeAttributeNode.class::isInstance)
                        .map(JavaCodeAttributeNode.class::cast)
                        .flatMap(code -> code.instructions().stream())
                        .filter(JavaConstantInstructionNode.class::isInstance)
                        .map(JavaConstantInstructionNode.class::cast)
                        .filter(instruction -> instruction.opcode() == JvmOpcode.INVOKESPECIAL)
                        .findFirst()
                        .orElseThrow();
        int wrongIndex =
                root.pool().entries().stream()
                        .filter(JavaUtf8ConstantNode.class::isInstance)
                        .findFirst()
                        .orElseThrow()
                        .index();
        int operand = Math.toIntExact(call.span().start()) + 1;
        bytes[operand] = (byte) (wrongIndex >>> Byte.SIZE);
        bytes[operand + 1] = (byte) wrongIndex;
        assertThrows(
                IllegalArgumentException.class,
                () -> new JavaBytecodeParser().parse("broken-call.class", bytes));
    }

    static final class BranchFixture {
        static int local(int input) {
            int value = input + 1;
            return value;
        }

        int choose(boolean input) {
            if (input) {
                return 1;
            }
            return 2;
        }
    }

    @Test
    void compactLocalInstructionsMustFitMaximumLocals() throws Exception {
        byte[] bytes;
        try (InputStream input =
                BranchFixture.class.getResourceAsStream(
                        "JavaBytecodeParserTest$BranchFixture.class")) {
            bytes = input.readAllBytes();
        }
        JavaClassFileNode root =
                new JavaBytecodeParser().parse("BranchFixture.class", bytes).requireComplete();
        JavaCodeAttributeNode code =
                root.methods().stream()
                        .filter(method -> method.name().equals("local"))
                        .flatMap(method -> method.attributes().stream())
                        .filter(JavaCodeAttributeNode.class::isInstance)
                        .map(JavaCodeAttributeNode.class::cast)
                        .findFirst()
                        .orElseThrow();
        int maximumLocals = Math.toIntExact(code.span().start()) + 8;
        bytes[maximumLocals] = 0;
        bytes[maximumLocals + 1] = 1;
        assertThrows(
                IllegalArgumentException.class,
                () -> new JavaBytecodeParser().parse("broken-local.class", bytes));
    }

    @Test
    void branchTargetsMustPointToInstructionBoundaries() throws Exception {
        byte[] bytes;
        try (InputStream input =
                BranchFixture.class.getResourceAsStream(
                        "JavaBytecodeParserTest$BranchFixture.class")) {
            bytes = input.readAllBytes();
        }
        JavaClassFileNode root =
                new JavaBytecodeParser().parse("BranchFixture.class", bytes).requireComplete();
        JavaBranchInstructionNode branch =
                root.methods().stream()
                        .flatMap(method -> method.attributes().stream())
                        .filter(JavaCodeAttributeNode.class::isInstance)
                        .map(JavaCodeAttributeNode.class::cast)
                        .flatMap(code -> code.instructions().stream())
                        .filter(JavaBranchInstructionNode.class::isInstance)
                        .map(JavaBranchInstructionNode.class::cast)
                        .findFirst()
                        .orElseThrow();
        int operand = Math.toIntExact(branch.span().start()) + 1;
        bytes[operand] = 0;
        bytes[operand + 1] = 1;
        assertThrows(
                IllegalArgumentException.class,
                () -> new JavaBytecodeParser().parse("broken-branch.class", bytes));
    }

    @Test
    void malformedMemberDescriptorsAreRejectedEvenWhenTheBinaryLayoutIsReadable() throws Exception {
        byte[] bytes;
        try (InputStream input =
                Fixture.class.getResourceAsStream("JavaBytecodeParserTest$Fixture.class")) {
            bytes = input.readAllBytes();
        }
        JavaClassFileNode original =
                new JavaBytecodeParser().parse("Fixture.class", bytes).requireComplete();
        JavaUtf8ConstantNode descriptor =
                original.pool().entries().stream()
                        .filter(JavaUtf8ConstantNode.class::isInstance)
                        .map(JavaUtf8ConstantNode.class::cast)
                        .filter(constant -> constant.value().equals("(I)I"))
                        .findFirst()
                        .orElseThrow();
        bytes[Math.toIntExact(descriptor.span().end()) - 3] = 'V';
        assertThrows(
                IllegalArgumentException.class,
                () -> new JavaBytecodeParser().parse("malformed.class", bytes));
    }

    @Test
    void thePlatformModuleDescriptorRetainsItsModuleTables() throws Exception {
        byte[] bytes;
        try (InputStream input = String.class.getResourceAsStream("/module-info.class")) {
            bytes = input.readAllBytes();
        }
        JavaClassFileNode root =
                new JavaBytecodeParser()
                        .parse("java.base/module-info.class", bytes)
                        .requireComplete();
        assertEquals("module-info", root.internalName());
        assertTrue(
                root.attributes().stream()
                        .anyMatch(attribute -> attribute.name().equals("Module")));
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.TYPE_USE, ElementType.PARAMETER, ElementType.METHOD})
    @interface Marker {
        String value() default "marker";

        Class<?> type() default String.class;

        int[] numbers() default {1, 2};

        RetentionPolicy policy() default RetentionPolicy.RUNTIME;
    }

    @Marker
    record ComplexFixture(@Marker String value) {
        @Marker
        String run(@Marker int input) {
            Runnable callback = () -> value.trim();
            callback.run();
            return switch (input) {
                case 1 -> value;
                case 8 -> "eight";
                default -> "other";
            };
        }
    }

    @Test
    void recordsAnnotationsStackMapsAndBootstrapMethodsAreParsedAsKnownStructures()
            throws Exception {
        for (Class<?> type : new Class<?>[] {ComplexFixture.class, Marker.class}) {
            byte[] bytes;
            try (InputStream input =
                    type.getResourceAsStream(
                            type.getSimpleName().equals("Marker")
                                    ? "JavaBytecodeParserTest$Marker.class"
                                    : "JavaBytecodeParserTest$ComplexFixture.class")) {
                bytes = input.readAllBytes();
            }
            JavaClassFileNode root =
                    new JavaBytecodeParser().parse(type.getName(), bytes).requireComplete();
            assertEquals(type.getName().replace('.', '/'), root.internalName());
        }
    }

    static final class Fixture {
        static final int ANSWER = 42;

        int value(int input) {
            return input + ANSWER;
        }
    }

    @Test
    void classBytesRetainCanonicalDescriptorsCodeAndExactOwnedSource() throws Exception {
        byte[] bytes;
        try (InputStream input =
                Fixture.class.getResourceAsStream("JavaBytecodeParserTest$Fixture.class")) {
            bytes = input.readAllBytes();
        }
        JavaBytecodeResult result = new JavaBytecodeParser().parse("Fixture.class", bytes);
        JavaClassFileNode root = result.requireComplete();
        assertEquals(NodeLifecycle.FROZEN, root.lifecycle());
        assertEquals(
                "org/jmouse/java/bytecode/JavaBytecodeParserTest$Fixture", root.internalName());
        JavaCodeAttributeNode code =
                root.methods().stream()
                        .filter(method -> method.name().equals("value"))
                        .peek(method -> assertEquals("(I)I", method.descriptor()))
                        .findFirst()
                        .orElseThrow()
                        .attributes()
                        .stream()
                        .filter(JavaCodeAttributeNode.class::isInstance)
                        .map(JavaCodeAttributeNode.class::cast)
                        .findFirst()
                        .orElseThrow();
        assertTrue(
                code.instructions().stream()
                        .anyMatch(instruction -> instruction.opcode() == JvmOpcode.IRETURN));
        byte original = result.source().slice(0, 1)[0];
        bytes[0] = 0;
        assertEquals(original, result.source().slice(0, 1)[0]);
    }
}
