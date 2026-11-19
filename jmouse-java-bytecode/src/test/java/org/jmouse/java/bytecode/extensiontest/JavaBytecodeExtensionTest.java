package org.jmouse.java.bytecode.extensiontest;

import static org.junit.jupiter.api.Assertions.*;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.el.translate.NodeOperationBindingException;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.JavaBytecodeLanguage;
import org.jmouse.java.bytecode.JavaBytecodeLimits;
import org.jmouse.java.bytecode.JavaBytecodeNames;
import org.jmouse.java.bytecode.JavaBytecodeParser;
import org.jmouse.java.bytecode.node.JavaClassFileNode;
import org.jmouse.java.bytecode.node.constant.JavaConstantNode;
import org.jmouse.java.bytecode.validation.JvmValidationOutcome;
import org.jmouse.java.bytecode.vocabulary.JvmConstantKind;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Arrays;

class JavaBytecodeExtensionTest {
    private static final String NODE_NAME = "test.bytecode.external-constant";
    private static final String DECODER_NAME = "test.bytecode.external-decoder";
    private static final String VALIDATOR_NAME = "test.bytecode.external-validator";

    private enum ExternalKind implements JvmConstantKind {
        EXTERNAL;

        @Override
        public int code() {
            return 238;
        }

        @Override
        public int slots() {
            return 1;
        }
    }

    private static final class ExternalConstant extends JavaConstantNode {
        static final NodeKind<ExternalConstant> KIND =
                new NodeKind<>(NODE_NAME, ExternalConstant.class);
        static final NodeSchema<ExternalConstant> SCHEMA = NodeSchema.of(KIND);
        private final int value;

        ExternalConstant(BytesSource source, BytecodeSpan span, int index, int value) {
            super(SCHEMA, source, span, index, ExternalKind.EXTERNAL);
            this.value = value;
        }

        int value() {
            return value;
        }
    }

    @Test
    void anExternalTagRequiresItsOwnDecoderAndValidatorWithoutChangingTheStandardLanguage()
            throws Exception {
        byte[] original;
        try (InputStream input =
                getClass()
                        .getResourceAsStream(
                                "/org/jmouse/java/bytecode/JavaBytecodeParserTest$Fixture.class")) {
            original = input.readAllBytes();
        }
        JavaClassFileNode fixture =
                new JavaBytecodeParser().parse("Fixture.class", original).requireComplete();
        int poolEnd = Math.toIntExact(fixture.pool().span().end());
        byte[] contributed = Arrays.copyOf(original, original.length + 2);
        System.arraycopy(original, poolEnd, contributed, poolEnd + 2, original.length - poolEnd);
        contributed[poolEnd] = (byte) ExternalKind.EXTERNAL.code();
        contributed[poolEnd + 1] = 42;
        int slots = fixture.pool().slotCount() + 1;
        contributed[8] = (byte) (slots >>> Byte.SIZE);
        contributed[9] = (byte) slots;

        JavaBytecodeLanguage.Builder extension = JavaBytecodeLanguage.builder();
        extension.constant(
                ExternalKind.EXTERNAL,
                DECODER_NAME,
                (input, session) -> {
                    int value = input.bytes().unsigned8();
                    return new ExternalConstant(
                            input.bytes().source(),
                            new BytecodeSpan(input.start(), input.bytes().position()),
                            input.index(),
                            value);
                });
        JavaBytecodeParser decoderOnly =
                new JavaBytecodeParser(extension.build(), JavaBytecodeLimits.standard());
        NodeOperationBindingException missing =
                assertThrows(
                        NodeOperationBindingException.class,
                        () -> decoderOnly.parse("external.class", contributed));
        assertEquals(NODE_NAME, missing.nodeKind());
        assertEquals(JavaBytecodeNames.VALIDATION, missing.operation());

        extension
                .validations()
                .bind(
                        ExternalConstant.KIND,
                        VALIDATOR_NAME,
                        (node, context) -> {
                            context.require(
                                    node.value() == 42, node, "Unexpected external constant value");
                            return JvmValidationOutcome.VALID;
                        });
        JavaBytecodeParser parser =
                new JavaBytecodeParser(extension.build(), JavaBytecodeLimits.standard());
        JavaClassFileNode result = parser.parse("external.class", contributed).requireComplete();
        assertEquals(42, ((ExternalConstant) result.pool().entry(slots - 1)).value());
        assertThrows(
                IllegalArgumentException.class,
                () -> new JavaBytecodeParser().parse("external.class", contributed));
        assertThrows(
                NodeOperationBindingException.class,
                () -> decoderOnly.parse("external.class", contributed));
    }
}
