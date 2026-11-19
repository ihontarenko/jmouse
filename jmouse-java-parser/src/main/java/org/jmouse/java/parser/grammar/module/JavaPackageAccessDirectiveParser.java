package org.jmouse.java.parser.grammar.module;

import java.util.function.BiFunction;
import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParseRoles;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.module.JavaPackageAccessDirectiveNode;
import org.jmouse.java.parser.source.SourceSpan;

/** One production shared by independently replaceable exports and opens rules. */
public final class JavaPackageAccessDirectiveParser<N extends JavaPackageAccessDirectiveNode>
        implements ParseRule<JavaParsingInput, N> {
    private final JavaKeyword keyword;
    private final BiFunction<String, SourceSpan, N> factory;

    public JavaPackageAccessDirectiveParser(JavaKeyword keyword, BiFunction<String, SourceSpan, N> factory) {
        this.keyword = keyword;
        this.factory = factory;
    }

    @Override
    public N parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        input.expect(keyword, session);
        N node = factory.apply(input.name(), input.point());
        node.setName(session.parse(JavaParseRoles.QUALIFIED_NAME, input));
        if (input.take(JavaKeyword.TO, session)) {
            do {
                node.addTarget(session.parse(JavaParseRoles.QUALIFIED_NAME, input));
            } while (input.take(",", session));
        }
        input.expect(";", session);
        return input.finish(node, start);
    }
}
