package org.jmouse.cad.document;

import java.util.Optional;
import java.util.Map;
import java.util.LinkedHashMap;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.Expression;
import org.jmouse.el.grammar.ClauseLineNode;
import org.jmouse.el.grammar.ClauseBlockParser;
import org.jmouse.el.grammar.ClauseBlockNode;
import org.jmouse.el.grammar.QuotedText;

import org.jmouse.el.grammar.Lines;

import org.jmouse.el.grammar.GrammarRefusal;

import org.jmouse.core.text.Slugifier;
import org.jmouse.el.language.parser.AbstractBodyParser;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.support.SourceReading;
import org.jmouse.el.node.expression.SpanNode;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.ParserContext;

/**
 * One document: {@code kind "Title" [as slug] [type role] { statements }}. Its kind is a word the language's
 * {@link DocumentGrammar} knows; its body is read by {@link DocumentBodyParser}.
 */
public class DocumentParser extends AbstractBodyParser<DocumentNode, BasicToken> {

    private static final String    SLUG  = "[a-z0-9]+(-[a-z0-9]+)*";
    private static final Slugifier SLUGS = Slugifier.standard().fallingBackTo("untitled");

    private final DocumentGrammar grammar;

    /** The slug a title gives a document that writes none: {@code "NPN transistor"} is {@code npn-transistor}. */
    public static String slugOf(String title) {
        return SLUGS.slug(title);
    }

    public DocumentParser(DocumentGrammar grammar) {
        this.grammar = grammar;
    }

    @Override
    protected DocumentNode createNode(TokenCursor cursor, ParserContext context) {
        Token head = cursor.lookAt(-1);
        DocumentKind kind = grammar.kind(head.value()).orElseThrow();
        if (cursor.isCurrent(BasicToken.T_COLON)) {
            return termHead(cursor, kind, head);
        }
        String title = QuotedText.of(cursor.ensure(BasicToken.T_STRING));
        String slug = SLUGS.slug(title);
        boolean slugWritten = false;
        String type = null;
        while (!cursor.isCurrent(BasicToken.T_OPEN_CURLY)) {
            Token word = cursor.current();
            if (Lines.lineEnd().matches(cursor)) {
                throw GrammarRefusal.at(cursor, head, kind.word() + " \"" + title + "\" is opened with '{'");
            }
            cursor.next();
            if (word.value().equals("as")) {
                slug = SourceReading.touchingWord(cursor, Lines.WORD_SEPARATORS);
                slugWritten = true;
            } else if (word.value().equals("type")) {
                type = SourceReading.touchingWord(cursor, Lines.WORD_SEPARATORS);
            } else {
                throw GrammarRefusal.at(cursor, word, "after the title come 'as slug' and 'type role', then '{'");
            }
        }
        if (!slug.matches(SLUG)) {
            throw GrammarRefusal.at(cursor, head, "'" + slug + "' is not a slug: lowercase words and digits joined by hyphens");
        }
        return new DocumentNode(kind, title, slug, slugWritten, type, head.lineNumber());
    }

    /** {@code component:and-gate("AND gate", type: gate)}: the slug as the term's name, the title, the role. */
    private DocumentNode termHead(TokenCursor cursor, DocumentKind kind, Token head) {
        cursor.next();
        String slug = SourceReading.touchingWord(cursor, Lines.WORD_SEPARATORS);
        if (!cursor.isCurrent(BasicToken.T_OPEN_PAREN)) {
            throw GrammarRefusal.at(cursor, head, kind.word() + ":" + slug + " is followed by '(\"Title\")'");
        }
        cursor.next();
        String title = QuotedText.of(cursor.ensure(BasicToken.T_STRING));
        String type = null;
        while (cursor.isCurrent(BasicToken.T_COMMA)) {
            cursor.next();
            Token word = cursor.current();
            String argument = SourceReading.touchingWord(cursor, Lines.WORD_SEPARATORS);
            if (!argument.equals("type") || !cursor.isCurrent(BasicToken.T_COLON)) {
                throw GrammarRefusal.at(cursor, word, "after the title comes 'type: role', then ')'");
            }
            cursor.next();
            type = SourceReading.touchingWord(cursor, Lines.WORD_SEPARATORS);
        }
        if (!cursor.isCurrent(BasicToken.T_CLOSE_PAREN)) {
            throw GrammarRefusal.here(cursor, kind.word() + ":" + slug + "(\"Title\", type: role) closes with ')'");
        }
        cursor.next();
        if (!slug.matches(SLUG)) {
            throw GrammarRefusal.at(cursor, head, "'" + slug + "' is not a slug: lowercase words and digits joined by hyphens");
        }
        // Written only where the title would not give it: the same document, however it is spelled.
        DocumentNode document = new DocumentNode(kind, title, slug, !slug.equals(SLUGS.slug(title)), type, head.lineNumber());
        document.spelledAs(DocumentNode.Spelling.TERMS);
        return document;
    }

    /**
     * The body, its terms gathered back into the blocks a flat document leaves out — every {@code parameter:…} into one
     * {@code parameters} block, standing where the first of them stood —, so a document is one tree however it is written.
     */
    @Override
    protected void parseBody(TokenCursor cursor, DocumentNode node, ParserContext context) {
        ((DocumentBodyParser) context.getParser(DocumentBodyParser.class)).begin(node.kind());
        Node children = parseStatements(cursor, context);
        Map<String, ClauseBlockNode> gathered = new LinkedHashMap<>();
        for (Node child : children.getChildren()) {
            Expression statement = (Expression) child;
            Optional<ClauseBlockParser> block = statement instanceof ClauseLineNode line
                    ? grammar.gatheredBy(line.grammar(), node.kind()) : Optional.empty();
            if (block.isEmpty()) {
                node.addExpression(statement);
                continue;
            }
            ClauseBlockNode into = gathered.get(block.get().word());
            if (into == null) {
                // What stands above the first of its terms stood above the block the flat writing left out.
                into = new ClauseBlockNode(block.get().word(), null);
                into.addLeadingTrivia(statement.getLeadingTrivia());
                into.open();
                gathered.put(block.get().word(), into);
                node.addExpression(into);
                statement = ((ClauseLineNode) statement).withoutLeadingTrivia();
            }
            into.addExpression(statement);
        }
    }

    @Override
    protected Class<? extends Parser> statementsParser() {
        return DocumentBodyParser.class;
    }

    @Override
    protected BasicToken token() {
        return BasicToken.T_IDENTIFIER;
    }

    @Override
    protected boolean matches(TokenCursor cursor) {
        return DocumentMatcher.document(grammar).matches(cursor) || DocumentMatcher.documentTerm(grammar).matches(cursor);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <S extends SpanNode> S span(TokenCursor cursor) {
        if (!matches(cursor)) {
            throw GrammarRefusal.here(cursor, "a document opens with its kind and a title in quotes: 'symbol \"Resistor\" { … }'");
        }
        return (S) SpanNode.of(cursor.current().lineNumber(), cursor.current().offset());
    }
}
