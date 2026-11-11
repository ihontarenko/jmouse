package org.jmouse.cad.document;

import org.jmouse.el.grammar.BlockNode;
import org.jmouse.el.grammar.ClauseBlockNode;
import org.jmouse.el.grammar.ClauseBlockParser;
import org.jmouse.el.grammar.ClauseLineNode;
import org.jmouse.el.grammar.ClauseLineParser;
import org.jmouse.el.grammar.GrammarRefusal;
import org.jmouse.el.grammar.LineGrammar;
import org.jmouse.el.grammar.LineReader;
import org.jmouse.el.grammar.LineSet;
import org.jmouse.el.grammar.Nodes;
import org.jmouse.el.grammar.ValueKind;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.expression.ExpressionsNode;
import org.jmouse.el.writer.SourceLayout;
import org.jmouse.el.writer.TextSourceWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * A document's tree as an editor works on it: read as a form — its statements, each block's lines and every line's
 * values by role —, and changed by an edit that builds nodes and puts them in the tree. The text is then written by the
 * language's own writer, spelled as the document was read; nobody joins strings into a line, and no span of text is
 * patched.
 *
 * <p>A statement is addressed by its place in the document's body, a line by its place in its block; a drawn kind's line
 * — a symbol's {@code pin}, a footprint's {@code pad} — is a statement of its own.</p>
 */
public final class DocumentEditing {

    private final DocumentLanguage language;
    private final LanguageWalk     walk;

    public DocumentEditing(DocumentLanguage language, List<DocumentModule> modules) {
        this.language = language;
        this.walk = LanguageWalk.of(modules);
    }

    // ── The form ─────────────────────────────────────────────────────────────────────────────────────────────────

    /** What a statement of the body is. */
    public enum Form { BLOCK, LINE, DECLARATION, SCRIPT, META, OTHER }

    /**
     * A document as a form.
     *
     * @param statements its body, in the order written
     */
    public record DocumentForm(String kind, String slug, String title, String type, DocumentNode.Spelling spelling,
                               List<Statement> statements) {
    }

    /**
     * One statement of the body.
     *
     * @param header  a headed block's head — {@code symbol uses …} —, or null
     * @param lines   a block's lines; a line statement's own line, alone
     * @param names   a declaration's names: {@code use one, other}
     * @param fields  {@code meta}'s fields, as written
     * @param written what neither a grammar nor a field reads — a jMS body, an equation — as written
     */
    public record Statement(int index, Form form, String word, Line header, List<Line> lines, List<String> names,
                            List<Field> fields, String written) {
    }

    /**
     * One line, its values as written by what they are: a slot by its role, a clause by its word, the ending by its name.
     *
     * @param written the whole line as written, for a line no grammar reads
     */
    public record Line(int index, String word, List<String> prefixes, Map<String, String> slots, List<Field> clauses,
                       String ending, String written) {
    }

    /** A clause or a {@code meta} field: its word and its value as written; null for a flag. */
    public record Field(String word, String written) {
    }

    /** The document as a form. */
    public DocumentForm form(DocumentNode document) {
        List<Statement> statements = new ArrayList<>();
        List<Expression> body = document.getExpressions();
        for (int index = 0; index < body.size(); index++) {
            statements.add(statement(index, body.get(index)));
        }
        return new DocumentForm(document.kind().word(), document.slug(), document.title(), document.type().orElse(null),
                                document.spelling(), statements);
    }

    private static Statement statement(int index, Expression expression) {
        return switch (expression) {
            case MetaNode meta -> new Statement(index, Form.META, meta.word(), null, List.of(), List.of(),
                                                meta.lines().stream().map(line -> new Field(line.key(), line.text())).toList(),
                                                null);
            case ScriptBlockNode script -> new Statement(index, Form.SCRIPT, script.word(), null, List.of(), List.of(),
                                                         List.of(), script.written());
            case DeclarationNode declaration -> new Statement(index, Form.DECLARATION, declaration.word(), null, List.of(),
                                                              declaration.names(), List.of(), null);
            case ClauseBlockNode block -> new Statement(index, Form.BLOCK, block.word(),
                                                        block.header().map(header -> line(-1, header)).orElse(null),
                                                        lines(block), List.of(), List.of(), null);
            case ClauseLineNode line -> new Statement(index, Form.LINE, line.word(), null, List.of(line(0, line)), List.of(),
                                                      List.of(), null);
            default -> new Statement(index, Form.OTHER, null, null, List.of(), List.of(), List.of(), written(expression));
        };
    }

