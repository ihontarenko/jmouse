package org.jmouse.cad.management;

import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.cad.board.drawing.BoardDrawing;
import org.jmouse.cad.board.drawing.FootprintDrawing;
import org.jmouse.cad.document.LanguageSchema;
import org.jmouse.cad.schematic.drawing.SymbolDrawing;
import org.jmouse.cad.simulation.CircuitSheet;
import org.jmouse.cad.store.catalogue.CatalogueViews.DocumentText;
import org.jmouse.cad.store.editing.LanguageEditing;
import org.jmouse.cad.store.editing.LanguageEditing.DrawnText;
import org.jmouse.cad.store.editing.LanguageEditing.EditedDocument;
import org.jmouse.cad.store.editing.LanguageEditing.LineEdit;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ✏️ The jM language for editors — {@code JMOUSE_CAD_API.md} §2.5: what a form is built from. Nothing stored is read.
 */
@RestController
@RequestMapping(CadManagementRoutes.PREFIX)
public class LanguageController {

    private final LanguageEditing editing;

    public LanguageController(LanguageEditing editing) {
        this.editing = editing;
    }

    /** What a form sends: the text it holds, and the edit to make of it. */
    public record EditRequest(String source, LineEdit edit) {
    }

    /** What a canvas sends: the text it holds, what it drew, and the head fields it edits beside the drawing. */
    public record DrawingRequest<T>(String source, T drawing, String title, String category) {
    }

    /** What a schematic editor sends: the text it holds, the sheet it drew, and the head fields it edits beside it. */
    public record CircuitRequest(String source, CircuitSheet circuit, String title, String summary, String category) {
    }

    /** Every kind, block and line of the language, read off the grammar the reader reads with. */
    @GetMapping("/language/schema")
    @PublicEndpoint("the language itself; nothing stored is read")
    public LanguageSchema schema() {
        return editing.schema();
    }

    /** A text as a form, with what does not read in it. */
    @PostMapping("/language/form")
    @PublicEndpoint("a text the caller sent; nothing stored is read")
    public EditedDocument form(@RequestBody DocumentText text) {
        return editing.form(text.source());
    }

    /** The text after one edit, written by the language. */
    @PostMapping("/language/edit")
    @PublicEndpoint("a text the caller sent; nothing stored is read")
    public EditedDocument edit(@RequestBody EditRequest request) {
        return editing.edit(request.source(), request.edit());
    }

    /** The text after a schematic editor's sheet, written line by line, each known by its reference. */
    @PostMapping("/language/circuit")
    @PublicEndpoint("a text the caller sent; nothing stored is read")
    public EditedDocument circuit(@RequestBody CircuitRequest request) {
        return editing.circuit(request.source(), request.circuit(), request.title(), request.summary(), request.category());
    }

    /** The text after a drawing canvas's drawing — a symbol's, a portrait's, a frame's —, written statement by statement. */
    @PostMapping("/language/drawing")
    @PublicEndpoint("a text the caller sent; nothing stored is read")
    public EditedDocument drawing(@RequestBody DrawingRequest<SymbolDrawing> request) {
        return editing.drawing(request.source(), request.drawing(), request.title(), request.category());
    }

    /** The text after a footprint editor's drawing, written line by line. */
    @PostMapping("/language/footprint")
    @PublicEndpoint("a text the caller sent; nothing stored is read")
    public EditedDocument footprint(@RequestBody DrawingRequest<FootprintDrawing> request) {
        return editing.footprint(request.source(), request.drawing(), request.title(), request.category());
    }

    /** The text after a board editor's layout, written line by line. */
    @PostMapping("/language/board")
    @PublicEndpoint("a text the caller sent; nothing stored is read")
    public EditedDocument board(@RequestBody DrawingRequest<BoardDrawing> request) {
        return editing.board(request.source(), request.drawing(), request.title(), request.category());
    }

    /** A text read as the document it would be, without saving it: what a draft is previewed by. */
    @PostMapping("/language/read")
    @PublicEndpoint("a text the caller sent; the catalogue is read only for what everybody may read")
    public LanguageEditing.ReadText read(@RequestBody DocumentText text) {
        return editing.read(text.source());
    }

    /** A text's drawing, drawn without saving it: what a canvas previews. */
    @PostMapping("/language/drawn")
    @PublicEndpoint("a text the caller sent; nothing stored is read")
    public DrawnText drawn(@RequestBody DocumentText text) {
        return editing.drawn(text.source());
    }
}
