package org.jmouse.java.bytecode.signature;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.bytecode.vocabulary.JvmSignatureToken;

public final class JvmTypeArgumentParser implements ParseRule<JvmTextInput, JvmTypeArgumentNode> {
    @Override
    public JvmTypeArgumentNode parse(JvmTextInput input, ParseSession session) {
        int start = input.offset();
        JvmTypeArgumentVariance variance = JvmTypeArgumentVariance.EXACT;
        JvmReferenceTypeSignatureNode bound = null;
        if (input.take(JvmSignatureToken.UNBOUNDED, session)) {
            variance = JvmTypeArgumentVariance.UNBOUNDED;
        } else {
            if (input.take(JvmSignatureToken.EXTENDS, session)) {
                variance = JvmTypeArgumentVariance.EXTENDS;
            } else if (input.take(JvmSignatureToken.SUPER, session)) {
                variance = JvmTypeArgumentVariance.SUPER;
            }
            bound = session.parse(JvmSignatureRoles.REFERENCE, input);
        }
        return new JvmTypeArgumentNode(
                input.cursor().source(), start, input.offset(), variance, bound);
    }
}
