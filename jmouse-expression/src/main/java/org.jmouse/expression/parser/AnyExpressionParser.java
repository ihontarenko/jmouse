package org.jmouse.expression.parser;

import org.jmouse.common.ast.lexer.Lexer;
import org.jmouse.common.ast.node.Node;
import org.jmouse.common.ast.parser.Parser;
import org.jmouse.common.ast.parser.ParserContext;
import org.jmouse.expression.ExtendedToken;

import static org.jmouse.common.ast.token.DefaultToken.T_OPEN_CURLY_BRACE;
import static org.jmouse.expression.Checkers.*;

public class AnyExpressionParser implements Parser {

    @Override
    public void parse(Lexer lexer, Node parent, ParserContext context) {
        Node valueNode;

        // resolve next expression after equal
        if (lexer.isNext(T_OPEN_CURLY_BRACE)) {
            // resolve array expression '{"string", "literal", etc.}'
            valueNode = context.getParser(ArrayParser.class).parse(lexer, context);
        } else if (lexer.isNext(ExtendedToken.T_ANNOTATION)) {
            // resolve nested annotation 'nextAnnotation=@AnnotationName(...)'
            shift(lexer, ExtendedToken.T_ANNOTATION);
            valueNode = context.getParser(AnnotationParser.class).parse(lexer, context);
        } else if (PARAMETERS.test(lexer)) {
            // parameters (key1=expression, ...)
            valueNode = context.getParser(ParametersParser.class).parse(lexer, context);
        } else if (lexer.isNext(ExtendedToken.T_CLASS_NAME)) {
            // resolve java-class name 'className=com.example.validator.NotNullValidator'
            shift(lexer, ExtendedToken.T_CLASS_NAME);
            valueNode = context.getParser(ClassNameParser.class).parse(lexer, context);
        } else if (lexer.isNext(ExtendedToken.T_PATH_VARIABLE)) {
            // resolve path-variable name '/user/:id'
            valueNode = context.getParser(PathVariableParser.class).parse(lexer, context);
        } else if (VARIABLE_DEFINITION.test(lexer)) {
            // resolve string definition name '#handler:command_name'
            valueNode = context.getParser(StringDefinitionParser.class).parse(lexer, context);
        } else if (lexer.check(VARIABLE)) {
            // either #variable, #staticMethod(#var, 123) or #instance.method(#sum(1, 2), 0)

            // by default #localVariable
            Class<? extends Parser> variableParser = VariableParser.class;

            // if #object.methodCall() or // if #staticMethodCall()
            if (lexer.check(OBJECT_METHOD)) {
                variableParser = ObjectMethodCallParser.class;
            } else if (lexer.check(STATIC_METHOD)) {
                variableParser = FunctionCallParser.class;
            }

            valueNode = context.getParser(variableParser).parse(lexer, context);
        } else {
            // after all only literals available to parse 'value=100' || 'value="string"'
            valueNode = context.getParser(LiteralParser.class).parse(lexer, context);
        }

        parent.add(valueNode);
    }

}
