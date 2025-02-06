package svit.expression.parser;

import svit.expression.ast.AnnotationNode;
import org.jmouse.common.ast.lexer.Lexer;
import org.jmouse.common.ast.node.Node;
import org.jmouse.common.ast.parser.Parser;
import org.jmouse.common.ast.parser.ParserContext;

import static org.jmouse.common.ast.token.DefaultToken.*;
import static svit.expression.ExtendedToken.T_ANNOTATION;

public class AnnotationParser implements Parser {

    @Override
    public void parse(Lexer lexer, Node parent, ParserContext context) {
        ensureNext(lexer, T_ANNOTATION);

        AnnotationNode annotation = new AnnotationNode(lexer.current());

        shift(lexer, T_OPEN_BRACE);

        if (lexer.isNext(T_IDENTIFIER)) {
            context.getParser(ParametersParser.class).parse(lexer, annotation, context);
        }

        shift(lexer, T_CLOSE_BRACE);

        parent.add(annotation);
    }

}
