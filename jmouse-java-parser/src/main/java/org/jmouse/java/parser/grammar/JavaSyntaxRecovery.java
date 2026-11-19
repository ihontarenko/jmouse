package org.jmouse.java.parser.grammar;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.Optional;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.parser.ParseRole;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.el.parser.ParsingLimitException;
import org.jmouse.el.parser.ParsingLimitKind;
import org.jmouse.java.parser.lexer.JavaToken;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.source.ParseProblem;
import org.jmouse.java.parser.source.SourceSpan;

/** Recovery around committed productions. Speculative alternatives continue to use input checkpoints. */
public final class JavaSyntaxRecovery {
    private JavaSyntaxRecovery() {
    }

    public static <N extends JavaSyntaxNode> Optional<N> parse(ParseRole<JavaParsingInput, N> role,
                                                              JavaParsingInput input, ParseSession session,
                                                              JavaRecoveryBoundary boundary, ParseProblem.Phase phase) {
        return attempt(input, session, boundary, phase, () -> session.parse(role, input));
    }

    public static <N extends JavaSyntaxNode> Optional<N> attempt(JavaParsingInput input, ParseSession session,
                                                                JavaRecoveryBoundary boundary, ParseProblem.Phase phase,
                                                                Supplier<N> production) {
        JavaParsingInput.Checkpoint checkpoint = input.checkpoint();
        int position = input.cursor().position();
        try {
            N node = production.get();
            if (input.cursor().position() == position) {
                throw new IllegalStateException("A committed production must consume input");
            }
            return Optional.of(node);
        } catch (JavaSyntaxException exception) {
            input.restore(checkpoint);
            input.report(exception, phase);
            synchronize(input, session, boundary);
            return Optional.empty();
        } catch (ParsingLimitException exception) {
            if (exception.kind() == ParsingLimitKind.WORK) {
                throw exception;
            }
            SourceSpan failure = input.point();
            input.restore(checkpoint);
            input.reportLimit(exception, failure, phase);
            synchronize(input, session, boundary);
            return Optional.empty();
        }
    }

    public static void closeBrace(JavaParsingInput input, ParseSession session,
                                  SourceSpan opening, ParseProblem.Phase phase) {
        if (input.atEnd()) {
            input.unclosed(opening, phase);
        } else {
            input.expect(JavaToken.RIGHT_BRACE, session);
        }
    }

    public static <N extends JavaSyntaxNode> void readRegion(ParseRole<JavaParsingInput, N> role,
                                                             JavaParsingInput input, ParseSession session,
                                                             SourceSpan opening, JavaRecoveryBoundary boundary,
                                                             ParseProblem.Phase phase, Consumer<N> destination) {
        while (!input.matches(JavaToken.RIGHT_BRACE) && !input.atEnd()) {
            int position = input.cursor().position();
            parse(role, input, session, boundary, phase).ifPresent(destination);
            if (input.cursor().position() == position) {
                throw new IllegalStateException("A region must consume or own its recovery boundary");
            }
        }
        closeBrace(input, session, opening, phase);
    }

    /** Consumes damaged input, leaving an enclosing delimiter to its owning production. */
    public static void synchronize(JavaParsingInput input, ParseSession session, JavaRecoveryBoundary boundary) {
        Deque<Token.Type> closing = new ArrayDeque<>();
        int start = input.cursor().position();
        while (!input.atEnd()) {
            Token.Type type = input.token().type();
            if (input.cursor().position() != start && !closing.contains(JavaToken.RIGHT_BRACE)
                    && boundary.wordStops().contains(input.token().value())) {
                return;
            }
            boolean enclosingBrace = type == JavaToken.RIGHT_BRACE && !closing.contains(JavaToken.RIGHT_BRACE);
            boolean statementEnd = type == JavaToken.SEMICOLON && !closing.contains(JavaToken.RIGHT_BRACE);
            if ((closing.isEmpty() || enclosingBrace || statementEnd) && boundary.stops().contains(type)) {
                if (boundary.consumedStops().contains(type)) {
                    input.next(session);
                }
                return;
            }
            input.next(session);
            if (type == JavaToken.LEFT_BRACE) {
                closing.push(JavaToken.RIGHT_BRACE);
            } else if (type == JavaToken.LEFT_PARENTHESIS) {
                closing.push(JavaToken.RIGHT_PARENTHESIS);
            } else if (type == JavaToken.LEFT_BRACKET) {
                closing.push(JavaToken.RIGHT_BRACKET);
            } else if (type == JavaToken.RIGHT_BRACE && closing.contains(JavaToken.RIGHT_BRACE)) {
                while (closing.peek() != JavaToken.RIGHT_BRACE) {
                    closing.pop();
                }
                closing.pop();
                if (!closing.contains(JavaToken.RIGHT_BRACE) && boundary.blockExit() == JavaRecoveryBlockExit.STOP) {
                    return;
                }
            } else if (!closing.isEmpty() && closing.peek().equals(type)) {
                closing.pop();
            }
        }
    }
}
