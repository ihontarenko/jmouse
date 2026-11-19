package org.jmouse.java.bytecode.signature;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.bytecode.vocabulary.JvmSignatureToken;
import org.jmouse.java.model.descriptor.JavaArrayDescriptor;
import org.jmouse.java.model.type.JavaPrimitiveType;
import org.jmouse.java.model.type.JavaVoidType;

/** FIELD and RETURN are separate roles: void is admitted only as a complete return type. */
public final class JvmDescriptorTypeParser
        implements ParseRule<JvmTextInput, JvmDescriptorTypeNode> {
    private final Role role;

    public enum Role {
        FIELD,
        RETURN
    }

    public JvmDescriptorTypeParser(Role role) {
        this.role = role;
    }

    @Override
    public JvmDescriptorTypeNode parse(JvmTextInput input, ParseSession session) {
        int start = input.offset();
        if (input.take(JvmSignatureToken.ARRAY, session)) {
            int dimensions = 1;
            while (input.take(JvmSignatureToken.ARRAY, session)) {
                if (++dimensions > JavaArrayDescriptor.MAXIMUM_DIMENSIONS) {
                    throw input.failure("JVM array rank exceeds the format limit");
                }
            }
            JvmDescriptorTypeNode element = session.parse(JvmDescriptorRoles.FIELD, input);
            return new JvmArrayDescriptorNode(
                    input.cursor().source(), start, input.offset(), dimensions, element);
        }
        if (input.take(JvmSignatureToken.CLASS, session)) {
            int nameStart = input.offset();
            while (!input.atEnd() && !input.matches(JvmSignatureToken.SEMICOLON)) {
                input.next(session);
            }
            String name = input.slice(nameStart, input.offset());
            input.expect(JvmSignatureToken.SEMICOLON, session);
            return new JvmClassDescriptorNode(input.cursor().source(), start, input.offset(), name);
        }
        if (role == Role.RETURN && input.take(JvmSignatureToken.VOID, session)) {
            return new JvmScalarDescriptorNode(
                    input.cursor().source(), start, input.offset(), JavaVoidType.VOID);
        }
        if (!(input.cursor().peek().type() instanceof JvmSignatureToken terminal)) {
            throw input.failure("Expected a JVM descriptor type");
        }
        JavaPrimitiveType primitive =
                switch (terminal) {
                    case BYTE -> JavaPrimitiveType.BYTE;
                    case CHAR -> JavaPrimitiveType.CHAR;
                    case DOUBLE -> JavaPrimitiveType.DOUBLE;
                    case FLOAT -> JavaPrimitiveType.FLOAT;
                    case INT -> JavaPrimitiveType.INT;
                    case LONG -> JavaPrimitiveType.LONG;
                    case SHORT -> JavaPrimitiveType.SHORT;
                    case BOOLEAN -> JavaPrimitiveType.BOOLEAN;
                    default -> throw input.failure("Expected a non-void JVM descriptor type");
                };
        input.next(session);
        return new JvmScalarDescriptorNode(
                input.cursor().source(), start, input.offset(), primitive);
    }
}
