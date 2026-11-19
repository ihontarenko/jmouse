package org.jmouse.java.bytecode.signature;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.bytecode.vocabulary.JvmSignatureToken;

public final class JvmSignatureTypeParser implements ParseRule<JvmTextInput, JvmTypeSyntaxNode> {
    @Override
    public JvmTypeSyntaxNode parse(JvmTextInput input, ParseSession session) {
        if (startsReference(input)) {
            return session.parse(JvmSignatureRoles.REFERENCE, input);
        }
        return session.parse(JvmDescriptorRoles.FIELD, input);
    }

    public static boolean startsReference(JvmTextInput input) {
        return input.matches(JvmSignatureToken.CLASS)
                || input.matches(JvmSignatureToken.TYPE_VARIABLE)
                || input.matches(JvmSignatureToken.ARRAY);
    }
}
