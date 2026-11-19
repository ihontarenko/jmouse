package org.jmouse.java.bytecode.signature;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.bytecode.vocabulary.JvmSignatureToken;

public final class JvmThrowsSignatureParser
        implements ParseRule<JvmTextInput, JvmThrowsSignatureNode> {
    @Override
    public JvmThrowsSignatureNode parse(JvmTextInput input, ParseSession session) {
        int start = input.offset();
        input.expect(JvmSignatureToken.THROWS, session);
        if (!input.matches(JvmSignatureToken.CLASS)
                && !input.matches(JvmSignatureToken.TYPE_VARIABLE)) {
            throw input.failure("A throws signature requires a class or type variable");
        }
        JvmReferenceTypeSignatureNode type = session.parse(JvmSignatureRoles.REFERENCE, input);
        return new JvmThrowsSignatureNode(input.cursor().source(), start, input.offset(), type);
    }
}
