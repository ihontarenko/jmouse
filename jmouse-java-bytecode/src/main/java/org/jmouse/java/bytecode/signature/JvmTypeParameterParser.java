package org.jmouse.java.bytecode.signature;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.bytecode.vocabulary.JvmSignatureToken;

import java.util.ArrayList;
import java.util.List;

public final class JvmTypeParameterParser implements ParseRule<JvmTextInput, JvmTypeParameterNode> {
    @Override
    public JvmTypeParameterNode parse(JvmTextInput input, ParseSession session) {
        int start = input.offset();
        String name = JvmSignatureIdentifiers.read(input, session);
        input.expect(JvmSignatureToken.BOUND, session);
        JvmReferenceTypeSignatureNode classBound =
                JvmSignatureTypeParser.startsReference(input)
                                && !JvmSignatureIdentifiers.startsTypeParameter(input, session)
                        ? session.parse(JvmSignatureRoles.REFERENCE, input)
                        : null;
        List<JvmReferenceTypeSignatureNode> interfaceBounds = new ArrayList<>();
        while (input.take(JvmSignatureToken.BOUND, session)) {
            interfaceBounds.add(session.parse(JvmSignatureRoles.REFERENCE, input));
        }
        return new JvmTypeParameterNode(
                input.cursor().source(), start, input.offset(), name, classBound, interfaceBounds);
    }
}
