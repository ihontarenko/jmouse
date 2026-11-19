package org.jmouse.java.bytecode;

import static org.junit.jupiter.api.Assertions.*;

import org.jmouse.java.bytecode.node.JavaClassFileNode;
import org.jmouse.java.bytecode.node.JavaMethodNode;
import org.jmouse.java.bytecode.vocabulary.JvmMethodAccessFlag;
import org.jmouse.java.bytecode.node.attribute.JavaModuleAttributeNode;
import org.jmouse.java.bytecode.node.constant.JavaModuleConstantNode;
import org.jmouse.java.bytecode.node.constant.JavaUtf8ConstantNode;
import org.jmouse.java.model.symbol.JavaJvmNames;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.sql.Driver;
import java.nio.charset.StandardCharsets;

class JavaBytecodeVersionConstraintsTest {
    @Test
    void moduleNamesAdmitEscapesAndRejectUnescapedReservedCharacters() throws Exception {
        byte[] bytes;
        try (InputStream input = Driver.class.getResourceAsStream("/module-info.class")) {
            bytes = input.readAllBytes();
        }
        JavaClassFileNode root = new JavaBytecodeParser().parse("java.sql/module-info.class", bytes).requireComplete();
        JavaModuleAttributeNode module = root.attributes().stream().filter(JavaModuleAttributeNode.class::isInstance)
                .map(JavaModuleAttributeNode.class::cast).findFirst().orElseThrow();
        JavaModuleConstantNode identity = root.pool().require(module.moduleNameIndex(), JavaModuleConstantNode.class);
        JavaUtf8ConstantNode name = root.pool().require(identity.nameIndex(), JavaUtf8ConstantNode.class);
        int offset = Math.toIntExact(name.span().end()) - 8;
        System.arraycopy("app\\:sql".getBytes(StandardCharsets.UTF_8), 0, bytes, offset, 8);
        assertDoesNotThrow(() -> new JavaBytecodeParser().parse("escaped-module.class", bytes));
        System.arraycopy("app:@sql".getBytes(StandardCharsets.UTF_8), 0, bytes, offset, 8);
        IllegalArgumentException invalid = assertThrows(IllegalArgumentException.class,
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
