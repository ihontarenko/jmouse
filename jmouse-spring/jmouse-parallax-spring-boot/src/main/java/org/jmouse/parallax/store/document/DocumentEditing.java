package org.jmouse.parallax.store.document;

import org.jmouse.parallax.script.Kind;
import org.jmouse.parallax.script.form.DocumentForm;
import org.jmouse.parallax.script.form.DocumentForms;
import org.jmouse.parallax.script.form.FormEdit;
import org.jmouse.formula.FormulaException;
import org.jmouse.parallax.script.engine.DefinedObject;
import org.jmouse.parallax.script.engine.ExperienceDefinitions;
import org.jmouse.parallax.object.DefinitionCompiler;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ParameterDefinition;
import org.jmouse.parallax.store.catalogue.ComponentModels;
import org.jmouse.parallax.store.catalogue.ObjectDocuments;
import org.jmouse.parallax.problem.ParallaxProblem;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A jMX text as a form, and a form's edit as a change of that text (DECISIONS §17).
 *
 * <p>Nothing here is stored: the editor holds the text, sends it with each edit and gets the next text back, and the
 * object is saved through the catalogue like any other text. Beyond the reader's own refusal, a text is read through the
 * adapter as a save would read it, so a form shows a formula that does not compile at its line while it is typed.</p>
 */
public class DocumentEditing {

    /** A document is at most this long; the catalogue refuses longer ones on save. */
    private static final int MAXIMUM_LENGTH = 1_000_000;

    private final ObjectDocuments documents;
    private final ComponentModels components;

    public DocumentEditing(ObjectDocuments documents, ComponentModels components) {
        this.documents = documents;
        this.components = components;
    }

    /** A text and its form: what an editor shows after loading a document or after an edit. */
    public record EditedDocument(String source, DocumentForm form) {
    }

    /**
     * What a symbol text draws — its drawing JSON, every living value read — or why it is refused, at its line: what a
     * symbol editor previews a living symbol by while it is written.
     */
    public record DrawnSymbol(String drawing, DocumentForm.Refusal refusal) {
    }

    public DrawnSymbol symbol(String source) {
        requireText(source);
        try {
            if (!(documents.read(source, "document").definition() instanceof ObjectDefinition.Symbol symbol)) {
                throw ParallaxProblem.invalidDocument("This text is not a symbol.", 1, 0);
            }
            return new DrawnSymbol(symbol.drawing(), null);
        } catch (ParallaxProblem.Invalid refused) {
            Object line = refused.details().get("line");
            Object column = refused.details().get("column");
            return new DrawnSymbol(null, new DocumentForm.Refusal(line instanceof Integer number ? number : 1,
                    column instanceof Integer number ? number : 0, refused.getMessage()));
        }
    }

    public EditedDocument form(String source) {
        return new EditedDocument(requireText(source), checked(source));
    }

    public EditedDocument edit(String source, FormEdit edit) {
        requireText(source);
        if (edit == null || edit.action() == null) {
            throw ParallaxProblem.invalid("An edit names its action.");
        }
        String edited;
        try {
            edited = DocumentForms.apply(source, edit);
        } catch (IllegalArgumentException refused) {
            throw ParallaxProblem.invalid(refused.getMessage());
        }
        return new EditedDocument(requireText(edited), checked(edited));
    }

    /**
     * What the schematic canvas drew, written into the circuit's text line by line: a part or a layout line changes
     * only when what it says changed, so a comment in the parts block survives a save from the canvas.
     */
    public EditedDocument patchCircuit(String source, ObjectDefinition definition, List<ParameterDefinition> parameters,
                                       String title, String summary, String category) {
        requireText(source);
        if (!(definition instanceof ObjectDefinition.Circuit circuit)) {
            throw ParallaxProblem.invalid("The schematic canvas draws a circuit.");
        }
        return patched(() -> ExperienceDefinitions.patchCircuit(source, circuit,
                parameters == null ? List.of() : parameters, title, summary, category));
    }

    /** What the drawing canvas drew, written into the symbol's text line by line. */
    public EditedDocument patchSymbol(String source, String drawing, String title, String category) {
        requireText(source);
        return patched(() -> ExperienceDefinitions.patchSymbol(source, drawing, title, category));
    }

    /** What the footprint editor drew, written into the footprint's text line by line. */
    public EditedDocument patchFootprint(String source, String drawing, String title, String category) {
        requireText(source);
        return patched(() -> ExperienceDefinitions.patchFootprint(source, drawing, title, category));
    }

    private EditedDocument patched(java.util.function.Supplier<String> patch) {
        String edited;
        try {
            edited = patch.get();
        } catch (IllegalArgumentException | ClassCastException refused) {
            throw ParallaxProblem.invalid(refused.getMessage());
        }
        return new EditedDocument(requireText(edited), checked(edited));
    }

    /** The form, with the adapter's refusal when the reader has none: a formula that does not compile, a bad unit. */
    private DocumentForm checked(String source) {
        DocumentForm form = DocumentForms.form(source);
        if (form.refusal().isPresent() || !isObject(form.kind())) {
            return form;
        }
        try {
            compile(source);
            return form;
        } catch (ParallaxProblem.Invalid refused) {
            Object line = refused.details().get("line");
            Object column = refused.details().get("column");
            DocumentForm.Refusal refusal = new DocumentForm.Refusal(line instanceof Integer number ? number : 1,
                    column instanceof Integer number ? number : 0, refused.getMessage());
            return new DocumentForm(form.kind(), form.title(), form.statements(), form.addable(), Optional.of(refusal));
        }
    }

    /** Reads the text through the adapter and compiles what it defines, as a save would: a formula that does not compile is refused at its place. */
    private void compile(String source) {
        DefinedObject defined = documents.read(source, "document");
        try {
            DefinitionCompiler.compile(defined.definition(), defined.parameters(), Map.of(), components.forEditing());
        } catch (FormulaException refused) {
            throw ObjectDocuments.refusal(refused, source);
        } catch (IllegalArgumentException | IllegalStateException refused) {
            throw ParallaxProblem.invalidDocument(refused.getMessage(), 1, 0);
        }
    }

    /** Whether a kind is stored as a catalogue object, and so read by the adapter when saved. */
    private static boolean isObject(String keyword) {
        return Kind.ofKeyword(keyword).map(kind -> kind.role() == Kind.Role.COMPUTABLE || kind.role() == Kind.Role.DRAWING
                        || kind.role() == Kind.Role.COMPOSITION)
                .orElse(false);
    }

    private static String requireText(String source) {
        if (source == null || source.isBlank()) {
            throw ParallaxProblem.invalid("A document is a text: 'kind \"Title\" { … }'.");
        }
        if (source.length() > MAXIMUM_LENGTH) {
            throw ParallaxProblem.invalid("A document is at most a million characters.");
        }
        return source;
    }
}
