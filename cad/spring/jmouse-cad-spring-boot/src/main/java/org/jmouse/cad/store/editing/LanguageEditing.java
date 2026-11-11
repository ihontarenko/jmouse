package org.jmouse.cad.store.editing;

import org.jmouse.cad.board.BoardModule;
import org.jmouse.cad.board.drawing.BoardDrawing;
import org.jmouse.cad.board.drawing.FootprintDrawing;
import org.jmouse.cad.document.DocumentEditing;
import org.jmouse.cad.document.DocumentEditing.AddBlock;
import org.jmouse.cad.document.DocumentEditing.AddLine;
import org.jmouse.cad.document.DocumentEditing.DocumentForm;
import org.jmouse.cad.document.DocumentEditing.Edit;
import org.jmouse.cad.document.DocumentEditing.Head;
import org.jmouse.cad.document.DocumentEditing.Redrawing;
import org.jmouse.cad.document.DocumentEditing.RemoveLine;
import org.jmouse.cad.document.DocumentEditing.RemoveStatement;
import org.jmouse.cad.document.DocumentEditing.SetAddress;
import org.jmouse.cad.document.DocumentEditing.SetTitle;
import org.jmouse.cad.document.DocumentEditing.SetField;
import org.jmouse.cad.document.DocumentEditing.SetLine;
import org.jmouse.cad.document.DocumentInput;
import org.jmouse.cad.document.DocumentKind;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.LanguageSchema;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.schematic.SchematicModule;
import org.jmouse.cad.schematic.drawing.DrawingStatements;
import org.jmouse.cad.schematic.drawing.LivingDrawing;
import org.jmouse.cad.schematic.drawing.SymbolDrawing;
import org.jmouse.cad.simulation.CircuitLines;
import org.jmouse.cad.simulation.CircuitSheet;
import org.jmouse.cad.simulation.SimulationModule;
import org.jmouse.cad.simulation.SimulationReading;
import org.jmouse.cad.simulation.ParameterSliders;
import org.jmouse.cad.store.catalogue.DocumentReading;
import org.jmouse.cad.store.catalogue.ModelFacts;
import org.jmouse.cad.store.problem.CadProblem;
import org.jmouse.el.grammar.GrammarRefusal;
import org.jmouse.el.node.Expression;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * The jM language for editors — {@code JMOUSE_CAD_API.md} §2.5: a text as a form, and a form's edit as the next text.
 * Nothing is stored: the editor holds the text, sends it with each edit, and saves it through the catalogue like any
 * other text.
 *
 * <p>⚠️ An edit reads its text afresh every time, never through the cache of read documents: an edit changes the tree
 * it is applied to, and a cached tree is shared.</p>
 */
public class LanguageEditing {

    private final DocumentEditing editing;
    private final DocumentReading reading;
    private final CadLanguage     language;

    public LanguageEditing(DocumentReading reading) {
        this.reading = reading;
        this.language = reading.language();
        this.editing = new DocumentEditing(language.language(), language.modules());
    }

    /** The language as an editor builds its forms from it. */
    public LanguageSchema schema() {
        return language.schema();
    }

    /** What a text does not read as, where: an editor shows a half-written text with these rather than refusing it. */
    public record Problem(int line, int column, String detail) {
    }

    /**
     * A text, its form, and what is wrong with it.
     *
     * @param form     null when the text does not read at all
     * @param problems empty when it reads and its model reads
     */
    public record EditedDocument(String source, DocumentForm form, List<Problem> problems) {
    }