    private static List<Line> lines(BlockNode block) {
        List<Line> lines = new ArrayList<>();
        List<Expression> expressions = block.getExpressions();
        for (int index = 0; index < expressions.size(); index++) {
            Expression expression = expressions.get(index);
            lines.add(expression instanceof ClauseLineNode line ? line(index, line)
                              : new Line(index, null, List.of(), Map.of(), List.of(), null, written(expression)));
        }
        return lines;
    }

    private static Line line(int index, ClauseLineNode line) {
        Map<String, String> slots = new LinkedHashMap<>();
        for (LineGrammar.Slot slot : line.grammar().slots()) {
            line.slot(slot.role()).ifPresent(value -> slots.put(slot.role(), written(value)));
        }
        List<Field> clauses = line.clauses().stream()
                .map(clause -> new Field(clause.word(), clause.value() == null ? null : written(clause.value())))
                .toList();
        return new Line(index, line.word(), line.prefixes(), slots, clauses,
                        line.endingValue().map(DocumentEditing::written).orElse(null), null);
    }

    private static String written(Expression expression) {
        return TextSourceWriter.render(expression, SourceLayout.WORDS).strip();
    }

    // ── The edits ────────────────────────────────────────────────────────────────────────────────────────────────

    /** A change of a document's tree. */
    public sealed interface Edit permits SetLine, AddLine, RemoveLine, SetField, AddBlock, RemoveStatement, SetTitle, SetAddress {
    }

    /**
     * New values on a line that stands: each by its slot's role, its clause's word or its ending's name, as typed; an
     * empty value takes an optional one away. A flag is set by {@code flags}.
     *
     * @param line the line's place in its block, {@code -1} for a block's head; ignored for a line statement
     */
    public record SetLine(int statement, int line, Map<String, String> values, Map<String, Boolean> flags) implements Edit {
    }

    /**
     * A new line, of the grammar its word names, in a block — or, for a drawn kind, in the body.
     *
     * @param statement the block's place; {@code null} for a drawn kind's body
     * @param before    the place it stands before; {@code null} for after the last
     */
    public record AddLine(Integer statement, Integer before, String word, Map<String, String> values,
                          Map<String, Boolean> flags) implements Edit {
    }

    /**
     * A line taken away.
     *
     * @param line the line's place in its block; ignored for a line statement, which is taken away whole
     */
    public record RemoveLine(int statement, int line) implements Edit {
    }

    /** A {@code meta} field set to a value as written, or taken away by an empty one. */
    public record SetField(String key, String written) implements Edit {
    }

    /**
     * A block the document's kind allows and does not have yet — or one it may write again —, at the end of its body:
     * its head of {@code values} and {@code flags} when the block has one ({@code symbol uses …}), its body empty.
     */
    public record AddBlock(String word, Map<String, String> values, Map<String, Boolean> flags) implements Edit {
    }

    /** A statement of the body taken away whole: a block with its lines, a declaration, a line standing in the body. */
    public record RemoveStatement(int statement) implements Edit {
    }

    /** The document's title; its address stays, written out when the new title would give another. */
    public record SetTitle(String title) implements Edit {
    }

    /** The document's address — its slug —; its title stays. */
    public record SetAddress(String slug) implements Edit {
    }

