package org.jmouse.parallax.store.subject;

import org.jmouse.formula.FormulaException;
import org.jmouse.parallax.Computations;
import org.jmouse.parallax.RepresentationKind;
import org.jmouse.parallax.script.engine.DefinedObject;
import org.jmouse.parallax.file.ObjectFiles;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ParameterDefinition;
import org.jmouse.parallax.store.catalogue.Catalogue;
import org.jmouse.parallax.store.catalogue.CatalogueViews.ObjectDetail;
import org.jmouse.parallax.store.catalogue.CatalogueViews.ObjectSummary;
import org.jmouse.parallax.store.catalogue.ObjectDocuments;
import org.jmouse.parallax.store.computation.ComputationRequest;
import org.jmouse.parallax.store.formula.FormulaNotations;
import org.jmouse.parallax.store.formula.FormulaNotations.Notation;
import org.jmouse.parallax.store.formula.FormulaNotations.Rendering;
import org.jmouse.parallax.problem.ParallaxProblem;

import java.util.List;
import java.util.Set;

/**
 * Turns a {@link ComputationSubject} into the one shape the interface and the computation both work from. A
 * catalogue subject is its stored object; an inline one becomes the same object description, unsaved — no id,
 * no slug, no status — so a page that draws an object draws a typed formula or an inline document without knowing
 * the difference. An inline document's detail carries its text as {@code source}.
 */
public class Subjects {

    /** A subject resolved, or an inline formula refused with words and a position. */
    public record SubjectView(String reference, boolean saved, ObjectDetail object, String refusal, Integer position) {
    }

    private static final String UNSAVED_TITLE = "Formula";
    private static final String DOCUMENT_NAME = "the inline document";

    private final Catalogue catalogue;
    private final Computations computations;
    private final FormulaNotations notations;
    private final ObjectDocuments documents;

    public Subjects(Catalogue catalogue, Computations computations, FormulaNotations notations, ObjectDocuments documents) {
        this.catalogue = catalogue;
        this.computations = computations;
        this.notations = notations;
        this.documents = documents;
    }

    public SubjectView resolve(String reference) {
        return switch (ComputationSubject.parse(reference)) {
            case ComputationSubject.Catalogue stored -> new SubjectView(stored.reference(), true, catalogue.detail(stored.slug()), null, null);
            case ComputationSubject.Inline inline -> resolveInline(inline);
            case ComputationSubject.Document document -> resolveDocument(document);
        };
    }

    /** The request with its subject spelled out as the stored object or the inline definition it names. */
    public ComputationRequest bind(ComputationRequest request) {
        if (request.subject() == null) {
            return request;
        }
        return switch (ComputationSubject.parse(request.subject())) {
            case ComputationSubject.Catalogue stored -> request.naming(stored.slug(), null, List.of());
            case ComputationSubject.Inline inline -> {
                ObjectFiles.ReadObject read = infer(inline);
                yield request.naming(null, read.definition(), read.parameters());
            }
            case ComputationSubject.Document document -> {
                DefinedObject defined = documents.read(document.source(), DOCUMENT_NAME);
                yield request.naming(null, defined.definition(), defined.parameters());
            }
        };
    }

    private SubjectView resolveInline(ComputationSubject.Inline inline) {
        Rendering rendering = notations.render(inline.source());
        if (rendering.refusal() != null) {
            return new SubjectView(inline.reference(), false, null, rendering.refusal(), rendering.position());
        }
        ObjectFiles.ReadObject read;
        try {
            read = infer(inline);
        } catch (ParallaxProblem unclassified) {
            return new SubjectView(inline.reference(), false, null, unclassified.getMessage(), null);
        }
        return new SubjectView(inline.reference(), false, unsavedObject(read.definition(), read.parameters(), rendering), null, null);
    }

    /** A document that does not read is refused with its words, as a formula is; it never fails the request. */
    private SubjectView resolveDocument(ComputationSubject.Document document) {
        DefinedObject defined;
        try {
            defined = documents.read(document.source(), DOCUMENT_NAME);
        } catch (ParallaxProblem unreadable) {
            return new SubjectView(document.reference(), false, null, unreadable.getMessage(), null);
        }
        Notation notation = notations.describe(defined.definition(), defined.formulaLatex());
        return new SubjectView(document.reference(), false,
                unsavedObject(defined.title(), defined.summary(), defined.definition(), defined.parameters(), notation,
                        document.source()), null, null);
    }

    private ObjectDetail unsavedObject(ObjectDefinition definition, List<ParameterDefinition> parameters, Rendering rendering) {
        return unsavedObject(UNSAVED_TITLE, null, definition, parameters, rendering.notation(), null);
    }

    private ObjectDetail unsavedObject(String title, String description, ObjectDefinition definition,
                                       List<ParameterDefinition> parameters, Notation notation, String source) {
        List<RepresentationKind> applicable = computations.applicableTo(definition, parameters);
        ObjectSummary summary = new ObjectSummary(null, null, title, description, definition.objectKind(),
                definition.objectKind().nature(), null,
                definition.ambientDimension(), notation.latex(), notation, false, null, null,
                Set.of(), applicable.isEmpty() ? null : applicable.getFirst(), 0, null, null);
        return new ObjectDetail(summary, null, definition, 0, parameters, List.of(), List.of(), applicable, List.of(),
                List.of(), null, 0, source, 0, false, null, false);
    }

    private static ObjectFiles.ReadObject infer(ComputationSubject.Inline inline) {
        try {
            return ObjectFiles.infer("formula", 1, inline.source());
        } catch (FormulaException | IllegalArgumentException unreadable) {
            throw ParallaxProblem.invalid("invalid-formula", unreadable.getMessage());
        }
    }
}