    /**
     * An edit as an editor sends it.
     *
     * @param operation {@code set-line}, {@code add-line}, {@code remove-line} or {@code set-field}
     * @param statement the statement's place in the body; null to add a drawn kind's line to the body
     * @param line      the line's place in its block; {@code -1} for a block's head
     * @param before    where a new line stands; null for after the last
     * @param word      the word of the line added: its grammar's word, or its term's
     * @param values    values as written, by slot role, clause word or ending name; an empty one takes it away
     * @param flags     flags set or cleared, by clause word
     * @param key       the {@code meta} field set
     * @param written   the {@code meta} field's value as written; empty takes it away
     */
    public record LineEdit(String operation, Integer statement, Integer line, Integer before, String word,
                           Map<String, String> values, Map<String, Boolean> flags, String key, String written) {
    }

    /**
     * A text's drawing as the engine draws it, unsaved: what a canvas previews while its text is being written.
     *
     * @param drawing a {@code SymbolDrawing}, a {@code FootprintDrawing} or a {@code BoardDrawing}; null when the text does
     *                not read, and then {@code problems} says why
     */
    public record DrawnText(String source, String kind, Object drawing, List<Problem> problems) {
    }

    /** A text read as a form; one that does not read answers its problem and no form. */
    public EditedDocument form(String source) {
        DocumentNode document;
        try {
            document = readFresh(source);
        } catch (GrammarRefusal refused) {
            return new EditedDocument(source, null, List.of(new Problem(refused.line(), refused.column(), refused.detail())));
        }
        return new EditedDocument(source, editing.form(document), problemsOf(source));
    }

    /** The text after one edit, with its form; an edit the language refuses is refused whole. */
    public EditedDocument edit(String source, LineEdit edit) {
        DocumentNode document;
        try {
            document = readFresh(source);
        } catch (GrammarRefusal refused) {
            throw CadProblem.invalidDocument(refused.detail(), refused.line(), refused.column());
        }
        String next;
        try {
            next = editing.apply(document, editOf(edit));
        } catch (GrammarRefusal refused) {
            throw CadProblem.invalidDocument(refused.detail(), refused.line(), refused.column());
        } catch (IndexOutOfBoundsException | IllegalArgumentException refused) {
            throw CadProblem.invalid(refused.getMessage());
        }
        return form(next);
    }

    /**
     * What a schematic editor drew, written into the circuit's text line by line, each known by its reference: a part, a
     * placement, a wire or a probe the sheet did not change stays as it is written, with its comments; what the sheet does
     * not draw — parameters, ports, expectations — is never touched.
     */
    public EditedDocument circuit(String source, CircuitSheet sheet, String title, String summary, String category) {
        DocumentNode document = readKind(source, Set.of(SimulationModule.CIRCUIT));
        if (sheet == null) {
            throw CadProblem.invalid("A schematic editor sends what it drew as 'circuit'.");
        }
        CircuitLines lines = new CircuitLines(language.language(), document.slug());
        CircuitSheet before;
        try {
            before = CircuitSheet.of(SimulationReading.circuit(document));
        } catch (RuntimeException unread) {
            before = null;
        }
        List<DocumentEditing.BlockRedrawing> blocks;
        try {
            blocks = sheet.redrawing(before, lines);
        } catch (IllegalArgumentException | GrammarRefusal refused) {
            throw CadProblem.invalid("The sheet does not write: " + refused.getMessage());
        }
        try {
            return form(editing.redraw(document, blocks, new Head(title, summary, category)));
        } catch (GrammarRefusal refused) {
            throw CadProblem.invalidDocument(refused.detail(), refused.line(), refused.column());
        } catch (IllegalArgumentException refused) {
            throw CadProblem.invalid(refused.getMessage());
        }
    }

    /** The kinds a drawing canvas draws. */
    private static final Set<DocumentKind> DRAWN_KINDS = Set.of(SchematicModule.SYMBOL, SchematicModule.PORTRAIT,
                                                                SchematicModule.FRAME);

