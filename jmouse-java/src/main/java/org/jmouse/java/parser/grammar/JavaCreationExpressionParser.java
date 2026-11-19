package org.jmouse.java.parser.grammar;

import java.util.ArrayList;
import java.util.List;
import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.annotation.JavaAnnotationNode;
import org.jmouse.java.parser.node.expression.JavaArrayCreationExpressionNode;
import org.jmouse.java.parser.node.expression.JavaClassInstanceCreationExpressionNode;
import org.jmouse.java.parser.node.expression.JavaExpressionNode;
import org.jmouse.java.parser.node.type.JavaArrayDimensionNode;
import org.jmouse.java.parser.node.type.JavaInferredTypeNode;
import org.jmouse.java.parser.node.type.JavaNamedTypeNode;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;
import org.jmouse.java.parser.node.type.JavaVoidTypeNode;

/** Class creation and array creation share the 'new' prefix, with independent AST shapes. */
public final class JavaCreationExpressionParser implements ParseRule<JavaParsingInput, JavaExpressionNode> {
    @Override
    public JavaExpressionNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        input.expect(JavaKeyword.NEW, session);
        List<JavaTypeSyntaxNode> arguments = new ArrayList<>();
        JavaTypeParser.explicitArguments(input, session, arguments::add);
        JavaTypeSyntaxNode type = JavaTypeParser.element(input, session, JavaTypeArgumentsForm.DIAMOND_ALLOWED);
        if (input.matches("[") || input.matches("@")) {
            if (!arguments.isEmpty() || type instanceof JavaVoidTypeNode || type instanceof JavaInferredTypeNode) {
                throw input.error("Invalid array creation type");
            }
            return array(input, session, type, start);
        }
        if (!(type instanceof JavaNamedTypeNode)) {
            throw input.error("Class creation requires a class type");
        }
        JavaClassInstanceCreationExpressionNode node =
                new JavaClassInstanceCreationExpressionNode(input.name(), input.point());
        arguments.forEach(node::addTypeArgument);
        node.setType(type);
        JavaArgumentListParser.parse(input, session, node::addArgument);
        if (input.matches("{")) {
            node.setBody(session.parse(JavaParseRoles.ANONYMOUS_CLASS_BODY, input));
        }
        return input.finish(node, start);
    }

    private JavaArrayCreationExpressionNode array(JavaParsingInput input, ParseSession session,
                                                   JavaTypeSyntaxNode type, int start) {
        JavaArrayCreationExpressionNode node = new JavaArrayCreationExpressionNode(input.name(), input.point());
        node.setElementType(type);
        boolean sized = false;
        boolean unsized = false;
        while (input.matches("[") || input.matches("@")) {
            int dimensionStart = input.offset();
            List<JavaAnnotationNode> annotations = JavaTypeParser.annotations(input, session);
            input.expect("[", session);
            JavaArrayDimensionNode dimension = new JavaArrayDimensionNode(input.name(), input.point());
            annotations.forEach(dimension::addAnnotation);
            if (!input.matches("]")) {
                if (unsized) {
                    throw input.error("Sized dimensions cannot follow an unsized dimension");
                }
                dimension.setLength(session.parse(JavaParseRoles.EXPRESSION, input));
                sized = true;
            } else {
                unsized = true;
            }
            input.expect("]", session);
            node.addDimension(input.finish(dimension, dimensionStart));
        }
        if (input.matches("{")) {
            if (sized) {
                throw input.error("An array initializer cannot accompany dimension expressions");
            }
            node.setInitializer(session.parse(JavaParseRoles.ARRAY_INITIALIZER, input));
        } else if (!sized) {
            throw input.error("Array creation requires a dimension expression or an initializer");
        }
        return input.finish(node, start);
    }
}
