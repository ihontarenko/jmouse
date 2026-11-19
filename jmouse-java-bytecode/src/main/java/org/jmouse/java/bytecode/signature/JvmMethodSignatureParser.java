package org.jmouse.java.bytecode.signature;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.bytecode.vocabulary.JvmSignatureToken;

import java.util.ArrayList;
import java.util.List;

public final class JvmMethodSignatureParser
        implements ParseRule<JvmTextInput, JvmMethodSignatureNode> {
    @Override
    public JvmMethodSignatureNode parse(JvmTextInput input, ParseSession session) {
        int start = input.offset();
        List<JvmTypeParameterNode> typeParameters = JvmTypeParametersParser.read(input, session);
        input.expect(JvmSignatureToken.PARAMETERS_OPEN, session);
        List<JvmTypeSyntaxNode> parameters = new ArrayList<>();
        while (!input.matches(JvmSignatureToken.PARAMETERS_CLOSE)) {
            parameters.add(session.parse(JvmSignatureRoles.TYPE, input));
        }
        input.expect(JvmSignatureToken.PARAMETERS_CLOSE, session);
        JvmTypeSyntaxNode result =
                input.matches(JvmSignatureToken.VOID)
                        ? session.parse(JvmDescriptorRoles.RETURN, input)
                        : session.parse(JvmSignatureRoles.TYPE, input);
        List<JvmThrowsSignatureNode> thrown = new ArrayList<>();
        while (input.matches(JvmSignatureToken.THROWS)) {
            thrown.add(session.parse(JvmSignatureRoles.THROWS, input));
        }
        return new JvmMethodSignatureNode(
                input.cursor().source(),
                start,
                input.offset(),
                typeParameters,
                parameters,
                result,
                thrown);
    }
}