    /**
     * What a drawing canvas drew — a symbol, a portrait, a frame —, written into its text statement by statement: a
     * statement the drawing did not change stays as it is written, with its comments.
     */
    public EditedDocument drawing(String source, SymbolDrawing drawing, String title, String category) {
        DocumentNode document = readKind(source, DRAWN_KINDS);
        if (drawing == null) {
            throw CadProblem.invalid("A drawing canvas sends what it drew as 'drawing'.");
        }
        String origin = document.slug();
        Redrawing redrawing = new Redrawing(
                drawnOrNone(() -> DrawingStatements.of(LivingDrawing.of(document), language.language(), origin)),
                drawnLines(() -> DrawingStatements.of(drawing, language.language(), origin)), DrawingStatements::classOf);
        return redrawn(document, redrawing, new Head(title, category));
    }

    /**
     * What a footprint editor drew, written into the footprint's text line by line: a pad, an outline or a field the
     * drawing did not change stays as it is written, with its comments.
     */
    public EditedDocument footprint(String source, FootprintDrawing drawing, String title, String category) {
        DocumentNode document = readKind(source, Set.of(BoardModule.FOOTPRINT));
        if (drawing == null) {
            throw CadProblem.invalid("A footprint editor sends what it drew as 'drawing'.");
        }
        Redrawing redrawing = new Redrawing(drawnOrNone(() -> FootprintDrawing.of(document).lines()), drawnLines(drawing::lines),
                                            FootprintDrawing::classOf);
        return redrawn(document, redrawing, new Head(title, category));
    }

    /**
     * What a board editor laid out, written into the board's text line by line: a line the layout did not change stays as
     * it is written, with its comments; its {@code sizes}, which the layout does not hold, are never touched.
     */
    public EditedDocument board(String source, BoardDrawing drawing, String title, String category) {
        DocumentNode document = readKind(source, Set.of(BoardModule.PCB));
        if (drawing == null) {
            throw CadProblem.invalid("A board editor sends what it laid out as 'drawing'.");
        }
        Redrawing redrawing = new Redrawing(drawnOrNone(() -> BoardDrawing.of(document).lines()), drawnLines(drawing::lines),
                                            BoardDrawing::classOf);
        return redrawn(document, redrawing, new Head(title, category));
    }

    /**
     * An unsaved text read as the document it would be: its model, its parameters as sliders and how it is presented —
     * what a draft is previewed by. One that does not read answers its problem and no model.
     */
    public record ReadText(String kind, String slug, String title, Object model, List<ParameterSliders.Slider> parameters,
                           String presentation, List<Problem> problems) {
    }

    /** A text read as its model, without saving it. */
    public ReadText read(String source) {
        DocumentNode document;
        try {
            document = readFresh(source);
        } catch (GrammarRefusal refused) {
            return new ReadText(null, null, null, null, List.of(), null,
                                List.of(new Problem(refused.line(), refused.column(), refused.detail())));
        }
        Object model;
        try {
            model = reading.read(source, "the document").model().orElse(null);
        } catch (CadProblem refused) {
            return new ReadText(document.kind().word(), document.slug(), document.title(), null, List.of(), null,
                                List.of(problemOf(refused)));
        }
        return new ReadText(document.kind().word(), document.slug(), document.title(), model, ModelFacts.slidersOf(model),
                            ModelFacts.presentationOf(model), List.of());
    }

    /** A text's drawing as the engine draws it, without saving it. */
    public DrawnText drawn(String source) {
        DocumentNode document;
        try {
            document = readFresh(source);
        } catch (GrammarRefusal refused) {
            return new DrawnText(source, null, null, List.of(new Problem(refused.line(), refused.column(), refused.detail())));
        }
        String kind = document.kind().word();
        Object model;
        try {
            model = reading.read(source, "the document").model().orElse(null);
        } catch (CadProblem refused) {
            return new DrawnText(source, kind, null, List.of(problemOf(refused)));
        }
        if (!(model instanceof SymbolDrawing || model instanceof FootprintDrawing || model instanceof BoardDrawing)) {
            throw CadProblem.invalid("A " + kind + " is not drawn: a symbol, a portrait, a frame, a footprint and a board are.");
        }
        return new DrawnText(source, kind, model, List.of());
    }

