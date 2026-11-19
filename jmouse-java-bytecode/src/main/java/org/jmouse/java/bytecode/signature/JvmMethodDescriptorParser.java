package org.jmouse.java.bytecode.signature;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.bytecode.vocabulary.JvmSignatureToken;

import java.util.ArrayList;
import java.util.List;

public final class JvmMethodDescriptorParser
        implements ParseRule<JvmTextInput, JvmMethodDescriptorNode> {
    @Override
    public JvmMethodDescriptorNode parse(JvmTextInput input, ParseSession session) {
        int start = input.offset();
        input.expect(JvmSignatureToken.PARAMETERS_OPEN, session);
        List<JvmDescriptorTypeNode> parameters = new ArrayList<>();
        while (!input.matches(JvmSignatureToken.PARAMETERS_CLOSE)) {
            parameters.add(session.parse(JvmDescriptorRoles.FIELD, input));
        }
        input.expect(JvmSignatureToken.PARAMETERS_CLOSE, session);
        JvmDescriptorTypeNode result = session.parse(JvmDescriptorRoles.RETURN, input);
        JvmMethodDescriptorNode method =
                new JvmMethodDescriptorNode(
                        input.cursor().source(), start, input.offset(), parameters, result);
        method.descriptor();
        return method;
    }
}
