package svit.expression.parser;

import svit.expression.ast.ClassNameNode;
import org.jmouse.common.ast.lexer.Lexer;
import org.jmouse.common.ast.node.Node;
import org.jmouse.common.ast.parser.Parser;
import org.jmouse.common.ast.parser.ParserContext;
import svit.expression.ExtendedToken;

import static org.jmouse.common.ast.token.Token.Entry;

public class ClassNameParser implements Parser {

    @Override
    public void parse(Lexer lexer, Node parent, ParserContext context) {
        ensureCurrent(lexer, ExtendedToken.T_CLASS_NAME);

        Entry         entry = lexer.current();
        ClassNameNode node  = new ClassNameNode(entry);

        node.setClassName(entry.value());

        parent.add(node);
    }

}
