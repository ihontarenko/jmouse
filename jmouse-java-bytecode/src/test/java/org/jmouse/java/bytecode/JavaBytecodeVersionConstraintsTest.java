package org.jmouse.java.bytecode;

import static org.junit.jupiter.api.Assertions.*;

import org.jmouse.java.bytecode.node.JavaClassFileNode;
import org.jmouse.java.bytecode.node.JavaMethodNode;
import org.jmouse.java.bytecode.node.attribute.JavaModuleAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaModuleRequireNode;
import org.jmouse.java.bytecode.node.constant.JavaModuleConstantNode;
import org.jmouse.java.bytecode.node.constant.JavaUtf8ConstantNode;
import org.jmouse.java.bytecode.vocabulary.JvmMethodAccessFlag;
import org.jmouse.java.bytecode.vocabulary.JvmModuleRequiresFlag;
import org.jmouse.java.model.symbol.JavaJvmNames;
import org.jmouse.java.model.version.BytecodeProfile;
import org.jmouse.java.model.version.JavaRelease;
import org.jmouse.java.model.version.JavaSpecificationCatalog;
import org.jmouse.java.model.version.PreviewPolicy;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Driver;

class JavaBytecodeVersionConstraintsTest {
    @Test
    void recordAndSealedAttributesFollowTheirHistoricalPreviewVersions() {
        int[][] cases = {{14, 58, 0}, {15, 59, 0}, {15, 59, 1}, {16, 60, 1}};
        for (int[] example : cases) {
            JavaRelease release = new JavaRelease(example[0]);
            JavaBytecodeLanguage language =
                    JavaBytecodeLanguage.builder()
                            .profile(
                                    new BytecodeProfile(
                                            release,
                                            PreviewPolicy.enabled(release),
                                            JavaSpecificationCatalog.standard()))
                            .build();
            ClassWriter writer = new ClassWriter(0);
            int access =
                    Opcodes.ACC_PUBLIC
                            | Opcodes.ACC_SUPER
                            | (example[2] == 0 ? Opcodes.ACC_FINAL | Opcodes.ACC_RECORD : 0);
            writer.visit(
                    Opcodes.V_PREVIEW | example[1],
                    access,
                    "sample/Preview",
                    null,
                    example[2] == 0 ? "java/lang/Record" : "java/lang/Object",
                    null);
            if (example[2] == 0) {
                writer.visitRecordComponent("value", "I", null).visitEnd();
            } else {
                writer.visitPermittedSubclass("sample/Child");
            }
            writer.visitEnd();
            byte[] bytes = writer.toByteArray();
            JavaBytecodeParser parser =
                    new JavaBytecodeParser(language, JavaBytecodeLimits.standard());
            assertTrue(
                    parser.parse("preview.class", bytes).isComplete(),
                    "release " + release.number());
            assertFalse(
                    new JavaBytecodeParser()
                            .parse("historical-preview.class", bytes, JavaBytecodeMode.INSPECTION)
                            .isComplete());
            bytes[4] = 0;
            bytes[5] = 0;
            assertThrows(
                    IllegalArgumentException.class,
                    () -> parser.parse("preview-marker-missing.class", bytes));
        }
    }

    @Test
    void modernModulesCannotRequireJavaBaseTransitively() throws Exception {
        byte[] bytes;
        try (InputStream input = Driver.class.getResourceAsStream("/module-info.class")) {
            bytes = input.readAllBytes();
        }
        JavaClassFileNode root =
                new JavaBytecodeParser()
                        .parse("java.sql/module-info.class", bytes)
                        .requireComplete();
        JavaModuleAttributeNode module =
                root.attributes().stream()
                        .filter(JavaModuleAttributeNode.class::isInstance)
                        .map(JavaModuleAttributeNode.class::cast)
                        .findFirst()
                        .orElseThrow();
        JavaModuleRequireNode requirement =
                module.requires().stream()
                        .filter(
                                entry -> {
                                    JavaModuleConstantNode name =
                                            root.pool()
                                                    .require(
                                                            entry.moduleIndex(),
                                                            JavaModuleConstantNode.class);
                                    return root.pool().utf8(name.nameIndex()).equals("java.base");
                                })
                        .findFirst()
                        .orElseThrow();
        int offset = Math.toIntExact(requirement.span().start()) + 2;
        int flags = requirement.accessFlags() | JvmModuleRequiresFlag.TRANSITIVE.code();
        bytes[offset] = (byte) (flags >>> Byte.SIZE);
        bytes[offset + 1] = (byte) flags;
        assertThrows(
                IllegalArgumentException.class,
                () -> new JavaBytecodeParser().parse("transitive-base.class", bytes));
    }

