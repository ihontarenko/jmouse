package org.jmouse.cad.store.catalogue;

import org.jmouse.cad.jpa.catalogue.DocumentOwner;
import org.jmouse.cad.simulation.ParameterSliders;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * What the catalogue answers and takes — the shapes {@code JMOUSE_CAD_API.md} §3 writes out with examples.
 */
public final class CatalogueViews {

    private CatalogueViews() {
    }

    /** Where a document comes from: the shared catalogue, or an owner's own. */
    public enum Origin { CATALOGUE, OWNED }

    /** A section of the catalogue, with how many published catalogue documents it files. */
    public record CategoryView(String code, String title, String description, int position, long documentCount) {
    }

    /** A section as a curator writes it; {@code position} left out puts it last. */
    public record CategoryDraft(String title, String description, Integer position) {
    }

    /** A document's text: everything a write sends. */
    public record DocumentText(String source) {
    }

    /** The status a curator moves a catalogue document to. */
    public record StatusChange(String status) {
    }

    /** The slugs a list asks to have summarised. */
    public record SlugList(List<String> slugs) {
    }

    /**
     * A document as a list shows it.
     *
     * @param problem why its head text no longer reads — the language moved on since it was saved; null when it reads
     */
    public record DocumentSummary(String id, String slug, String title, String summary, String kind, String category,
                                  String folder, boolean featured, String status, Origin origin, DocumentOwner owner,
                                  List<String> tags, int revision, String revisionLabel, Instant updatedAt,
                                  String problem) {
    }

    /**
     * A document as its page shows it: the text of the revision asked for, and everything read from it.
     *
     * @param sourceRevision the revision {@code source} is: the head, or the one an {@code @} pinned
     * @param meta           every {@code meta} field besides summary, category and tags, as written
     * @param model          the engine's model of the text, or null when it does not read or is of a kind with none
     * @param parameters     its parameters as a bench offers them — each worked out in SI with its range, step and the
     *                       unit it is written in —, so an interface draws sliders without computing a formula; empty for a
     *                       document with none, or whose values do not compute
     * @param writable       whether the reader may write its next revision
     */
    public record DocumentDetail(DocumentSummary summary, String source, int sourceRevision, long version,
                                 Map<String, String> meta, List<RelatedView> related, Object model,
                                 List<ParameterSliders.Slider> parameters, String presentation, boolean shippedUpdatePending,
                                 boolean writable) {
    }

    /** Another document this one names, and how. */
    public record RelatedView(String slug, String title, String kind, String relation) {
    }

    /** One saved revision of a document's text, without the text. */
    public record RevisionView(int revision, String label, String checksum, Instant writtenAt, String writtenBy) {
    }

    /** One revision's text. */
    public record RevisionSource(int revision, String label, String source) {
    }

    /** A circuit of an owner as a sheet of one design: what it hands its parent, and the sheets it places. */
    public record SheetView(String id, String slug, String title, Instant updatedAt, List<String> ports,
                            List<String> nets, List<SheetBlock> blocks, String problem) {
    }

    /** A part of a sheet that places another sheet of the same owner. */
    public record SheetBlock(String block, String sheet) {
    }
}
