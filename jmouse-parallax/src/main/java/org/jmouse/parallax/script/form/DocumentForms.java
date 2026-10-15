package org.jmouse.parallax.script.form;

import org.jmouse.parallax.script.ExperienceNotation;
import org.jmouse.parallax.script.ExperienceParseException;
import org.jmouse.parallax.script.ExperienceReader;
import org.jmouse.parallax.script.Kind;
import org.jmouse.parallax.script.form.DocumentForm.LineForm;
import org.jmouse.parallax.script.form.DocumentForm.Refusal;
import org.jmouse.parallax.script.form.DocumentForm.StatementForm;
import org.jmouse.parallax.script.form.DocumentOutline.Body;
import org.jmouse.parallax.script.form.DocumentOutline.Line;
import org.jmouse.parallax.script.form.DocumentOutline.Span;
import org.jmouse.parallax.script.form.DocumentOutline.Statement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * A jMX text as a form, and a form's edit as a change of that text.
 *
 * <h2>⚠️ A form patches the text, it never writes the document</h2>
 *
 * <p>An edit replaces one span — a line's content, a block's header, a body between braces, a title — and leaves
 * every other character where it was. A comment above a parameter, the order of the blocks, the alignment the author
 * chose and the text of every line nobody touched survive any number of edits. A changed line is written in the
 * line's own form by its codec; its indentation and a comment after it stay.</p>
 */
public final class DocumentForms {

    private static final String DOCUMENT = "document";

    private DocumentForms() {
    }

    // ─── text → form ────────────────────────────────────────────────────────────────────────────────

    public static DocumentForm form(String source) {
        DocumentOutline outline = DocumentOutline.of(source);
        Optional<Refusal> refusal = refusalOf(source);
        Optional<Kind> kind = Kind.ofKeyword(outline.kind());
        String title = outline.title().of(source);
        if (kind.isEmpty()) {
            return new DocumentForm(outline.kind(), title, List.of(), List.of(), refusal);
        }
        List<StatementForm> statements = new ArrayList<>();
        for (int index = 0; index < outline.statements().size(); index++) {
            statements.add(statement(kind.get(), source, outline.statements().get(index), index));
        }
        return new DocumentForm(outline.kind(), title, statements, addable(kind.get(), outline), refusal);
    }

    private static StatementForm statement(Kind kind, String source, Statement statement, int index) {
        int line = lineOf(source, statement.span().start());
        String header = statement.header().of(source);
        if (statement.body().isEmpty()) {
            return oneLine(kind, source, statement, index, line);
        }
        Body body = statement.body().get();
        String word = statement.word();
        BlockSchema.Editor editor = LanguageSchema.editorOf(word);
        if (editor == BlockSchema.Editor.BODY) {
            String text = ExperienceNotation.dedent(source.substring(body.open() + 1, body.close()));
            return new StatementForm(index, word, editor, line, List.of(), List.of(), List.of(), text, null);
        }
        List<FormField> headerFields = List.of();
        String problem = null;
        LineCodec codec = LineCodecs.of(word);
        if (word.equals("symbol")) {
            if (StatementCodecs.namesSymbols(header)) {
                try {
                    headerFields = StatementCodecs.readSymbolHeader(kind, header);
                } catch (ExperienceParseException refused) {
                    problem = refused.getDetail();
                    headerFields = asWritten(header);
                }
            } else {
                editor = BlockSchema.Editor.DRAWING;
                codec = LineCodecs.AS_WRITTEN;
                headerFields = header.isBlank() ? List.of() : asWritten(header);
            }
        } else if (!header.isBlank()) {
            headerFields = asWritten(header);
        }
        List<LineForm> lines = new ArrayList<>();
        for (int position = 0; position < body.lines().size(); position++) {
            lines.add(line(kind, source, body.lines().get(position), position, codec, word));
        }
        return new StatementForm(index, word, editor, line, headerFields, lines, codec.fields(), null, problem);
    }

    private static StatementForm oneLine(Kind kind, String source, Statement statement, int index, int line) {
        String text = statement.span().of(source);
        String word = statement.word();
        if (StatementCodecs.isDeclaration(word)) {
            try {
                return new StatementForm(index, word, BlockSchema.Editor.STATEMENT, line,
                        StatementCodecs.readDeclaration(kind, text), List.of(), List.of(), null, null);
            } catch (ExperienceParseException refused) {
                return new StatementForm(index, word, BlockSchema.Editor.STATEMENT, line, asWritten(text), List.of(),
                        List.of(), null, refused.getDetail());
            }
        }
        BlockSchema.Editor editor = kind == Kind.SYMBOL && Kind.DRAWING_LINES.contains(word)
                ? BlockSchema.Editor.DRAWING : BlockSchema.Editor.STATEMENT;
        return new StatementForm(index, word, editor, line, asWritten(text), List.of(), List.of(), null, null);
    }