    @Test
    void moduleNamesAdmitEscapesAndRejectUnescapedReservedCharacters() throws Exception {
        byte[] bytes;
        try (InputStream input = Driver.class.getResourceAsStream("/module-info.class")) {
            bytes = input.readAllBytes();
        }
        JavaClassFileNode root =
                new JavaBytecodeParser()
                        .parse("java.sql/module-info.class", bytes)
                        .requireComplete();
        JavaModuleAttributeNode module =
                root.attributes().stream()
                        .filter(JavaModuleAttributeNode.class::isInstance)
                        .map(JavaModuleAttributeNode.class::cast)
                        .findFirst()
                        .orElseThrow();
        JavaModuleConstantNode identity =
                root.pool().require(module.moduleNameIndex(), JavaModuleConstantNode.class);
        JavaUtf8ConstantNode name =
                root.pool().require(identity.nameIndex(), JavaUtf8ConstantNode.class);
        int offset = Math.toIntExact(name.span().end()) - 8;
        System.arraycopy("app\\:sql".getBytes(StandardCharsets.UTF_8), 0, bytes, offset, 8);
        assertDoesNotThrow(() -> new JavaBytecodeParser().parse("escaped-module.class", bytes));
        System.arraycopy("app:@sql".getBytes(StandardCharsets.UTF_8), 0, bytes, offset, 8);
        IllegalArgumentException invalid =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new JavaBytecodeParser().parse("invalid-module.class", bytes));
        assertTrue(invalid.getMessage().contains("module name"));
    }

    interface AbstractFixture {
        void run();
    }

    @Test
    void interfaceMethodsCannotHaveProtectedAccess() throws Exception {
        byte[] bytes;
        try (InputStream input =
                AbstractFixture.class.getResourceAsStream(
                        "JavaBytecodeVersionConstraintsTest$AbstractFixture.class")) {
            bytes = input.readAllBytes();
        }
        JavaMethodNode method =
                new JavaBytecodeParser()
                        .parse("AbstractFixture.class", bytes)
                        .requireComplete()
                        .methods()
                        .getFirst();
        int flags =
                (method.accessFlags() & ~JvmMethodAccessFlag.PUBLIC.code())
                        | JvmMethodAccessFlag.PROTECTED.code();
        int offset = Math.toIntExact(method.span().start());
        bytes[offset] = (byte) (flags >>> Byte.SIZE);
        bytes[offset + 1] = (byte) flags;
        assertThrows(
                IllegalArgumentException.class,
                () -> new JavaBytecodeParser().parse("protected-interface.class", bytes));
    }

    @Test
    void strictFlagIsForbiddenOnAnAbstractMethodOnlyWhileThatFlagHasDefinedSemantics()
            throws Exception {
        byte[] bytes;
        try (InputStream input =
                AbstractFixture.class.getResourceAsStream(
                        "JavaBytecodeVersionConstraintsTest$AbstractFixture.class")) {
            bytes = input.readAllBytes();
        }
        JavaMethodNode method =
                new JavaBytecodeParser()
                        .parse("AbstractFixture.class", bytes).requireComplete().methods().stream()
                                .filter(candidate -> candidate.name().equals("run"))
                                .findFirst()
                                .orElseThrow();
        int flags = method.accessFlags() | JvmMethodAccessFlag.STRICT.code();
        int offset = Math.toIntExact(method.span().start());
        bytes[offset] = (byte) (flags >>> Byte.SIZE);
        bytes[offset + 1] = (byte) flags;
        assertDoesNotThrow(
                () -> new JavaBytecodeParser().parse("reserved-strict-bit.class", bytes));
        bytes[6] = 0;
        bytes[7] = 60;
        assertThrows(
                IllegalArgumentException.class,
                () -> new JavaBytecodeParser().parse("abstract-strict.class", bytes));
    }

    static final class InitializationFixture {
        static int value = Math.abs(-42);
    }

    @Test
    void classInitializerIgnoresFlagsThatWouldBeIllegalForOrdinaryMethods() throws Exception {
        byte[] bytes;
        try (InputStream input =
                InitializationFixture.class.getResourceAsStream(
                        "JavaBytecodeVersionConstraintsTest$InitializationFixture.class")) {
            bytes = input.readAllBytes();
        }
        JavaClassFileNode root =
                new JavaBytecodeParser()
                        .parse("InitializationFixture.class", bytes)
                        .requireComplete();
        JavaMethodNode initializer =
                root.methods().stream()
                        .filter(method -> method.name().equals(JavaJvmNames.CLASS_INITIALIZER))
                        .findFirst()
                        .orElseThrow();
        int flags =
                initializer.accessFlags()
                        | JvmMethodAccessFlag.PUBLIC.code()
                        | JvmMethodAccessFlag.PRIVATE.code()
                        | JvmMethodAccessFlag.ABSTRACT.code()
                        | JvmMethodAccessFlag.NATIVE.code();
        int offset = Math.toIntExact(initializer.span().start());
        bytes[offset] = (byte) (flags >>> Byte.SIZE);
        bytes[offset + 1] = (byte) flags;
        JavaClassFileNode accepted =
                assertDoesNotThrow(
                        () ->
                                new JavaBytecodeParser()
                                        .parse("flagged-initializer.class", bytes)
                                        .requireComplete());
        assertEquals(
                flags,
                accepted.methods().stream()
                        .filter(method -> method.name().equals(JavaJvmNames.CLASS_INITIALIZER))
                        .findFirst()
                        .orElseThrow()
                        .accessFlags());
    }
}
