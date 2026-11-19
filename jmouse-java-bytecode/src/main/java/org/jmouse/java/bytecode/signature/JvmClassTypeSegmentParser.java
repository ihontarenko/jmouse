package org.jmouse.java.bytecode.signature;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.bytecode.vocabulary.JvmSignatureToken;

import java.util.ArrayList;
import java.util.List;

public final class JvmClassTypeSegmentParser
        implements ParseRule<JvmTextInput, JvmClassTypeSegmentNode> {
    @Override
    public JvmClassTypeSegmentNode parse(JvmTextInput input, ParseSession session) {
        int start = input.offset();
        String name = JvmSignatureIdentifiers.read(input, session);
        List<JvmTypeArgumentNode> arguments = new ArrayList<>();
        if (input.take(JvmSignatureToken.ARGUMENTS_OPEN, session)) {
            arguments.add(session.parse(JvmSignatureRoles.ARGUMENT, input));
            while (!input.matches(JvmSignatureToken.ARGUMENTS_CLOSE)) {
                arguments.add(session.parse(JvmSignatureRoles.ARGUMENT, input));
            }
            input.expect(JvmSignatureToken.ARGUMENTS_CLOSE, session);
        }
        return new JvmClassTypeSegmentNode(
                input.cursor().source(), start, input.offset(), name, arguments);
    }
}
