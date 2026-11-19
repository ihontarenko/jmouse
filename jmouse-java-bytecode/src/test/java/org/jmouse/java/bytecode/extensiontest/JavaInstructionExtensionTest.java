package org.jmouse.java.bytecode.extensiontest;

import static org.junit.jupiter.api.Assertions.*;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.el.translate.NodeOperationBindingException;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.JavaBytecodeLanguage;
import org.jmouse.java.bytecode.JavaBytecodeLimits;
import org.jmouse.java.bytecode.JavaBytecodeParser;
import org.jmouse.java.bytecode.grammar.JavaBytecodeRoles;
import org.jmouse.java.bytecode.node.JavaClassFileNode;
import org.jmouse.java.bytecode.node.attribute.JavaCodeAttributeNode;
import org.jmouse.java.bytecode.node.instruction.JavaInstructionNode;
import org.jmouse.java.bytecode.node.instruction.JvmInstructionEncoding;
import org.jmouse.java.bytecode.validation.JvmValidationOutcome;
import org.jmouse.java.bytecode.vocabulary.JvmInstructionKind;
import org.jmouse.java.bytecode.vocabulary.JvmOpcode;
import org.jmouse.java.model.version.*;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

class JavaInstructionExtensionTest {
    private static final String NODE = "test.bytecode.probe";
    private static final String GRAMMAR = "test.bytecode.probe-grammar";
    private static final String VALIDATOR = "test.bytecode.probe-validator";
    private static final String PROFILE = "test.bytecode.synthetic-release-27";
    private static final int EXAMPLE_MAJOR = 71;

    private enum ProbeKind implements JvmInstructionKind {
        PROBE;

        @Override
        public int code() {
            return 203;
        }

        @Override
        public String mnemonic() {
            return "probe";
        }
    }

    private static final class ProbeNode extends JavaInstructionNode {
        private static final NodeKind<ProbeNode> KIND = new NodeKind<>(NODE, ProbeNode.class);
        private static final NodeSchema<ProbeNode> SCHEMA = NodeSchema.of(KIND);
        private final int value;

        ProbeNode(BytesSource source, BytecodeSpan span, int offset, int value) {
            super(SCHEMA, source, span, ProbeKind.PROBE, offset, JvmInstructionEncoding.ORDINARY);
            this.value = value;
        }
    }

    @Test
    void aFutureProfileCanContributeAnOpcodeItsOwnNodeAndValidationWithoutCoreChanges()
            throws Exception {
        byte[] bytes;
        try (InputStream input =
                getClass()
                        .getResourceAsStream(
                                "/org/jmouse/java/bytecode/JavaBytecodeParserTest$Fixture.class")) {
            bytes = input.readAllBytes();
        }
        JavaClassFileNode original =
                new JavaBytecodeParser().parse("Fixture.class", bytes).requireComplete();
        JavaInstructionNode immediate =
                original.methods().stream()
                        .flatMap(method -> method.attributes().stream())
                        .filter(JavaCodeAttributeNode.class::isInstance)
                        .map(JavaCodeAttributeNode.class::cast)
                        .flatMap(code -> code.instructions().stream())
                        .filter(instruction -> instruction.opcode() == JvmOpcode.BIPUSH)
                        .findFirst()
                        .orElseThrow();
        bytes[Math.toIntExact(immediate.span().start())] = (byte) ProbeKind.PROBE.code();
        bytes[6] = 0;
        bytes[7] = EXAMPLE_MAJOR;
        JavaBytecodeLanguage.Builder extension =
                JavaBytecodeLanguage.builder().profile(exampleProfile());
        extension
                .rules()
                .decorate(
                        JavaBytecodeRoles.INSTRUCTION,
                        extension.rules().identity(JavaBytecodeRoles.INSTRUCTION),
                        GRAMMAR,
                        standard ->
                                (input, session) -> {
                                    if (input.bytes().lookAt(0) != ProbeKind.PROBE.code()) {
                                        return standard.parse(input, session);
                                    }
                                    long start = input.bytes().position();
                                    input.bytes().unsigned8();
                                    int value = input.bytes().signed8();
                                    return new ProbeNode(
                                            input.bytes().source(),
                                            new BytecodeSpan(start, input.bytes().position()),
                                            Math.toIntExact(start - input.codeStart()),
                                            value);
                                });
        JavaBytecodeParser decoderOnly =
                new JavaBytecodeParser(extension.build(), JavaBytecodeLimits.standard());
        assertThrows(
                NodeOperationBindingException.class,
                () -> decoderOnly.parse("example.class", bytes));
        extension
                .validations()
                .bind(
                        ProbeNode.KIND,
                        VALIDATOR,
                        (node, context) -> {
                            context.require(node.value == 42, node, "Invalid probe payload");
                            return JvmValidationOutcome.VALID;
                        });
        JavaClassFileNode root =
                new JavaBytecodeParser(extension.build(), JavaBytecodeLimits.standard())
                        .parse("example.class", bytes)
                        .requireComplete();
        ProbeNode probe =
                root.methods().stream()
                        .flatMap(method -> method.attributes().stream())
                        .filter(JavaCodeAttributeNode.class::isInstance)
                        .map(JavaCodeAttributeNode.class::cast)
                        .flatMap(code -> code.instructions().stream())
                        .filter(ProbeNode.class::isInstance)
                        .map(ProbeNode.class::cast)
                        .findFirst()
                        .orElseThrow();
        assertEquals(ProbeKind.PROBE, probe.opcodeKind());
        assertEquals(203, probe.opcodeKind().code());
        assertEquals(42, probe.value);
        assertThrows(IllegalStateException.class, probe::opcode);
        assertThrows(
                IllegalArgumentException.class,
                () -> new JavaBytecodeParser().parse("example.class", bytes));
    }

    /** An invented test profile, not a claim about a future Java standard. */
    private static BytecodeProfile exampleProfile() {
        JavaSpecificationCatalog standard = JavaSpecificationCatalog.standard();
        Map<JavaRelease, ClassFileVersion> releases = new HashMap<>();
        standard.knownReleases()
                .forEach(release -> releases.put(release, standard.classFileVersion(release)));
        JavaRelease example = new JavaRelease(27);
        releases.put(example, new ClassFileVersion(EXAMPLE_MAJOR, 0));
        JavaSpecificationCatalog specification =
                new JavaSpecificationCatalog(
                        PROFILE, example, releases, standard.features(), standard.classFileRules());
        return new BytecodeProfile(example, PreviewPolicy.disabled(), specification);
    }
}
