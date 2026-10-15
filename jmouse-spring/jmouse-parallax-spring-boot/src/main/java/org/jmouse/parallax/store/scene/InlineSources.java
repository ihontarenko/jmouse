package org.jmouse.parallax.store.scene;

import org.jmouse.parallax.script.engine.DefinedObject;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ParameterDefinition;
import org.jmouse.parallax.store.catalogue.ObjectDocuments;
import org.jmouse.parallax.problem.ParallaxProblem;

import java.util.List;

/**
 * An object a scene or a comparison defines inline, kept as its jMX text — the text is the truth there as everywhere.
 * A reader may still send a definition and its parameters, as a form builds them; it is written as text before it is
 * kept, and every answer carries the definition read back from that text beside the text itself.
 */
public class InlineSources {

    private static final String INLINE_SLUG = "inline";

    private final ObjectDocuments documents;

    public InlineSources(ObjectDocuments documents) {
        this.documents = documents;
    }

    /** The text an inline object is kept as: the one sent, or the one written for the definition sent. */
    public String sourceOf(String source, ObjectDefinition definition, List<ParameterDefinition> parameters, String label) {
        if (source != null && !source.isBlank()) {
            return source;
        }
        if (definition == null) {
            throw ParallaxProblem.invalid("An inline object is its jMX text, or a definition to write as one.");
        }
        String title = label == null || label.isBlank() ? "Inline formula" : label.strip();
        return documents.write(new DefinedObject(INLINE_SLUG, title, null, null, "GENERAL", List.of(), false, null,
                definition, parameters == null ? List.of() : parameters, List.of(), List.of(), List.of(), List.of()));
    }

    /** What an inline object's text defines. */
    public DefinedObject read(String source) {
        return documents.read(source, "the inline object");
    }
}
