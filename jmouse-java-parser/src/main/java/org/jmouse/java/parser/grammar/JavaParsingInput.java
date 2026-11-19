package org.jmouse.java.parser.grammar;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.el.parser.ParsingLimitException;
import org.jmouse.el.parser.ParsingLimitKind;
import org.jmouse.java.model.version.JavaFeatureId;
import org.jmouse.java.model.version.JavaSourceFeatures;
import org.jmouse.java.model.version.JavaSourceProfile;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.lexer.JavaKeywordCatalog;
import org.jmouse.java.parser.lexer.JavaKeywordDefinition;
import org.jmouse.java.parser.lexer.JavaLexer;
import org.jmouse.java.parser.lexer.JavaSourceText;
import org.jmouse.java.parser.lexer.JavaToken;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.node.name.JavaIdentifierNode;
import org.jmouse.java.parser.source.ParseProblem;
import org.jmouse.java.parser.source.ProblemKind;
import org.jmouse.java.parser.source.SourceSpan;

/** Per-invocation Java state; token storage, checkpoints and bounds belong to jME. */
public final class JavaParsingInput {
    private final TokenCursor cursor;
    private final JavaSourceText source;
    private final JavaSourceProfile profile;
    private final JavaRecoveryPolicy recovery;
    private final List<ParseProblem> problems;
    private final JavaKeywordCatalog keywords;
    private JavaParsingState state = JavaParsingState.READING;

    public JavaParsingInput(TokenCursor cursor, JavaSourceProfile profile) {
        this(cursor, profile, JavaRecoveryPolicy.STRICT);
    }

    public JavaParsingInput(TokenCursor cursor, JavaSourceProfile profile, JavaRecoveryPolicy recovery) {
        this(cursor, profile, recovery, JavaKeywordCatalog.standard());
    }

    public JavaParsingInput(TokenCursor cursor, JavaSourceProfile profile, JavaRecoveryPolicy recovery,
                            JavaKeywordCatalog keywords) {
        this.cursor = Objects.requireNonNull(cursor, "cursor");
        this.source = JavaLexer.sourceOf(cursor);
        this.profile = Objects.requireNonNull(profile, "profile");
        this.recovery = Objects.requireNonNull(recovery, "recovery");
        problems = new ArrayList<>(source.problems());
        this.keywords = Objects.requireNonNull(keywords, "keywords");
    }

    /** Token position and diagnostics form one transaction when an alternative is speculative. */
    public static final class Checkpoint {
        private final JavaParsingInput owner;
        private final TokenCursor.Savepoint cursor;
        private final int problems;

        private Checkpoint(JavaParsingInput owner, TokenCursor.Savepoint cursor, int problems) {
            this.owner = owner;
            this.cursor = cursor;
            this.problems = problems;
        }
    }

    public Checkpoint checkpoint() {
        return new Checkpoint(this, cursor.savepoint(), problems.size());
    }

    public void restore(Checkpoint checkpoint) {
        if (checkpoint.owner != this || checkpoint.problems > problems.size()) {
            throw new IllegalArgumentException("This checkpoint does not belong to the current input state");
        }
        cursor.restore(checkpoint.cursor);
        problems.subList(checkpoint.problems, problems.size()).clear();
    }

    public List<ParseProblem> problems() {
        return List.copyOf(problems);
    }

    public JavaParsingState state() {
        return state;
    }

    public void reportLimit(ParsingLimitException exception, SourceSpan span, ParseProblem.Phase phase) {
        if (recovery == JavaRecoveryPolicy.STRICT) {
            throw exception;
        }
        ProblemKind kind = exception.kind() == ParsingLimitKind.WORK
                ? ProblemKind.WORK_BUDGET_EXCEEDED : ProblemKind.NESTING_TOO_DEEP;
        problems.add(new ParseProblem(kind, exception.getMessage(), span, phase));
        if (exception.kind() == ParsingLimitKind.WORK) {
            state = JavaParsingState.WORK_LIMIT_REACHED;
        }
    }

    public void report(JavaSyntaxException exception, ParseProblem.Phase phase) {
        if (recovery == JavaRecoveryPolicy.STRICT) {
            throw exception;
        }
        problems.add(new ParseProblem(exception.kind(), exception.getMessage(), exception.span(), phase));
    }

    public void unclosed(SourceSpan opening, ParseProblem.Phase phase) {
        report(new JavaSyntaxException(ProblemKind.UNBALANCED_DELIMITER, "The opened region is never closed", opening), phase);
        report(new JavaSyntaxException(ProblemKind.UNEXPECTED_END_OF_FILE, "Unexpected end of source", point()), phase);
    }

    public TokenCursor cursor() {
        return cursor;
    }

    public JavaSourceText source() {
        return source;
    }

    public JavaSourceProfile profile() {
        return profile;
    }

    public String name() {
        return source.getName();
    }

    public Token token() {
        return cursor.peek();
    }

    public boolean atEnd() {
        return JavaToken.END_OF_FILE.matches(token());
    }

    public int offset() {
        return token().offset();
    }

