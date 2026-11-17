package org.jmouse.cad.store.catalogue;

import org.jmouse.cad.document.source.DocumentLocation;
import org.jmouse.cad.folder.StoredDocument;
import org.jmouse.cad.document.DocumentOwner;
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

    /**
     * Where a copy of a document is held, as a list says it ({@link DocumentLocation}, ADR 0001). For a copy in the
     * database the view's own owner, id and revision say the rest.
     *
     * @param place the library that ships it, the mount it is in, the format another product's file is read by; null for
     *              the database
     * @param path  where in that place — a jar's path, a mount's key, the file —; null for the database
     * @param entry what in the file it is, for one of several documents a foreign file holds; null otherwise
     */
    public record LocationView(DocumentLocation.Kind kind, DocumentLocation.Access access, String place, String path,
                               String entry) {

        public static LocationView of(DocumentLocation location) {
            return switch (location) {
                case DocumentLocation.Stored stored -> new LocationView(stored.kind(), stored.access(), null, null, null);
                case DocumentLocation.Shipped shipped -> new LocationView(shipped.kind(), shipped.access(), shipped.library(),
                        shipped.path(), null);
                case DocumentLocation.Mounted mounted -> new LocationView(mounted.kind(), mounted.access(), mounted.mount(),
                        mounted.key(), null);
                case DocumentLocation.Foreign foreign -> new LocationView(foreign.kind(), foreign.access(), foreign.format(),
                        foreign.file(), foreign.entry());
            };
        }

        /** A catalogued document's copy, at its head: in the database, or the file a library ships it in. */
        public static LocationView of(StoredDocument document) {
            return of(DocumentTexts.locationOf(document, document.getRevision()));
        }
    }

    /**
     * A category — a {@code category} document —, with how many published catalogue documents it files itself, not
     * counting those of the categories under it.
     *
     * @param parent the category it stands under, or null at the top
     * @param order  where it stands among its siblings, or null where its document says nothing
     * @param owner  whose it is — a workspace's, for the category of a library an import brought there —; null for the
     *               catalogue's
     */
    public record CategoryView(String code, String title, String summary, String parent, Integer order, long documentCount,
                               DocumentOwner owner) {
    }

    /** A category as a curator writes it: its document's title, summary, parent and order; no order puts it last. */
    public record CategoryDraft(String title, String summary, String parent, Integer order) {
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
     * @param problem why the catalogue refuses its head text as its file holds it — it does not read, it is filed in what is
     *                no category, its drawing disagrees with its parts —; null for a text it takes
     */
    public record DocumentSummary(String id, String slug, String title, String summary, String kind, String category,
                                  String folder, boolean featured, String status, LocationView location, DocumentOwner owner,
                                  List<String> tags, String benchRole, int revision, Instant updatedAt,
                                  String problem) {

        /** A document of the store as a list shows it, its problem the refusal its file stands under, if any. */
        public static DocumentSummary of(StoredDocument document) {
            return of(document, document.refusal().orElse(null));
        }

        /** A document of the store as a list shows it, with the problem its text is refused for. */
        public static DocumentSummary of(StoredDocument document, String problem) {
            return new DocumentSummary(document.getId(), document.getSlug(), document.getTitle(), document.getSummary(),
                                       document.getKind(), document.getCategoryCode(), document.getFolder(),
                                       document.isFeatured(), document.getStatus().name(), LocationView.of(document),
                                       document.owner().orElse(null), List.copyOf(document.getTags()), document.getBenchRole(),
                                       document.getRevision(), document.getUpdatedAt(), problem);
        }
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
                                 List<ParameterSliders.Slider> parameters, String presentation, boolean writable) {
    }

    /** Another document this one names, and how. */
    public record RelatedView(String slug, String title, String kind, String relation) {
    }

    /** One saved revision of a document's text, without the text. */
    public record RevisionView(int revision, String checksum, Instant writtenAt, String writtenBy) {
    }

    /** One revision's text. */
    public record RevisionSource(int revision, String source) {
    }

    /** A circuit of an owner as a sheet of one design: what it hands its parent, and the sheets it places. */
    public record SheetView(String id, String slug, String title, String category, Instant updatedAt, List<String> ports,
                            List<String> nets, List<SheetBlock> blocks, String problem) {
    }

    /** A part of a sheet that places another sheet of the same owner. */
    public record SheetBlock(String block, String sheet) {
    }
}
