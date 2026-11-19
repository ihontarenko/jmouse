package org.jmouse.java.parser.grammar;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.model.version.JavaSourceFeatures;
import org.jmouse.java.parser.grammar.statement.JavaStatementRoles;
import org.jmouse.java.parser.lexer.JavaToken;
import org.jmouse.java.parser.node.expression.JavaBlockLambdaBodyNode;
import org.jmouse.java.parser.node.expression.JavaExpressionLambdaBodyNode;
import org.jmouse.java.parser.node.expression.JavaLambdaExpressionNode;
import org.jmouse.java.parser.node.parameter.JavaFormalParameterNode;
import org.jmouse.java.parser.node.parameter.JavaInferredLambdaParameterNode;
import org.jmouse.java.parser.node.parameter.JavaParameterNode;
import org.jmouse.java.parser.node.parameter.JavaVariableArityParameterNode;
import org.jmouse.java.parser.node.type.JavaInferredTypeNode;

/** Lambda parameter forms and bodies remain syntax; target typing is a semantic operation. */
public final class JavaLambdaExpressionParser implements ParseRule<JavaParsingInput, JavaLambdaExpressionNode> {
    private enum ParameterStyle { IMPLICIT, EXPLICIT, INFERRED_TYPE }

    @Override
    public JavaLambdaExpressionNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        JavaLambdaExpressionNode node = new JavaLambdaExpressionNode(input.name(), input.point());
        if (input.take("(", session)) {
            ParameterStyle previous = null;
            if (!input.take(")", session)) {
                do {
                    JavaParameterNode parameter;
                    ParameterStyle style;
                    if (JavaToken.IDENTIFIER.matches(input.token())
                            && (input.cursor().lookAt(1).value().equals(",") || input.cursor().lookAt(1).value().equals(")"))) {
                        parameter = inferredParameter(input, session);
                        style = ParameterStyle.IMPLICIT;
                    } else {
                        parameter = session.with(JavaGrammarContext.PARAMETER_FORM, JavaGrammarContext.ParameterForm.LAMBDA,
                                () -> session.parse(JavaParseRoles.PARAMETER, input));
                        style = parameter instanceof JavaFormalParameterNode formal
                                && formal.type() instanceof JavaInferredTypeNode
                                ? ParameterStyle.INFERRED_TYPE : ParameterStyle.EXPLICIT;
                        if (style == ParameterStyle.INFERRED_TYPE) {
                            input.requireFeature(JavaSourceFeatures.LAMBDA_VAR_PARAMETERS);
                        }
                    }
                    if (previous != null && previous != style) {
                        throw input.error("Lambda parameter declaration styles cannot be mixed");
                    }
                    previous = style;
                    node.addParameter(parameter);
                    if (parameter instanceof JavaVariableArityParameterNode && input.matches(",")) {
                        throw input.error("A variable-arity parameter must be last");
                    }
                } while (input.take(",", session));
                input.expect(")", session);
            }
        } else {
            node.addParameter(inferredParameter(input, session));
        }
        input.expect("->", session);
        int bodyStart = input.offset();
        if (input.matches("{")) {
            JavaBlockLambdaBodyNode body = new JavaBlockLambdaBodyNode(input.name(), input.point());
            body.setBlock(session.parse(JavaStatementRoles.BLOCK, input));
            node.setBody(input.finish(body, bodyStart));
        } else {
            JavaExpressionLambdaBodyNode body = new JavaExpressionLambdaBodyNode(input.name(), input.point());
            body.setExpression(session.parse(JavaParseRoles.EXPRESSION, input));
            node.setBody(input.finish(body, bodyStart));
        }
        return input.finish(node, start);
    }

    private JavaInferredLambdaParameterNode inferredParameter(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        JavaInferredLambdaParameterNode node = new JavaInferredLambdaParameterNode(input.name(), input.point());
        node.setName(input.identifier(session));
        return input.finish(node, start);
    }
}
