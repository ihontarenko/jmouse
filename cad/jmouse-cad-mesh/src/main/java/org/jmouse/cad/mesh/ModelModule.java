package org.jmouse.cad.mesh;

import java.util.Map;
import org.jmouse.cad.document.DocumentModel;
import org.jmouse.cad.document.DocumentKind;
import org.jmouse.cad.document.DocumentModule;
import org.jmouse.el.parser.Parser;

import java.util.List;

/**
 * The {@code model} document: the 3D files that show a part's body ({@link ModelLinesParser}). Brought to the language
 * by the plugin that reads and shows models — without it, a footprint's model line names a model nothing reads.
 */
public final class ModelModule implements DocumentModule {

    public static final DocumentKind MODEL = DocumentKind.of("model").drawnBy(ModelLinesParser.class);

    @Override
    public List<DocumentKind> kinds() {
        return List.of(MODEL);
    }

    /** A model is the files it is made of, each in its format and convention, or the shapes it is drawn from. */
    @Override
    public Map<DocumentKind, DocumentModel<?>> models() {
        return Map.of(MODEL, DocumentModel.of(ModelContents.class, document -> new ModelContents(ModelFile.of(document), ModelShapes.of(document))));
    }

    @Override
    public List<Parser> getParsers() {
        return List.of(new ModelLinesParser());
    }
}
