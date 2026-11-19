package org.jmouse.java.bytecode.signature;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;

import java.util.ArrayList;
import java.util.List;

public final class JvmClassSignatureParser
        implements ParseRule<JvmTextInput, JvmClassSignatureNode> {
    @Override
    public JvmClassSignatureNode parse(JvmTextInput input, ParseSession session) {
        int start = input.offset();
        List<JvmTypeParameterNode> parameters = JvmTypeParametersParser.read(input, session);
        JvmClassTypeSignatureNode superclass = session.parse(JvmSignatureRoles.CLASS_TYPE, input);
        List<JvmClassTypeSignatureNode> interfaces = new ArrayList<>();
        while (!input.atEnd()) {
            interfaces.add(session.parse(JvmSignatureRoles.CLASS_TYPE, input));
        }
        return new JvmClassSignatureNode(
                input.cursor().source(), start, input.offset(), parameters, superclass, interfaces);
    }
}
