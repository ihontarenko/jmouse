package org.jmouse.java.bytecode.signature;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.bytecode.vocabulary.JvmSignatureToken;

public final class JvmReferenceSignatureParser
        implements ParseRule<JvmTextInput, JvmReferenceTypeSignatureNode> {
    @Override
    public JvmReferenceTypeSignatureNode parse(JvmTextInput input, ParseSession session) {
        int start = input.offset();
        if (input.matches(JvmSignatureToken.CLASS)) {
            return session.parse(JvmSignatureRoles.CLASS_TYPE, input);
        }
        if (input.take(JvmSignatureToken.TYPE_VARIABLE, session)) {
            String name = JvmSignatureIdentifiers.read(input, session);
            input.expect(JvmSignatureToken.SEMICOLON, session);
            return new JvmTypeVariableSignatureNode(
                    input.cursor().source(), start, input.offset(), name);
        }
        if (input.take(JvmSignatureToken.ARRAY, session)) {
            int dimensions = 1;
            while (input.take(JvmSignatureToken.ARRAY, session)) {
                dimensions++;
            }
            JvmTypeSyntaxNode element = session.parse(JvmSignatureRoles.TYPE, input);
            return new JvmArraySignatureNode(
                    input.cursor().source(), start, input.offset(), dimensions, element);
        }
        throw input.failure("Expected a JVM reference type signature");
    }
}
