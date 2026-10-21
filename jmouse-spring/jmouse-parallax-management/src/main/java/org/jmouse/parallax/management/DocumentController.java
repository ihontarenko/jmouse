package org.jmouse.parallax.management;

import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.parallax.script.form.FormEdit;
import org.jmouse.parallax.script.form.LanguageSchema;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ParameterDefinition;
import org.jmouse.parallax.store.document.DocumentEditing;
import org.jmouse.parallax.store.document.DocumentEditing.DrawnSymbol;
import org.jmouse.parallax.store.document.DocumentEditing.EditedDocument;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The jMX language for editors: its schema, a text as a form, and a form's edit as the next text. Nothing is stored
 * here; an editor saves the text through the catalogue.
 */
@RestController
@RequestMapping(ParallaxManagementRoutes.PREFIX + "/jmx")
public class DocumentController {

    private final DocumentEditing editing;

    public DocumentController(DocumentEditing editing) {
        this.editing = editing;
    }

    public record SourceRequest(String source) {
    }

    public record EditRequest(String source, FormEdit edit) {
    }

    /** The schematic canvas: the circuit it drew, its sliders, and the head fields it edits beside the sheet. */
    public record SchematicRequest(String source, ObjectDefinition definition, List<ParameterDefinition> parameters,
                                   String title, String summary, String category) {
    }

    /** A drawing canvas — a symbol's, a footprint's —: the drawing as JSON, and the head fields it edits beside the sheet. */
    public record DrawingRequest(String source, String drawing, String title, String category) {
    }

    /** Every kind, the blocks each may hold, and the fields of their lines — what forms are generated from. */
    @GetMapping("/schema")
    @PublicEndpoint("the language schema describes the API, not any data")
    public LanguageSchema schema() {
        return LanguageSchema.describe();
    }

    @PostMapping("/form")
    @PublicEndpoint("reading a text the caller sent; nothing is read from or written to the catalogue")
    public EditedDocument form(@RequestBody SourceRequest request) {
        return editing.form(request.source());
    }

    @PostMapping("/edit")
    @PublicEndpoint("changing a text the caller sent; nothing is read from or written to the catalogue")
    public EditedDocument edit(@RequestBody EditRequest request) {
        return editing.edit(request.source(), request.edit());
    }

    @PostMapping("/schematic")
    @PublicEndpoint("changing a text the caller sent; nothing is read from or written to the catalogue")
    public EditedDocument schematic(@RequestBody SchematicRequest request) {
        return editing.patchCircuit(request.source(), request.definition(), request.parameters(), request.title(),
                request.summary(), request.category());
    }

    @PostMapping("/drawing")
    @PublicEndpoint("changing a text the caller sent; nothing is read from or written to the catalogue")
    public EditedDocument drawing(@RequestBody DrawingRequest request) {
        return editing.patchSymbol(request.source(), request.drawing(), request.title(), request.category());
    }

    @PostMapping("/footprint")
    @PublicEndpoint("changing a text the caller sent; nothing is read from or written to the catalogue")
    public EditedDocument footprint(@RequestBody DrawingRequest request) {
        return editing.patchFootprint(request.source(), request.drawing(), request.title(), request.category());
    }

    @PostMapping("/symbol")
    @PublicEndpoint("reading a text the caller sent; nothing is read from or written to the catalogue")
    public DrawnSymbol symbol(@RequestBody SourceRequest request) {
        return editing.symbol(request.source());
    }
}