    private static LineForm line(Kind kind, String source, Line line, int index, LineCodec codec, String block) {
        String text = line.content().of(source);
        int number = lineOf(source, line.content().start());
        try {
            return new LineForm(index, number, codec.read(kind, text), null);
        } catch (ExperienceParseException refused) {
            return new LineForm(index, number, asWritten(text), refused.getDetail());
        } catch (RuntimeException unexpected) {
            return new LineForm(index, number, asWritten(text), "this line cannot be shown as fields in '" + block + "'");
        }
    }

    private static List<FormField> asWritten(String text) {
        return LineCodecs.AS_WRITTEN.read(Kind.CURVE, text);
    }

    /** The blocks a kind allows and the document has not written yet — a repeatable block always — and the declarations. */
    private static List<String> addable(Kind kind, DocumentOutline outline) {
        Set<String> written = new HashSet<>();
        outline.statements().forEach(statement -> written.add(statement.word()));
        Set<String> addable = new TreeSet<>();
        for (String block : kind.blocks()) {
            if (Kind.REPEATABLE_BLOCKS.contains(block) || !written.contains(block)) {
                addable.add(block);
            }
        }
        for (String declaration : Kind.DECLARATIONS) {
            if (declaration.equals("uses") || !written.contains(declaration)) {
                addable.add(declaration);
            }
        }
        return List.copyOf(addable);
    }

    private static Optional<Refusal> refusalOf(String source) {
        try {
            ExperienceReader.readOne(source, DOCUMENT);
            return Optional.empty();
        } catch (ExperienceParseException refused) {
            return Optional.of(new Refusal(refused.getLine(), refused.getColumn(), refused.getDetail()));
        }
    }

    // ─── edit → text ────────────────────────────────────────────────────────────────────────────────

    /**
     * The text after one edit; only the span the edit names changes.
     *
     * @throws IllegalArgumentException when the edit names a statement or a line the text does not have
     */
    public static String apply(String source, FormEdit edit) {
        DocumentOutline outline = DocumentOutline.of(source);
        return switch (edit.action()) {
            case SET_TITLE -> replace(source, outline.title(), edit.text().replace("\"", "'").strip());
            case SET_LINE -> setLine(source, outline, edit);
            case ADD_LINE -> addLine(source, outline, edit);
            case REMOVE_LINE -> remove(source, lineOf(outline, edit).whole());
            case SET_HEADER -> setHeader(source, outline, edit);
            case SET_BODY -> setBody(source, statementOf(outline, edit), edit.text());
            case SET_STATEMENT -> setStatement(source, outline, edit);
            case REMOVE_STATEMENT -> remove(source, physical(source, statementOf(outline, edit).span()));
            case ADD_STATEMENT -> addStatement(source, outline, edit);
        };
    }

    private static String setLine(String source, DocumentOutline outline, FormEdit edit) {
        Statement statement = statementOf(outline, edit);
        Line line = lineOf(outline, edit);
        if (unchanged(source, outline, statement, line, edit)) {
            return source;
        }
        String text = written(source, statement, edit, line.indent());
        return replace(source, line.content(), text);
    }

    /**
     * Whether an edit sends back the values a line already has: then the line keeps its text to the character, columns
     * the author aligned included.
     */
    private static boolean unchanged(String source, DocumentOutline outline, Statement statement, Line line, FormEdit edit) {
        Optional<Kind> kind = Kind.ofKeyword(outline.kind());
        if (kind.isEmpty()) {
            return false;
        }
        String text = line.content().of(source);
        LineCodec codec = edit.values().containsKey("line") ? LineCodecs.AS_WRITTEN : codecOf(source, statement);
        try {
            Map<String, String> current = valuesOf(codec.read(kind.get(), text));
            return current.entrySet().stream().allMatch(entry ->
                    entry.getValue().strip().equals(LineCodecs.value(edit.values(), entry.getKey())));
        } catch (RuntimeException unreadable) {
            return false;
        }
    }

    private static String addLine(String source, DocumentOutline outline, FormEdit edit) {
        Statement statement = statementOf(outline, edit);
        Body body = statement.body().orElseThrow(() -> new IllegalArgumentException(
                "'" + statement.word() + "' is a statement of one line and holds no lines"));
        String indent = body.lines().isEmpty() ? statement.indent() + "    " : body.lines().getLast().indent();
        String text = written(source, statement, edit, indent);
        if (body.lines().isEmpty()) {
            boolean closeOnOwnLine = source.substring(body.open() + 1, body.close()).contains("\n");
            if (closeOnOwnLine) {
                return insert(source, body.open() + 1, "\n" + indent + text);
            }
            return replace(source, new Span(body.open() + 1, body.close()), "\n" + indent + text + "\n" + statement.indent());
        }
        int end = source.indexOf('\n', body.lines().getLast().content().end());
        int at = end < 0 || end > body.close() ? body.lines().getLast().content().end() : end;
        return insert(source, at, "\n" + indent + text);
    }

    private static String setHeader(String source, DocumentOutline outline, FormEdit edit) {
        Statement statement = statementOf(outline, edit);
        String header = statement.word().equals("symbol") && !edit.values().containsKey("line")
                ? StatementCodecs.writeSymbolHeader(edit.values()) : LineCodecs.value(edit.values(), "line");
        Span span = statement.header();
        if (span.start() == span.end()) {
            return insert(source, span.start(), header + " ");
        }
        return replace(source, span, header);
    }

