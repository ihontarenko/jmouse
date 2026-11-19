package org.jmouse.java.parser.java27;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParseRoles;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.lexer.JavaToken;

public final class Java27BundleImportParser
        implements ParseRule<JavaParsingInput, Java27BundleImportNode> {
    @Override
    public Java27BundleImportNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        input.requireFeature(Java27Names.FEATURE);
        Java27BundleImportNode node = new Java27BundleImportNode(input.name(), input.point());
        input.expect(JavaKeyword.IMPORT, session);
        input.expect(Java27Keyword.BUNDLE, session);
        node.setName(session.parse(JavaParseRoles.QUALIFIED_NAME, input));
        input.expect(JavaToken.SEMICOLON, session);
        return input.finish(node, start);
    }
}
