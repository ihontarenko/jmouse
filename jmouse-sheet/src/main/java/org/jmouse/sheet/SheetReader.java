package org.jmouse.sheet;

import org.jmouse.sheet.lexer.SheetRecognizer;
import org.jmouse.sheet.node.SheetDocumentNode;
import org.jmouse.sheet.node.EntryNode;
import org.jmouse.sheet.node.FieldNode;
import org.jmouse.sheet.parser.SheetDocumentParser;
import org.jmouse.el.StringSource;
import org.jmouse.el.extension.ExtensionContainer;
import org.jmouse.el.extension.StandardExtensionContainer;
import org.jmouse.el.lexer.DefaultLexer;
import org.jmouse.el.lexer.DefaultTokenizer;
import org.jmouse.el.lexer.ExpressionSplitter;
import org.jmouse.el.lexer.Lexer;
import org.jmouse.el.lexer.SyntaxErrorException;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.BasicNode;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.Trivia;
import org.jmouse.el.node.expression.SpanNode;
import org.jmouse.el.parser.DefaultParserContext;
import org.jmouse.el.parser.ParserContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Reads a sheet document on the expression engine: its splitter, sheet's recognizer, and the parsers of
 * {@link SheetExtension}; then the tree they build becomes a {@link Sheet}.
 *
 * <pre>
 * sheet "Title" as slug {
 *     name value [source word named …]
 *     id ["title"] [source word named …] {
 *         …
 *     }
 * }
 * </pre>
 *
 * <p>A value is a text in quotes; a number with, at most, its unit ({@code 2.2 µs}, {@code -40 °C}); a bare word; a
 * link to another document's entry or field ({@code 74hc595.sht#supply-voltage/maximum}); a range
 * ({@code 2 V .. 6 V}); a name given a value ({@code 14 = data}); or several of those separated by commas.
 * {@code #} standing apart begins a comment, and a comment is kept with the line it is written above or beside.</p>
 */
public final class SheetReader {

    private static final Lexer LEXER = new DefaultLexer(new DefaultTokenizer(new ExpressionSplitter(), new SheetRecognizer()));

    private static final ParserContext CONTEXT = context();

    private final Set<String> ids = new HashSet<>();

    private SheetReader() {
    }

    /**
     * @param document its name, for a refusal to say which document it is about
     * @throws SheetSyntaxException for a document that does not read, saying where
     */
    public static Sheet read(String document, String text) {
        try {
            TokenCursor cursor = LEXER.tokenize(new StringSource(document, text));
            Node container = BasicNode.forToken(cursor.current());
            CONTEXT.getParser(SheetDocumentParser.class).parse(cursor, container, CONTEXT);
            return new SheetReader().sheet((SheetDocumentNode) container.getFirst());
        } catch (SheetSyntaxException refusal) {
            throw refusal.at(document);
        } catch (SyntaxErrorException refusal) {
            throw new SheetSyntaxException(document, 0, 0, refusal.getMessage());
        }
    }

    private static ParserContext context() {
        ExtensionContainer extensions = new StandardExtensionContainer();
        extensions.importExtension(new SheetExtension());
        return new DefaultParserContext(extensions);
    }

    private Sheet sheet(SheetDocumentNode document) {
        Body body = body(document.getExpressions());
        return new Sheet(document.getSlug(), document.getTitle(), body.fields(), body.entries(), comments(document));
    }

    private record Body(List<SheetField> fields, List<SheetEntry> entries) {
    }

    private Body body(List<Expression> statements) {
        List<SheetField> fields = new ArrayList<>();
        List<SheetEntry> entries = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (Expression statement : statements) {
            switch (statement) {
                case FieldNode field -> {
                    if (!names.add(field.getName())) {
                        throw refusal(field.getSpan(), "'" + field.getName() + "' is written twice in one block");
                    }
                    fields.add(new SheetField(field.getName(), field.getValue(), field.getSource(), comments(field)));
                }
                case EntryNode entry -> entries.add(entry(entry));
                default -> throw refusal(null, "a block holds fields and entries, not " + statement);
            }
        }
        return new Body(fields, entries);
    }

    private SheetEntry entry(EntryNode entry) {
        if (!ids.add(entry.getId())) {
            throw refusal(entry.getSpan(), "two entries are called '" + entry.getId()
                    + "': an id names one entry in its document");
        }
        Body body = body(entry.getExpressions());
        return new SheetEntry(entry.getId(), entry.getTitle(), entry.getSource(), body.fields(), body.entries(),
                comments(entry));
    }

    /** The comments the engine kept with a line; blank lines are the writer's to place. */
    private static SheetComments comments(Node node) {
        List<String> above = node.getLeadingTrivia().stream().filter(trivia -> !trivia.isBlank()).map(Trivia::text).toList();
        Optional<String> beside = Optional.ofNullable(node.getTrailingTrivia()).map(Trivia::text);
        return above.isEmpty() && beside.isEmpty() ? SheetComments.NONE : new SheetComments(above, beside);
    }

    private static SheetSyntaxException refusal(SpanNode span, String detail) {
        return new SheetSyntaxException(null, span == null ? 0 : span.lineNumber(), span == null ? 0 : span.position(), detail);
    }
}