    private EditedDocument redrawn(DocumentNode document, Redrawing redrawing, Head head) {
        try {
            return form(editing.redraw(document, redrawing, head));
        } catch (GrammarRefusal refused) {
            throw CadProblem.invalidDocument(refused.detail(), refused.line(), refused.column());
        }
    }

    /** A drawing's statements, a drawing a canvas sent that does not draw refused as the canvas's. */
    private static List<? extends Expression> drawnLines(Supplier<List<? extends Expression>> lines) {
        try {
            return lines.get();
        } catch (IllegalArgumentException | GrammarRefusal refused) {
            throw CadProblem.invalid("The drawing does not write: " + refused.getMessage());
        }
    }

    /** A document's own statements as its model draws them; none when its text does not read as its model yet. */
    private static List<? extends Expression> drawnOrNone(Supplier<List<? extends Expression>> lines) {
        try {
            return lines.get();
        } catch (RuntimeException unread) {
            return List.of();
        }
    }

    private DocumentNode readKind(String source, Set<DocumentKind> kinds) {
        DocumentNode document;
        try {
            document = readFresh(source);
        } catch (GrammarRefusal refused) {
            throw CadProblem.invalidDocument(refused.detail(), refused.line(), refused.column());
        }
        if (!kinds.contains(document.kind())) {
            throw CadProblem.invalid("This editor draws " + String.join(", ", kinds.stream().map(DocumentKind::word).sorted().toList())
                                     + "; the text holds a " + document.kind().word() + ".");
        }
        return document;
    }

    private DocumentNode readFresh(String source) {
        if (source == null || source.isBlank()) {
            throw CadProblem.invalid("A document is its text: send it as 'source'.");
        }
        return language.language().readOne(DocumentInput.text("the document", source));
    }

    /** What the engine refuses of a text that reads: a part no wire reaches, a drawing it cannot draw. */
    private List<Problem> problemsOf(String source) {
        try {
            reading.read(source, "the document");
            return List.of();
        } catch (CadProblem refused) {
            return List.of(problemOf(refused));
        }
    }

    private static Problem problemOf(CadProblem refused) {
        Object line = refused.details().get("line");
        Object column = refused.details().get("column");
        return new Problem(line instanceof Integer number ? number : 0, column instanceof Integer number ? number : 0,
                           refused.getMessage());
    }

    private static Edit editOf(LineEdit edit) {
        String operation = edit.operation() == null ? "" : edit.operation();
        return switch (operation) {
            case "set-line" -> new SetLine(required(edit.statement(), "statement"), edit.line() == null ? 0 : edit.line(),
                                           edit.values(), edit.flags());
            case "add-line" -> new AddLine(edit.statement(), edit.before(), requiredWord(edit.word()), edit.values(),
                                           edit.flags());
            case "remove-line" -> new RemoveLine(required(edit.statement(), "statement"), edit.line() == null ? 0 : edit.line());
            case "set-field" -> new SetField(requiredWord(edit.key()), edit.written());
            case "add-block" -> new AddBlock(requiredWord(edit.word()), edit.values(), edit.flags());
            case "remove-statement" -> new RemoveStatement(required(edit.statement(), "statement"));
            case "set-title" -> new SetTitle(edit.written());
            case "set-address" -> new SetAddress(edit.written());
            default -> throw CadProblem.invalid("An edit is set-line, add-line, remove-line, set-field, add-block, "
                                                + "remove-statement, set-title or set-address, not '" + operation + "'.");
        };
    }

    private static int required(Integer value, String name) {
        if (value == null) {
            throw CadProblem.invalid("This edit names its '" + name + "'.");
        }
        return value;
    }

    private static String requiredWord(String word) {
        if (word == null || word.isBlank()) {
            throw CadProblem.invalid("This edit names the word it is of.");
        }
        return word.strip();
    }
}
