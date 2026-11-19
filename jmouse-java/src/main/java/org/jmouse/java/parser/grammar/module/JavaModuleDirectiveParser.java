package org.jmouse.java.parser.grammar.module;

import java.util.Map;
import org.jmouse.el.parser.ParseRole;
import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.module.JavaModuleDirectiveNode;

public final class JavaModuleDirectiveParser implements ParseRule<JavaParsingInput, JavaModuleDirectiveNode> {
    private final Map<JavaKeyword, ParseRole<JavaParsingInput, ? extends JavaModuleDirectiveNode>> routes;

    public JavaModuleDirectiveParser(Map<JavaKeyword, ParseRole<JavaParsingInput, ? extends JavaModuleDirectiveNode>> routes) {
        this.routes = Map.copyOf(routes);
    }

    @Override
    public JavaModuleDirectiveNode parse(JavaParsingInput input, ParseSession session) {
        JavaKeyword keyword = JavaKeyword.fromSpelling(input.token().value()).orElse(null);
        ParseRole<JavaParsingInput, ? extends JavaModuleDirectiveNode> role = keyword == null ? null : routes.get(keyword);
        if (role == null) {
            throw input.error("Expected a module directive");
        }
        return session.parse(role, input);
    }
}
