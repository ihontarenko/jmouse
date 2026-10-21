package org.jmouse.parallax.script.reader;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.BasicNode;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.ExperienceDocument;
import org.jmouse.parallax.script.Kind;
import org.jmouse.parallax.script.Meta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * One document: {@code kind "Title" [as slug] [type role] { statements }}. Its statements are read by the
 * {@link JmxStatementParser}s {@link JmxExtension} lists, chosen by their word and the document's kind; then the
 * document is held to {@link DocumentRules}.
 */
final class DocumentParser extends AbstractParser {

    private static final String SLUG = "[a-z0-9]+(-[a-z0-9]+)*";

    private final Reading reading;
    private final DeclarationReader declarations;
    private final SymbolReader symbols;
    private final DocumentRules rules;
    private final List<JmxStatementParser> statements;

    DocumentParser(Reading reading, DeclarationReader declarations, SymbolReader symbols, DocumentRules rules,
                   List<JmxStatementParser> statements) {
        this.reading = reading;
        this.declarations = declarations;
        this.symbols = symbols;
        this.rules = rules;
        this.statements = List.copyOf(statements);
    }

    /** Whether the cursor sits on a document's head: a kind's word followed by a quoted title. */
    static boolean atDocument(Reading reading) {
        return !reading.atEnd() && reading.cursor.current().type() != BasicToken.T_STRING
                && Kind.ofKeyword(reading.cursor.current().value()).isPresent()
                && reading.cursor.isNext(BasicToken.T_STRING);
    }

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        if (!atDocument(reading)) {
            throw reading.refusal("a document opens with its kind and a title in quotes: 'surface \"Radial wave\" { … }'");
        }
        Token head = cursor.next();
        Kind kind = Kind.ofKeyword(head.value()).orElseThrow();
        declarations.forgetTypes();
        String title = reading.literal(cursor.next());
        String slug = ExperienceDocument.slugOf(title);
        Optional<String> type = Optional.empty();
        while (!reading.atLineEnd()) {
            Token word = cursor.current();
            boolean named = reading.lineView().remaining() > 1;
            if (word.value().equals("as") && named) {
                cursor.next();
                slug = reading.joinedWord();
            } else if (word.value().equals("type") && named) {
                cursor.next();
                type = Optional.of(reading.joinedWord());
            } else {
                throw reading.refusal(word, "after the title come 'as slug' and 'type role', then '{'");
            }
        }
        if (!slug.matches(SLUG)) {
            throw reading.refusal(head, "'" + slug + "' is not a slug: lowercase words and digits joined by hyphens");
        }
        Body body = body(kind, title, head, context);
        ExperienceDocument document = new ExperienceDocument(kind, title, slug, type, body.meta, body.base,
                body.units, body.uses, body.blocks, reading.lineOf(head));
        rules.validate(document, head);
        parent.add(new DocumentNode(document));
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return false;
    }

    /** What a document's braces hold, gathered while reading them. */
    private static final class Body {
        Meta             meta   = Meta.EMPTY;
        Optional<String> base   = Optional.empty();
        List<String>     units  = List.of();
        List<String>     uses   = new ArrayList<>();
        List<Block>      blocks = new ArrayList<>();
    }

    private Body body(Kind kind, String title, Token head, ParserContext context) {
        Body body = new Body();
        Set<String> written = new HashSet<>();
        SymbolReader.Drawing drawing = kind == Kind.SYMBOL ? symbols.drawing() : null;
        reading.body("'{' to open " + kind.keyword() + " \"" + title + "\"",
                () -> reading.refusal(head, kind.keyword() + " \"…\" is opened with '{' and never closed"),
                () -> {
                    if (atDocument(reading)) {
                        throw reading.refusal("a " + kind.keyword() + " does not hold other documents: one object is "
                                + "one file, and a collection lists them by slug");
                    }
                    // A symbol's drawing is written as lines of its own, not as a block: `rect …`, `line …`, `pin …`.
                    if (drawing != null && symbols.atDrawingLine()) {
                        drawing.read();
                    } else {
                        gather(body, statement(kind, written, context));
                    }
                });
        if (drawing != null) {
            body.blocks.add(drawing.symbol());
        }
        return body;
    }
    /** The statement the cursor stands on, read by the parser of its word. */
    private DocumentStatement statement(Kind kind, Set<String> written, ParserContext context) {
        Token keyword = reading.cursor.current();
        String name = keyword.value();
        if (reading.isString(keyword)) {
            throw reading.refusal(keyword, "a text in quotes cannot start a line here");
        }
        JmxStatementParser parser = statements.stream().filter(candidate -> candidate.reads(name, kind)).findFirst()
                .orElse(null);
        if (parser == null || parser.isBlock()) {
            if (!kind.allows(name)) {
                throw reading.refusal(keyword, "'" + name + "' is not a block of a " + kind.keyword()
                        + "; it holds " + String.join(", ", kind.blocks().stream().sorted().toList()));
            }
            if (!Kind.REPEATABLE_BLOCKS.contains(name) && !written.add(name)) {
                throw reading.refusal(keyword, "'" + name + "' is written twice");
            }
            if (parser == null) {
                throw reading.refusal(keyword, "'" + name + "' is not a block jMX knows");
            }
        }
        Node holder = BasicNode.forToken(keyword);
        parser.parse(reading.cursor, holder, context);
        return ((DocumentStatementNode) holder.getFirst()).statement();
    }

    private static void gather(Body body, DocumentStatement statement) {
        switch (statement) {
            case DocumentStatement.Base base -> body.base = Optional.of(base.slug());
            case DocumentStatement.Units units -> body.units = units.names();
            case DocumentStatement.Uses uses -> body.uses.addAll(uses.slugs());
            case DocumentStatement.MetaBlock meta -> body.meta = meta.meta();
            case DocumentStatement.BlockStatement block -> body.blocks.add(block.block());
        }
    }
}
