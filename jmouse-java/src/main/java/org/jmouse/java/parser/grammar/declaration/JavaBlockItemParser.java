package org.jmouse.java.parser.grammar.declaration;

import java.util.Set;
import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParseRoles;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.grammar.statement.JavaStatementRoles;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.lexer.JavaToken;
import org.jmouse.java.parser.node.JavaSyntaxNode;

public final class JavaBlockItemParser implements ParseRule<JavaParsingInput, JavaSyntaxNode> {
    private final Set<String> typeStarters;

    public JavaBlockItemParser(Set<String> typeStarters) {
        this.typeStarters = Set.copyOf(typeStarters);
    }

    @Override
    public JavaSyntaxNode parse(JavaParsingInput input, ParseSession session) {
        JavaParsingInput.Checkpoint checkpoint = input.checkpoint();
        session.parse(JavaParseRoles.MODIFIERS, input);
        boolean typeDeclaration = typeStarters.contains(input.token().value());
        if (input.matches(JavaKeyword.RECORD) && !JavaToken.IDENTIFIER.matches(input.cursor().lookAt(1))) {
            typeDeclaration = false;
        }
        input.restore(checkpoint);
        return typeDeclaration ? session.parse(JavaDeclarationRoles.TYPE_DECLARATION, input)
                : session.parse(JavaStatementRoles.STATEMENT, input);
    }
}
