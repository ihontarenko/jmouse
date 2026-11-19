package org.jmouse.java.parser.grammar;

import java.util.function.Consumer;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.node.parameter.JavaParameterNode;
import org.jmouse.java.parser.node.parameter.JavaReceiverParameterNode;
import org.jmouse.java.parser.node.parameter.JavaVariableArityParameterNode;

/** Shares the formal-list production between methods and constructors. */
public final class JavaParameterListParser {
    private JavaParameterListParser() {
    }

    public static void parse(JavaParsingInput input, ParseSession session, Consumer<JavaParameterNode> parameter,
                             Consumer<JavaReceiverParameterNode> receiver) {
        input.expect("(", session);
        if (input.take(")", session)) {
            return;
        }
        JavaParsingInput.Checkpoint checkpoint = input.checkpoint();
        JavaReceiverParameterNode receiverParameter = null;
        try {
            receiverParameter = session.parse(JavaParseRoles.RECEIVER_PARAMETER, input);
        } catch (JavaSyntaxException exception) {
            input.restore(checkpoint);
        }
        if (receiverParameter != null) {
            receiver.accept(receiverParameter);
            if (input.take(")", session)) {
                return;
            }
            input.expect(",", session);
        }
        do {
            JavaParameterNode written = session.parse(JavaParseRoles.PARAMETER, input);
            parameter.accept(written);
            if (written instanceof JavaVariableArityParameterNode && input.matches(",")) {
                throw input.error("A variable-arity parameter must be last");
            }
        } while (input.take(",", session));
        input.expect(")", session);
    }
}
