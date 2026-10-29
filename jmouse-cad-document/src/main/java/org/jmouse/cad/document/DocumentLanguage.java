package org.jmouse.cad.document;

import org.jmouse.el.grammar.GrammarExtension;
import org.jmouse.el.grammar.GrammarRefusal;
import org.jmouse.el.grammar.LineBoundaries;
import org.jmouse.el.grammar.LineGrammar;
import org.jmouse.el.grammar.ValueKind;
import org.jmouse.el.grammar.ValueReading;

import org.jmouse.el.extension.StandardExtensionContainer;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.DefaultLexer;
import org.jmouse.el.lexer.DefaultTokenizer;
import org.jmouse.el.lexer.ExpressionSplitter;
import org.jmouse.el.lexer.Lexer;
import org.jmouse.el.lexer.SyntaxErrorException;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.BasicNode;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.DefaultParserContext;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.writer.SourceLayout;
import org.jmouse.el.writer.TextSourceWriter;
import org.jmouse.script.el.ScriptExtension;
import org.jmouse.script.el.lexer.ScriptRecognizer;
import org.jmouse.script.el.node.ScriptNode;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * The jM document language, made of the modules given it: it reads a {@code .jm} text into its documents' trees and
 * writes a tree back as text. Reading is the expression engine's — its lexer, its parsers, its context — with
 * {@link DocumentExtension} and every module imported beside jMS, whose bodies a document may hold. Writing is each
 * node's own, through the engine's {@link TextSourceWriter}.
 */
public final class DocumentLanguage {

    /**
     * A document's numbers are a formula's — {@code 2d} is two times {@code d}, a part number such as {@code 24c02} is
     * no typed literal —, its behaviour is jMS, which counts with {@code ++}, and its texts may span lines.
     */
    private static final ExpressionSplitter.Dialect DIALECT = new ExpressionSplitter.Dialect(false, true).withLongTexts();

    private final DocumentGrammar       grammar;
    private final List<DocumentModule> modules;

    private DocumentLanguage(List<DocumentModule> modules) {
        this.modules = List.copyOf(modules);
        this.grammar = new DocumentGrammar(this.modules);
    }

    public static DocumentLanguage of(DocumentModule... modules) {
        return new DocumentLanguage(List.of(modules));
    }

    public DocumentGrammar grammar() {
        return grammar;
    }

    /** A file's text, its kind declared by its name: {@code resistor.symbol.jm} holds a symbol. */
    public List<DocumentNode> read(String text, String name) {
        return read(DocumentInput.file(name, text));
    }

    /**
     * Every document a text holds, in order, each held to the kind its source declares: a file by its suffix, a store's
     * record by its kind. A source that declares a kind this language does not have is refused, as is a document of
     * another kind than the one declared.
     */
    public List<DocumentNode> read(DocumentInput input) {
        String name = input.origin();
        Optional<DocumentKind> declared = input.declaredKind().map(word -> grammar.kind(word).orElseThrow(() ->
                new GrammarRefusal(name, 1, 0, "'" + word + "' is no kind of document this language reads: "
                        + grammar.kindWords())));
        DocumentSource source = DocumentSource.of(name, input.text());
        TokenCursor cursor;
        try {
            cursor = lexer().tokenize(source);
        } catch (SyntaxErrorException failure) {
            throw new GrammarRefusal(name, 0, 0, failure.getMessage());
        }
        ParserContext context = context();
        Node documents = context.getParser(DocumentsParser.class).parse(cursor, context);
        List<DocumentNode> read = documents.getChildren().stream().map(DocumentNode.class::cast).toList();
        if (read.isEmpty()) {
            throw new GrammarRefusal(name, 1, 0, "a text holds at least one document: 'kind \"Title\" { … }'");
        }
        declared.ifPresent(kind -> read.stream().filter(document -> !document.kind().equals(kind))
                .findFirst().ifPresent(document -> {
                    throw new GrammarRefusal(name, document.line(), 0, "its source declares a " + kind.word()
                            + " and it holds a " + document.kind().word() + "; a file is named <slug>"
                            + document.kind().fileSuffix() + " for what it holds, a record carries the kind it holds");
                }));
        return read;
    }

    /** The one document a file's text holds. */
    public DocumentNode readOne(String text, String name) {
        return readOne(DocumentInput.file(name, text));
    }

    /** The one document a text holds; refuses a text of several — one document is one file, one record. */
    public DocumentNode readOne(DocumentInput input) {
        String name = input.origin();
        List<DocumentNode> documents = read(input);
        if (documents.size() != 1) {
            throw new GrammarRefusal(name, documents.get(1).line(), 0,
                    "this text holds " + documents.size() + " documents where one was expected");
        }
        return documents.getFirst();
    }