    private static String setBody(String source, Statement statement, String text) {
        Body body = statement.body().orElseThrow(() -> new IllegalArgumentException(
                "'" + statement.word() + "' has no body"));
        String inner = statement.indent() + "    ";
        String indented = Arrays.stream(ExperienceNotation.dedent(text).split("\n", -1))
                .map(line -> line.isBlank() ? "" : inner + line)
                .collect(Collectors.joining("\n"));
        return replace(source, new Span(body.open() + 1, body.close()), "\n" + indented + "\n" + statement.indent());
    }

    private static String setStatement(String source, DocumentOutline outline, FormEdit edit) {
        Statement statement = statementOf(outline, edit);
        String text = StatementCodecs.isDeclaration(statement.word()) && !edit.values().containsKey("line")
                ? StatementCodecs.writeDeclaration(statement.word(), edit.values())
                : LineCodecs.value(edit.values(), "line");
        return replace(source, statement.span(), text);
    }

    /** An empty block, a declaration with its value, or a statement of one line as written, after the last statement. */
    private static String addStatement(String source, DocumentOutline outline, FormEdit edit) {
        String word = edit.text().strip();
        if (!word.matches("[a-z_]+")) {
            throw new IllegalArgumentException("'" + word + "' is not a block or a declaration");
        }
        String indent = outline.statements().isEmpty() ? "    " : outline.statements().getFirst().indent();
        String statement = edit.values().containsKey("line")
                ? LineCodecs.value(edit.values(), "line")
                : StatementCodecs.isDeclaration(word)
                ? word + " " + String.join(", ", LineCodecs.words(edit.values(), word))
                : word + (word.equals("symbol") ? " " + StatementCodecs.writeSymbolHeader(edit.values()) : "")
                  + " {\n" + indent + "}";
        int at = outline.statements().isEmpty() ? outline.headEnd() + 1 : outline.statements().getLast().span().end();
        return insert(source, at, "\n" + indent + statement);
    }

    // ─── finding what an edit names ─────────────────────────────────────────────────────────────────

    private static Statement statementOf(DocumentOutline outline, FormEdit edit) {
        if (edit.statement() < 0 || edit.statement() >= outline.statements().size()) {
            throw new IllegalArgumentException("the document has no statement " + edit.statement());
        }
        return outline.statements().get(edit.statement());
    }

    private static Line lineOf(DocumentOutline outline, FormEdit edit) {
        Statement statement = statementOf(outline, edit);
        List<Line> lines = statement.body().map(Body::lines).orElse(List.of());
        if (edit.line() < 0 || edit.line() >= lines.size()) {
            throw new IllegalArgumentException("'" + statement.word() + "' has no line " + edit.line());
        }
        return lines.get(edit.line());
    }

    /** A line from its fields, or as written when the form sends it whole — a line it could not show as fields. */
    private static String written(String source, Statement statement, FormEdit edit, String indent) {
        if (edit.values().containsKey("line")) {
            return LineCodecs.value(edit.values(), "line");
        }
        return codecOf(source, statement).write(edit.values(), indent);
    }

    /** The codec a statement's lines are written with: a component's port lines under {@code symbol uses}. */
    private static LineCodec codecOf(String source, Statement statement) {
        if (statement.word().equals("symbol") && !StatementCodecs.namesSymbols(statement.header().of(source))) {
            return LineCodecs.AS_WRITTEN;
        }
        return LineCodecs.of(statement.word());
    }

    // ─── text ───────────────────────────────────────────────────────────────────────────────────────

    private static String replace(String source, Span span, String text) {
        return source.substring(0, span.start()) + text + source.substring(span.end());
    }

    private static String insert(String source, int at, String text) {
        return source.substring(0, at) + text + source.substring(at);
    }

    private static String remove(String source, Span span) {
        return replace(source, span, "");
    }

    /** A statement's span grown to its whole lines when nothing else shares them. */
    private static Span physical(String source, Span span) {
        int lineStart = source.lastIndexOf('\n', span.start() - 1) + 1;
        int start = source.substring(lineStart, span.start()).isBlank() ? lineStart : span.start();
        int lineEnd = source.indexOf('\n', span.end());
        int end = lineEnd >= 0 && source.substring(span.end(), lineEnd).isBlank() ? lineEnd + 1 : span.end();
        return new Span(start, end);
    }

    private static int lineOf(String source, int offset) {
        int line = 1;
        for (int index = 0; index < offset && index < source.length(); index++) {
            if (source.charAt(index) == '\n') {
                line++;
            }
        }
        return line;
    }

    /** The values of a line as a map, by field name — what a form sends back when nothing changed. */
    public static Map<String, String> valuesOf(List<FormField> fields) {
        return fields.stream().collect(Collectors.toMap(field -> field.schema().name(), FormField::value,
                (first, second) -> first, java.util.LinkedHashMap::new));
    }
}
