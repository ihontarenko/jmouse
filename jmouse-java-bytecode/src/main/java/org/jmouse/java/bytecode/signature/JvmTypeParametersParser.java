package org.jmouse.java.bytecode.signature;

import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.bytecode.vocabulary.JvmSignatureToken;

import java.util.ArrayList;
import java.util.List;

/** Repeated formal parameters are shared by class and method productions. */
public final class JvmTypeParametersParser {
    private JvmTypeParametersParser() {}

    public static List<JvmTypeParameterNode> read(JvmTextInput input, ParseSession session) {
        List<JvmTypeParameterNode> parameters = new ArrayList<>();
        if (input.take(JvmSignatureToken.ARGUMENTS_OPEN, session)) {
            parameters.add(session.parse(JvmSignatureRoles.TYPE_PARAMETER, input));
            while (!input.matches(JvmSignatureToken.ARGUMENTS_CLOSE)) {
                parameters.add(session.parse(JvmSignatureRoles.TYPE_PARAMETER, input));
            }
            input.expect(JvmSignatureToken.ARGUMENTS_CLOSE, session);
        }
        return parameters;
    }
}
