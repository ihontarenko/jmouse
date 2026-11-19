package org.jmouse.java.parser.grammar.statement;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParseRoles;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.grammar.JavaSyntaxException;
import org.jmouse.java.parser.node.expression.JavaExpressionNode;
import org.jmouse.java.parser.node.expression.JavaFieldAccessExpressionNode;
import org.jmouse.java.parser.node.expression.JavaNameExpressionNode;
import org.jmouse.java.parser.node.resource.JavaResourceDeclarationNode;
import org.jmouse.java.parser.node.resource.JavaResourceNode;
import org.jmouse.java.parser.node.resource.JavaResourceReferenceNode;
import org.jmouse.java.parser.node.resource.JavaResourceVariableDeclarationNode;

/** Resource acquisition is syntax here; close order and exception suppression belong to lowering. */
public final class JavaResourceParser implements ParseRule<JavaParsingInput, JavaResourceNode> {
    @Override
    public JavaResourceNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        JavaParsingInput.Checkpoint checkpoint = input.checkpoint();
        try {
            JavaResourceVariableDeclarationNode variable =
                    new JavaResourceVariableDeclarationNode(input.name(), input.point());
            variable.setModifiers(session.parse(JavaParseRoles.VARIABLE_MODIFIERS, input));
            variable.setType(session.parse(JavaParseRoles.TYPE, input));
            variable.setName(input.identifier(session));
            input.expect("=", session);
            variable.setInitializer(session.parse(JavaParseRoles.EXPRESSION, input));
            input.finish(variable, start);
            JavaResourceDeclarationNode resource = new JavaResourceDeclarationNode(input.name(), variable.span());
            resource.setDeclaration(variable);
            return resource;
        } catch (JavaSyntaxException exception) {
            input.restore(checkpoint);
        }
        JavaExpressionNode expression = session.parse(JavaParseRoles.EXPRESSION, input);
        if (!(expression instanceof JavaNameExpressionNode || expression instanceof JavaFieldAccessExpressionNode)) {
            throw input.errorAt("A resource reference must be a variable access", expression.span());
        }
        JavaResourceReferenceNode resource = new JavaResourceReferenceNode(input.name(), expression.span());
        resource.setReference(expression);
        return resource;
    }
}
