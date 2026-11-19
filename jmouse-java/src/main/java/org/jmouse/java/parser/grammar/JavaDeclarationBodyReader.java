package org.jmouse.java.parser.grammar;

import org.jmouse.el.lexer.BoundedTokenCursor;
import org.jmouse.el.lexer.TokenNesting;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.lexer.JavaToken;
import org.jmouse.java.parser.node.body.JavaOmittedRegionNode;
import org.jmouse.java.parser.source.JavaOmittedRegion;
import org.jmouse.java.parser.source.JavaRegionKind;
import org.jmouse.java.parser.source.ParseProblem;
import org.jmouse.java.parser.source.SourceSpan;

import java.util.Set;

/**
 * Reads only a body's extent through jME's bounded cursor, recording that its syntax remains
 * unread.
 */
public final class JavaDeclarationBodyReader {
    private static final TokenNesting BRACES =
            new TokenNesting(Set.of(JavaToken.LEFT_BRACE), Set.of(JavaToken.RIGHT_BRACE));

    private JavaDeclarationBodyReader() {}

    public static JavaOmittedRegionNode read(
            JavaParsingInput input, ParseSession session, SourceSpan opening, JavaRegionKind kind) {
        BoundedTokenCursor body =
                new BoundedTokenCursor(
                        input.cursor(),
                        (token, next, depth) ->
                                JavaToken.END_OF_FILE.matches(token)
                                        || depth == 0 && JavaToken.RIGHT_BRACE.matches(token),
                        BRACES);
        while (body.hasNext()) {
            input.next(session);
        }
        SourceSpan extent = input.source().lines().span(opening.end().offset(), input.offset());
        input.omit(new JavaOmittedRegion(kind, extent));
        JavaOmittedRegionNode omitted = new JavaOmittedRegionNode(input.name(), extent, kind);
        JavaSyntaxRecovery.closeBrace(input, session, opening, ParseProblem.Phase.BODY);
        return omitted;
    }
}