    /**
     * Applies an edit to a document read for it, and answers the document's next text.
     *
     * @throws GrammarRefusal when a value does not read, a line its grammar refuses, or the edit names no place
     */
    public String apply(DocumentNode document, Edit edit) {
        DocumentNode written = switch (edit) {
            case SetTitle title -> document.retitled(required(title.title(), "title"));
            case SetAddress address -> document.readdressed(requireSlug(address.slug()));
            default -> document;
        };
        switch (edit) {
            case SetLine set -> setLine(document, set);
            case AddLine add -> addLine(document, add);
            case RemoveLine remove -> removeLine(document, remove);
            case SetField field -> setField(document, field);
            case AddBlock block -> addBlock(document, block);
            case RemoveStatement remove -> document.removeExpression(requireStatement(document, remove.statement()));
            case SetTitle ignored -> {
            }
            case SetAddress ignored -> {
            }
        }
        return language.writeAsRead(written);
    }

    private void setLine(DocumentNode document, SetLine set) {
        Expression statement = statementAt(document, set.statement());
        switch (statement) {
            case ClauseLineNode line -> document.replaceExpression(set.statement(), changed(line, set.values(), set.flags()));
            case ClauseBlockNode block when set.line() < 0 -> {
                ClauseLineNode header = block.header().orElseThrow(() -> refusal("'" + block.word() + "' has no head"));
                ClauseBlockNode headed = new ClauseBlockNode(block.word(), changed(header, set.values(), set.flags()).asHeader());
                headed.addLeadingTrivia(block.getLeadingTrivia());
                headed.setTrailingTrivia(block.getTrailingTrivia());
                block.getExpressions().forEach(headed::addExpression);
                if (block.opened()) {
                    headed.open();
                }
                document.replaceExpression(set.statement(), headed);
            }
            case ClauseBlockNode block -> {
                if (!(lineAt(block, set.line()) instanceof ClauseLineNode line)) {
                    throw refusal("line " + set.line() + " of '" + block.word() + "' is not one a form edits");
                }
                block.replaceExpression(set.line(), changed(line, set.values(), set.flags()));
            }
            default -> throw refusal("statement " + set.statement() + " has no lines a form edits");
        }
    }

    private void addLine(DocumentNode document, AddLine add) {
        ExpressionsNode holder;
        LineSet lines;
        if (add.statement() == null) {
            lines = walk.drawn().get(document.kind().word());
            if (lines == null) {
                throw refusal("a " + document.kind().word() + " holds its lines in blocks: name the block");
            }
            holder = document;
        } else if (statementAt(document, add.statement()) instanceof ClauseBlockNode block) {
            ClauseBlockParser parser = walk.blocks().get(block.word());
            if (parser == null || parser.lines() == null) {
                throw refusal("'" + block.word() + "' holds no lines a form adds");
            }
            lines = parser.lines();
            holder = block;
        } else {
            throw refusal("statement " + add.statement() + " is no block of lines");
        }
        LineGrammar grammar = grammarOf(lines, add.word());
        ClauseLineNode line = changed(new ClauseLineNode(grammar, true, Map.of(), List.of(), null), add.values(), add.flags());
        int size = holder.getExpressions().size();
        int before = add.before() == null ? size : add.before();
        if (before < 0 || before > size) {
            throw refusal("there is no place " + before + " to add a line at");
        }
        holder.insertExpression(before, line);
    }

    private static void removeLine(DocumentNode document, RemoveLine remove) {
        Expression statement = statementAt(document, remove.statement());
        if (statement instanceof ClauseBlockNode block) {
            lineAt(block, remove.line());
            block.removeExpression(remove.line());
        } else if (statement instanceof ClauseLineNode) {
            document.removeExpression(remove.statement());
        } else {
            throw refusal("statement " + remove.statement() + " has no lines a form removes");
        }
    }