    /** A document as text: each node writes itself. */
    public String write(DocumentNode document) {
        return TextSourceWriter.render(document, SourceLayout.WORDS);
    }

    /** A document as text, spelled as its own text was — as terms when it was read as terms, else with its blocks. */
    public String writeAsRead(DocumentNode document) {
        return document.spelling() == DocumentNode.Spelling.TERMS ? new TermDocumentWriter(true).write(document)
                : write(document);
    }

    /** The documents of one text, in order — each after what its author wrote above it, or after a blank line. */
    public String write(List<DocumentNode> documents) {
        StringBuilder text = new StringBuilder();
        for (DocumentNode document : documents) {
            if (!text.isEmpty()) {
                // A document's own blank line above it is written by the document; one without gets one here.
                text.append(document.getLeadingTrivia().isEmpty() ? "\n\n" : "\n");
            }
            text.append(write(document).stripTrailing());
        }
        return text.append('\n').toString();
    }

    /**
     * A jMS body — what a {@code behaviour} block held between its braces — read on its own, by the same lexer and in the
     * same context as the document it came from, into the script jMS runs: what an engine reads a component's behaviour
     * with, since the model keeps it as written.
     *
     * @param name   what the script is called: the block's word
     * @param origin what a refusal names: the component it belongs to
     */
    public ScriptNode script(String name, String body, String origin) {
        TokenCursor cursor;
        try {
            cursor = lexer().tokenize(DocumentSource.of(origin, body));
        } catch (SyntaxErrorException failure) {
            throw new GrammarRefusal(origin, 0, 0, failure.getMessage());
        }
        BasicNode holder = BasicNode.forToken(cursor.current());
        new ScriptBodyParser().parse(cursor, holder, context());
        ScriptNode script = new ScriptNode();
        script.setName(name);
        for (Node statement : holder.getChildren().getFirst().getChildren()) {
            script.addExpression((Expression) statement);
        }
        return script;
    }

    /**
     * One value of a line, as an editor typed it — in a line's spelling or a term's —, read the way the line reads it — by the
     * same lexer, in the same context, by the kind of value its slot or clause takes: what an edit builds a line's node from, so nobody writes
     * a line's text by joining strings.
     *
     * @param line    the line the value is of, for a refusal's words
     * @param allowed the only words the value may be; empty for any
     * @param written the value as typed: {@code 22 k}, {@code input}
     * @param origin  what a refusal names
     */
    public Expression value(LineGrammar line, ValueKind kind, Set<String> allowed, String written, String origin) {
        TokenCursor cursor;
        try {
            cursor = lexer().tokenize(DocumentSource.of(origin, written));
        } catch (SyntaxErrorException failure) {
            throw new GrammarRefusal(origin, 0, 0, failure.getMessage());
        }
        // The lexer opens every text with its start marker, which a document's reader passes before its first line.
        cursor.consumeIf(BasicToken.T_SOL);
        TokenCursor bounded = LineBoundaries.line(cursor);
        // A value is typed as either spelling writes it — a line's, '= 10 ns', or a term's argument, '10 ns' —: the kind's
        // own test of whether its line spelling stands here decides which reading it is, never a guess.
        ValueReading reading = new ValueReading(cursor, bounded, context(), Set.of(), allowed, cursor.current(), line);
        Expression value = kind.standsAt(cursor, Set.of(), allowed) ? kind.read(reading) : kind.readTerm(reading);
        if (bounded.hasNext()) {
            throw new GrammarRefusal(origin, 0, 0, "'" + written + "' holds more than one value: " + line.shape());
        }
        if (value != null && !allowed.isEmpty() && !allowed.contains(TextSourceWriter.render(value, SourceLayout.WORDS))) {
            throw new GrammarRefusal(origin, 0, 0, "'" + written + "' is not one of " + String.join(", ", new TreeSet<>(allowed))
                                                   + ": " + line.shape());
        }
        return value;
    }

    private static Lexer lexer() {
        return new DefaultLexer(new DefaultTokenizer(new ExpressionSplitter(DIALECT), new ScriptRecognizer()));
    }

    /** A context of one reading's own: a document's body is read with what that reading has met so far. */
    private ParserContext context() {
        StandardExtensionContainer extensions = new StandardExtensionContainer();
        extensions.importExtension(new ScriptExtension());
        extensions.importExtension(new GrammarExtension());
        extensions.importExtension(new DocumentExtension(grammar));
        modules.forEach(extensions::importExtension);
        return new DefaultParserContext(extensions);
    }
}