    public SourceSpan point() {
        return source.span(token());
    }

    public SourceSpan spanFrom(int start) {
        int end = cursor.position() == 0 ? start : source.endOffset(cursor.lookAt(-1));
        return source.lines().span(start, Math.max(start, end));
    }

    public <N extends JavaSyntaxNode> N finish(N node, int start) {
        node.setSpan(spanFrom(start));
        return node;
    }

    public Token next(ParseSession session) {
        session.step();
        if (atEnd()) {
            throw error("Unexpected end of source");
        }
        return cursor.next();
    }

    /** Fixed punctuation may span tokens; only adjacent source tokens form one operator. */
    public boolean matches(String spelling) {
        String first = token().value();
        if (first.equals(spelling)) {
            return true;
        }
        if (first.isEmpty() || !spelling.startsWith(first)) {
            return false;
        }
        int length = first.length();
        Token previous = token();
        for (int lookahead = 1; length < spelling.length(); lookahead++) {
            Token current = cursor.lookAt(lookahead);
            if (JavaToken.END_OF_FILE.matches(current) || source.endOffset(previous) != current.offset()
                    || !spelling.startsWith(current.value(), length)) {
                return false;
            }
            length += current.value().length();
            previous = current;
        }
        return length == spelling.length();
    }

    public boolean take(String spelling, ParseSession session) {
        if (!matches(spelling)) {
            return false;
        }
        int consumed = 0;
        do {
            consumed += next(session).value().length();
        } while (consumed < spelling.length());
        return true;
    }

    public boolean matches(JavaKeywordDefinition keyword) {
        return matches(keyword.spelling());
    }

    public boolean take(JavaKeywordDefinition keyword, ParseSession session) {
        return take(keyword.spelling(), session);
    }

    public void expect(JavaKeywordDefinition keyword, ParseSession session) {
        expect(keyword.spelling(), session);
    }

    public boolean matches(JavaToken token) {
        return token.matches(this.token());
    }

    public boolean take(JavaToken token, ParseSession session) {
        if (!matches(token)) {
            return false;
        }
        next(session);
        return true;
    }

    public void expect(JavaToken token, ParseSession session) {
        if (!take(token, session)) {
            throw error("Expected " + token + ", found " + token().type());
        }
    }

    public boolean matches(Token.Type type) {
        return type.equals(token().type());
    }

    public boolean take(Token.Type type, ParseSession session) {
        if (!matches(type)) {
            return false;
        }
        next(session);
        return true;
    }

    public void expect(Token.Type type, ParseSession session) {
        if (!take(type, session)) {
            throw error("Expected " + type + ", found " + token().type());
        }
    }

    public void expect(String spelling, ParseSession session) {
        if (!take(spelling, session)) {
            throw error("Expected '" + spelling + "', found '" + token().value() + "'");
        }
    }

    public JavaIdentifierNode identifier(ParseSession session) {
        if (!JavaToken.IDENTIFIER.matches(token())) {
            throw error("Expected an identifier");
        }
        int start = offset();
        String identifier = next(session).value();
        if (identifier.equals(JavaKeyword.UNDERSCORE.spelling()) && profile.permits(JavaSourceFeatures.UNDERSCORE_KEYWORD)) {
            throw errorAt("A single underscore is not an identifier in this source profile", spanFrom(start));
        }
        JavaIdentifierNode node = new JavaIdentifierNode(name(), spanFrom(start));
        node.setName(identifier);
        return node;
    }

    public JavaIdentifierNode typeIdentifier(ParseSession session) {
        JavaIdentifierNode identifier = identifier(session);
        requireTypeIdentifier(identifier);
        return identifier;
    }

    public void requireTypeIdentifier(JavaIdentifierNode identifier) {
        Optional<JavaFeatureId> restriction = keywords.find(identifier.name())
                .flatMap(JavaKeywordDefinition::typeNameRestriction);
        if (restriction.isPresent() && profile.permits(restriction.get())) {
            throw errorAt("Restricted type identifier: " + identifier.name(), identifier.span());
        }
    }

    public void requireFeature(JavaFeatureId identity) {
        if (!profile.permits(identity)) {
            throw error("Feature " + identity.name() + " is unavailable in source release " + profile.release().number());
        }
    }

    public JavaSyntaxException error(String message) {
        return new JavaSyntaxException(atEnd() ? ProblemKind.UNEXPECTED_END_OF_FILE : ProblemKind.UNEXPECTED_TOKEN,
                message, point());
    }

    public JavaSyntaxException errorAt(String message, SourceSpan span) {
        return new JavaSyntaxException(message, span);
    }

    public void requireEnd() {
        if (state == JavaParsingState.WORK_LIMIT_REACHED) {
            return;
        }
        if (!atEnd()) {
            throw error("Unexpected trailing token '" + token().value() + "'");
        }
        if (recovery == JavaRecoveryPolicy.STRICT && !problems.isEmpty()) {
            ParseProblem problem = problems.getFirst();
            throw new JavaSyntaxException(problem.kind(), problem.message(), problem.span());
        }
    }
}