    private void addBlock(DocumentNode document, AddBlock add) {
        DocumentKind kind = walk.kinds().get(document.kind().word());
        if (kind == null || !kind.blocks().contains(add.word())) {
            throw refusal("a " + document.kind().word() + " holds no '" + add.word() + "' block");
        }
        boolean written = document.getExpressions().stream()
                .anyMatch(statement -> statement instanceof BlockNode block && add.word().equals(block.word()));
        if (written && !kind.repeatable().contains(add.word())) {
            throw refusal("the " + document.kind().word() + " has its '" + add.word() + "' block already");
        }
        ClauseBlockParser parser = walk.blocks().get(add.word());
        if (parser == null) {
            throw refusal("'" + add.word() + "' is a block of another language: write it in the text");
        }
        ClauseLineNode header = parser.header()
                .map(grammar -> changed(new ClauseLineNode(grammar, true, Map.of(), List.of(), null), add.values(), add.flags()))
                .orElse(null);
        if (header == null && add.values() != null && !add.values().isEmpty()) {
            throw refusal("'" + add.word() + "' has no head to write " + add.values().keySet() + " in");
        }
        ClauseBlockNode block = new ClauseBlockNode(add.word(), header);
        if (parser.lines() != null) {
            block.open();
        }
        document.addExpression(block);
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw refusal("a document's " + name + " cannot be empty");
        }
        return value.strip();
    }

    /** A slug as an address writes it: what a title gives would be the same word — lower case, words joined by hyphens. */
    private static String requireSlug(String slug) {
        String written = required(slug, "address");
        if (!DocumentParser.slugOf(written).equals(written)) {
            throw refusal("'" + written + "' is no address: write it as '" + DocumentParser.slugOf(written) + "'");
        }
        return written;
    }

    private static int requireStatement(DocumentNode document, int index) {
        statementAt(document, index);
        return index;
    }

    private static void setField(DocumentNode document, SetField field) {
        boolean taken = field.written() == null || field.written().isBlank();
        setMeta(document, field.key(), taken ? null : metaField(field.key(), field.written().strip()));
    }

    /** One word — {@code circuits}, {@code yes} —, as a field's word is written. */
    private static final Pattern META_WORD = Pattern.compile("[\\p{L}\\p{N}_.-]+");

    /**
     * A field of the value a form shows — its words, not their spelling —, written as the language writes that value:
     * one word as a word, words between commas as a list, anything else as a text in quotes. A value typed in its quotes
     * is taken as written.
     */
    private static MetaLineNode metaField(String key, String value) {
        if (value.startsWith("\"")) {
            return new MetaLineNode(key, value, List.of());
        }
        if (META_WORD.matcher(value).matches()) {
            return MetaLineNode.of(key, Nodes.word(value));
        }
        List<String> words = Arrays.stream(value.split(",")).map(String::strip).toList();
        if (words.size() > 1 && words.stream().allMatch(word -> META_WORD.matcher(word).matches())) {
            return MetaLineNode.of(key, Nodes.words(words));
        }
        return MetaLineNode.of(key, Nodes.text(value));
    }

    /**
     * A {@code meta} field in place of the one written, what was written above and beside it kept; null takes it away.
     * A field that writes the same as the one written leaves it as it stands.
     */
    private static void setMeta(DocumentNode document, String key, MetaLineNode field) {
        MetaNode meta = document.block(MetaNode.class).orElse(null);
        if (meta == null) {
            if (field == null) {
                return;
            }
            meta = new MetaNode();
            document.insertExpression(0, meta);
        }
        List<Expression> lines = meta.getExpressions();
        for (int index = 0; index < lines.size(); index++) {
            if (lines.get(index) instanceof MetaLineNode line && line.key().equals(key)) {
                if (field == null) {
                    meta.removeExpression(index);
                } else if (!field.written().equals(line.written())) {
                    field.addLeadingTrivia(line.getLeadingTrivia());
                    field.setTrailingTrivia(line.getTrailingTrivia());
                    meta.replaceExpression(index, field);
                }
                return;
            }
        }
        if (field != null) {
            meta.addExpression(field);
        }
    }

    // ── A canvas's drawing ──────────────────────────────────────────────────────────────────────────────────────

    /**
     * What a canvas draws of a document, as statements a program built: the document's own, as its model draws them, and
     * the canvas's, in the same order — so the n-th statement of a class written is known by the n-th of its class drawn.
     *
     * @param before  the document's statements as its model draws them; empty when its text does not read as its model
     * @param after   the statements the canvas drew
     * @param classOf a statement's class — a pin, a pad, an element —; empty for a statement no canvas draws
     */
    public record Redrawing(List<? extends Expression> before, List<? extends Expression> after,
                            Function<Expression, Optional<String>> classOf) {

        public Redrawing {
            before = List.copyOf(before);
            after = List.copyOf(after);
        }
    }

    /**
     * What a canvas draws of one block of a document — a circuit's {@code parts}, {@code layout}, {@code probes} —, as lines a
     * program built, each known by its key: a part by its reference, a probe by its channel.
     *
     * @param block  the block's word
     * @param before the block's lines as the document's model draws them; empty when its text does not read as its model
     * @param after  the lines the canvas drew
     * @param keyOf  a line's key; empty for a line no canvas draws
     */
    public record BlockRedrawing(String block, List<? extends Expression> before, List<? extends Expression> after,
                                 Function<Expression, Optional<String>> keyOf) {

        public BlockRedrawing {
            before = List.copyOf(before);
            after = List.copyOf(after);
        }
    }

    /** What a canvas edits beside its drawing; a null field is left as written, an empty one taken away. */
    public record Head(String title, String summary, String category) {

        /** A head without a summary: what a drawing canvas edits. */
        public Head(String title, String category) {
            this(title, null, category);
        }
    }

    /**
     * A document with a canvas's drawing in place of its drawn statements, class by class: the n-th written statement of a
     * class stays as written — its comments with it — while it says what the n-th drawn one says, and the drawn one takes
     * its place, and what was written above and beside it, when it says something else. One more drawn than written is
     * written after the last of its class, one fewer takes the last away; a statement no canvas draws is never touched.
     * The text is the language's, spelled as the document was read.
     */
    public String redraw(DocumentNode document, Redrawing redrawing, Head head) {
        DocumentNode headed = headed(document, head);
        Map<String, List<Expression>> before = byClass(redrawing.before(), redrawing.classOf());
        Map<String, List<Expression>> after = byClass(redrawing.after(), redrawing.classOf());
        for (Map.Entry<String, List<Expression>> drawn : after.entrySet()) {
            List<Expression> said = before.getOrDefault(drawn.getKey(), List.of());
            List<Integer> written = placesOf(headed, drawn.getKey(), redrawing.classOf());
            int kept = Math.min(written.size(), drawn.getValue().size());
            for (int index = 0; index < kept; index++) {
                Expression wanted = drawn.getValue().get(index);
                if (index < said.size() && sameSaying(said.get(index), wanted)) {
                    continue;
                }
                Expression standing = headed.getExpressions().get(written.get(index));
                wanted.addLeadingTrivia(standing.getLeadingTrivia());
                wanted.setTrailingTrivia(standing.getTrailingTrivia());
                headed.replaceExpression(written.get(index), wanted);
            }
            for (int index = written.size() - 1; index >= kept; index--) {
                headed.removeExpression(written.get(index));
            }
            int place = written.isEmpty() ? placeOfNewClass(headed, drawn.getKey(), after, redrawing.classOf())
                    : written.get(kept - 1) + 1;
            for (int index = kept; index < drawn.getValue().size(); index++) {
                headed.insertExpression(place++, drawn.getValue().get(index));
            }
        }
        for (String gone : before.keySet()) {
            if (!after.containsKey(gone)) {
                List<Integer> written = placesOf(headed, gone, redrawing.classOf());
                for (int index = written.size() - 1; index >= 0; index--) {
                    headed.removeExpression(written.get(index));
                }
            }
        }
        return language.writeAsRead(headed);
    }

    /**
     * A document with a canvas's lines in place of the lines of its blocks, key by key: a written line stays as written —
     * its comments with it — while it says what the line drawn with its key says, the drawn one takes its place, and what
     * was written above and beside it, when it says something else, and a line whose key the canvas no longer draws goes.
     * A new key's line is written after the line of the key drawn before it; a block the canvas fills that the document
     * lacks is written after the blocks drawn before it, and a block the canvas empties goes. A line no canvas draws is
     * never touched. The text is the language's, spelled as the document was read.
     *
     * @param blocks in the order a document writes them
     */
    public String redraw(DocumentNode document, List<BlockRedrawing> blocks, Head head) {
        DocumentNode headed = headed(document, head);
        for (int index = 0; index < blocks.size(); index++) {
            redrawBlock(headed, blocks.get(index), blocks.subList(0, index));
        }
        return language.writeAsRead(headed);
    }

    private static void redrawBlock(DocumentNode document, BlockRedrawing redrawing, List<BlockRedrawing> earlier) {
        Map<String, Expression> before = byKey(redrawing.before(), redrawing.keyOf());
        Map<String, Expression> after = byKey(redrawing.after(), redrawing.keyOf());
        ClauseBlockNode block = blockOf(document, redrawing.block()).orElse(null);
        if (block == null) {
            if (after.isEmpty()) {
                return;
            }
            block = new ClauseBlockNode(redrawing.block(), null);
            block.open();
            document.insertExpression(placeOfNewBlock(document, earlier), block);
        }
        for (int index = block.getExpressions().size() - 1; index >= 0; index--) {
            Expression standing = block.getExpressions().get(index);
            Optional<String> key = redrawing.keyOf().apply(standing);
            if (key.isEmpty()) {
                continue;
            }
            Expression wanted = after.get(key.get());
            if (wanted == null) {
                block.removeExpression(index);
            } else if (!(before.containsKey(key.get()) && sameSaying(before.get(key.get()), wanted))) {
                wanted.addLeadingTrivia(standing.getLeadingTrivia());
                wanted.setTrailingTrivia(standing.getTrailingTrivia());
                block.replaceExpression(index, wanted);
            }
        }
        List<String> keys = List.copyOf(after.keySet());
        for (int index = 0; index < keys.size(); index++) {
            String key = keys.get(index);
            if (placeOfKey(block, key, redrawing.keyOf()) >= 0) {
                continue;
            }
            int place = -1;
            for (int earlierKey = index - 1; earlierKey >= 0 && place < 0; earlierKey--) {
                place = placeOfKey(block, keys.get(earlierKey), redrawing.keyOf());
            }
            block.insertExpression(place < 0 ? firstKeyedPlace(block, redrawing.keyOf()) : place + 1, after.get(key));
        }
        if (block.getExpressions().isEmpty()) {
            document.removeExpression(document.getExpressions().indexOf(block));
        }
    }

    private static Map<String, Expression> byKey(List<? extends Expression> lines, Function<Expression, Optional<String>> keyOf) {
        Map<String, Expression> keyed = new LinkedHashMap<>();
        for (Expression line : lines) {
            String key = keyOf.apply(line)
                    .orElseThrow(() -> new IllegalArgumentException("a canvas drew a line known by no key: " + line));
            if (keyed.put(key, line) != null) {
                throw new IllegalArgumentException("a canvas drew two lines of '" + key + "'");
            }
        }
        return keyed;
    }

    private static Optional<ClauseBlockNode> blockOf(DocumentNode document, String word) {
        return document.getExpressions().stream().filter(ClauseBlockNode.class::isInstance).map(ClauseBlockNode.class::cast)
                .filter(block -> block.word().equals(word)).findFirst();
    }

    /** After the last of the blocks drawn before it that the document holds; else after its last statement. */
    private static int placeOfNewBlock(DocumentNode document, List<BlockRedrawing> earlier) {
        for (int index = earlier.size() - 1; index >= 0; index--) {
            Optional<ClauseBlockNode> written = blockOf(document, earlier.get(index).block());
            if (written.isPresent()) {
                return document.getExpressions().indexOf(written.get()) + 1;
            }
        }
        return document.getExpressions().size();
    }

    private static int placeOfKey(ClauseBlockNode block, String key, Function<Expression, Optional<String>> keyOf) {
        List<Expression> lines = block.getExpressions();
        for (int index = 0; index < lines.size(); index++) {
            if (keyOf.apply(lines.get(index)).filter(key::equals).isPresent()) {
                return index;
            }
        }
        return -1;
    }

    /** Where a block's drawn lines begin — the first of them —; the block's end when it holds none. */
    private static int firstKeyedPlace(ClauseBlockNode block, Function<Expression, Optional<String>> keyOf) {
        List<Expression> lines = block.getExpressions();
        for (int index = 0; index < lines.size(); index++) {
            if (keyOf.apply(lines.get(index)).isPresent()) {
                return index;
            }
        }
        return lines.size();
    }

    /** The document under the canvas's title, with its summary and category as the canvas set them. */
    private static DocumentNode headed(DocumentNode document, Head head) {
        DocumentNode headed = head.title() == null || head.title().isBlank() || head.title().strip().equals(document.title())
                ? document : document.retitled(head.title().strip());
        // A summary is a text and a category a word: each written as its node writes itself, never joined into a line.
        if (head.summary() != null) {
            setMeta(headed, MetaNode.SUMMARY, head.summary().isBlank() ? null
                    : MetaLineNode.of(MetaNode.SUMMARY, Nodes.text(head.summary().strip())));
        }
        if (head.category() != null) {
            setMeta(headed, MetaNode.CATEGORY, head.category().isBlank() ? null
                    : MetaLineNode.of(MetaNode.CATEGORY, Nodes.word(head.category().strip())));
        }
        return headed;
    }

    private static Map<String, List<Expression>> byClass(List<? extends Expression> statements,
                                                         Function<Expression, Optional<String>> classOf) {
        Map<String, List<Expression>> classes = new LinkedHashMap<>();
        for (Expression statement : statements) {
            String drawnClass = classOf.apply(statement)
                    .orElseThrow(() -> new IllegalArgumentException("a canvas drew a statement of no class it draws: " + statement));
            classes.computeIfAbsent(drawnClass, ignored -> new ArrayList<>()).add(statement);
        }
        return classes;
    }

    private static List<Integer> placesOf(DocumentNode document, String drawnClass,
                                          Function<Expression, Optional<String>> classOf) {
        List<Integer> places = new ArrayList<>();
        List<Expression> body = document.getExpressions();
        for (int index = 0; index < body.size(); index++) {
            if (classOf.apply(body.get(index)).filter(drawnClass::equals).isPresent()) {
                places.add(index);
            }
        }
        return places;
    }

    /** Where the first of a class goes: after the last written of the classes drawn before it, else after the last statement. */
    private static int placeOfNewClass(DocumentNode document, String drawnClass, Map<String, List<Expression>> after,
                                       Function<Expression, Optional<String>> classOf) {
        int place = document.getExpressions().size();
        for (String earlier : after.keySet()) {
            if (earlier.equals(drawnClass)) {
                break;
            }
            List<Integer> written = placesOf(document, earlier, classOf);
            if (!written.isEmpty()) {
                place = written.getLast() + 1;
            }
        }
        return place;
    }

    /** Whether two statements a program built say the same: they are written the same. */
    private static boolean sameSaying(Expression said, Expression drawn) {
        return TextSourceWriter.render(said, SourceLayout.WORDS).equals(TextSourceWriter.render(drawn, SourceLayout.WORDS));
    }

    // ── Building a line ──────────────────────────────────────────────────────────────────────────────────────────

    /** The line with the values and flags set, each value read by the language; what it wrote above and beside kept. */
    private ClauseLineNode changed(ClauseLineNode line, Map<String, String> values, Map<String, Boolean> flags) {
        LineGrammar grammar = line.grammar();
        Map<String, String> asked = values == null ? Map.of() : values;
        Map<String, Boolean> flagged = flags == null ? Map.of() : flags;
        Map<String, Expression> slots = new LinkedHashMap<>();
        for (LineGrammar.Slot slot : grammar.slots()) {
            line.slot(slot.role()).ifPresent(value -> slots.put(slot.role(), value));
        }
        List<ClauseLineNode.Written> clauses = new ArrayList<>(line.clauses());
        Expression ending = line.endingValue().orElse(null);
        for (Map.Entry<String, String> entry : asked.entrySet()) {
            String name = entry.getKey();
            String written = entry.getValue() == null ? "" : entry.getValue().strip();
            Optional<LineGrammar.Slot> slot = grammar.slots().stream().filter(candidate -> candidate.role().equals(name)).findFirst();
            Optional<LineGrammar.Clause> clause = grammar.clauses().stream().filter(candidate -> candidate.word().equals(name)).findFirst();
            if (slot.isPresent()) {
                if (written.isEmpty()) {
                    requireOptional(slot.get().optional(), name, grammar);
                    slots.remove(name);
                } else {
                    slots.put(name, read(grammar, slot.get().value(), slot.get().allowed(), written));
                }
            } else if (clause.isPresent()) {
                if (clause.get().isNested()) {
                    throw refusal("'" + name + "' holds a line of its own; a form does not edit it yet");
                }
                clauses.removeIf(present -> present.word().equals(name));
                if (written.isEmpty()) {
                    requireOptional(!clause.get().required(), name, grammar);
                } else {
                    clauses.add(new ClauseLineNode.Written(name, read(grammar, clause.get().value(), clause.get().allowed(),
                                                                       written)));
                }
            } else if (name.equals(grammar.ending())) {
                ending = written.isEmpty() ? null : read(grammar, grammar.endingValue(), Set.of(), written);
            } else {
                throw refusal("'" + name + "' is no part of a " + describe(grammar) + ": " + grammar.shape());
            }
        }
        for (Map.Entry<String, Boolean> flag : flagged.entrySet()) {
            LineGrammar.Clause clause = grammar.clauses().stream().filter(candidate -> candidate.word().equals(flag.getKey()))
                    .findFirst().orElseThrow(() -> refusal("'" + flag.getKey() + "' is no flag of a " + describe(grammar)));
            clauses.removeIf(present -> present.word().equals(clause.word()));
            if (Boolean.TRUE.equals(flag.getValue())) {
                clauses.add(new ClauseLineNode.Written(clause.word(), null));
            }
        }
        ClauseLineNode built = new ClauseLineNode(grammar, true, line.prefixes(), slots, clauses, ending);
        built.addLeadingTrivia(line.getLeadingTrivia());
        built.setTrailingTrivia(line.getTrailingTrivia());
        for (LineGrammar.Slot slot : grammar.slots()) {
            if (!slot.optional() && !slots.containsKey(slot.role())) {
                throw refusal("a " + describe(grammar) + " needs its '" + slot.role() + "': " + grammar.shape());
            }
        }
        if (grammar.check() != null) {
            String refused = grammar.check().apply(built, grammar);
            if (refused != null) {
                throw refusal(refused);
            }
        }
        return built;
    }

    private Expression read(LineGrammar grammar, ValueKind kind, Set<String> allowed,
                            String written) {
        return language.value(grammar, kind, allowed, written, "the edit");
    }

    private static LineGrammar grammarOf(LineSet lines, String word) {
        for (LineReader reader : lines.readers()) {
            for (LineReader alternative : reader.alternatives()) {
                if (alternative instanceof ClauseLineParser parser
                        && (word.equals(parser.grammar().word()) || word.equals(parser.grammar().termWord()))) {
                    return parser.grammar();
                }
            }
        }
        throw refusal("'" + word + "' is no line this holds");
    }

    private static void requireOptional(boolean optional, String name, LineGrammar grammar) {
        if (!optional) {
            throw refusal("a " + describe(grammar) + " cannot go without its '" + name + "': " + grammar.shape());
        }
    }

    private static String describe(LineGrammar grammar) {
        return grammar.word() != null ? grammar.word() + " line" : grammar.termWord() + " line";
    }

    private static Expression statementAt(DocumentNode document, int index) {
        List<Expression> body = document.getExpressions();
        if (index < 0 || index >= body.size()) {
            throw refusal("there is no statement " + index + "; the body holds " + body.size());
        }
        return body.get(index);
    }

    private static Expression lineAt(BlockNode block, int index) {
        List<Expression> lines = block.getExpressions();
        if (index < 0 || index >= lines.size()) {
            throw refusal("'" + block.word() + "' has no line " + index + "; it holds " + lines.size());
        }
        return lines.get(index);
    }

    private static GrammarRefusal refusal(String detail) {
        return new GrammarRefusal("the edit", 0, 0, detail);
    }
}
