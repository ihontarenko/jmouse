package org.jmouse.cad.folder;

import org.jmouse.cad.document.DocumentOwner;
import org.jmouse.cad.document.DocumentStatus;
import org.jmouse.revisions.RevisionLabel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 📇 The index of a document of the catalogue or of an owner's: what lists, searches and authorization need without
 * reading it. The document itself is its jM text — a file of the library folder, every revision a commit, or a file a
 * library ships —; everything here but its address, owner and place is read from the head text, and never edited on
 * its own. A document with no owner is the shared catalogue's; an owned one belongs to whatever the host names.
 *
 * <p>Its identifier is its slug: the one address a document has, which a text names it by and a route reaches it by.
 * {@link #getKind() Its kind} and {@link Relation#getKind() a relation's} are the words the language writes them with —
 * {@code component}, {@code footprint} —, so a kind a new module brings needs no change here.</p>
 */
public class StoredDocument {

    /** Where a document's text is held. */
    public enum Holding {
        /** A file of the library folder: every revision of it a commit. */
        STORED,
        /** A file a library ships: read in place, one version, read only. */
        SHIPPED
    }

    private final String slug;
    private final String ownerType;
    private final String ownerId;
    private final String createdBy;
    private final Instant createdAt;
    private String title;
    private String summary;
    private String code;
    private String kind;
    private String categoryCode;
    private Holding holding;
    private String shippedLibrary;
    private String shippedPath;
    private int revision;
    private String revisionLabel;
    private String sourceChecksum;
    private DocumentStatus status;
    private boolean featured;
    private Instant updatedAt;
    private Instant publishedAt;
    private String refusal;
    private final List<Relation> relations = new ArrayList<>();
    private final Set<String> tags = new LinkedHashSet<>();

    /**
     * @param owner     null for the shared catalogue
     * @param createdBy the actor who wrote it; null for the shipped library
     */
    public StoredDocument(String slug, DocumentOwner owner, String createdBy, Instant now) {
        this.slug = slug;
        this.ownerType = owner == null ? null : owner.type();
        this.ownerId = owner == null ? null : owner.id();
        this.createdBy = createdBy;
        this.status = DocumentStatus.DRAFT;
        this.holding = Holding.STORED;
        this.revision = 0;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Moves the document to a new head revision and takes the index from what that revision says.
     *
     * @param label    the name the new head is said by
     * @param checksum the new head text's checksum
     * @param kind     the word its head opens with
     */
    public void indexRevision(int revision, RevisionLabel label, String checksum, String kind, String title, String code,
                              String summary, String categoryCode, boolean featured, Instant now) {
        this.code = code;
        this.revision = revision;
        this.revisionLabel = label.token();
        this.sourceChecksum = checksum;
        this.kind = kind;
        this.title = title;
        this.summary = summary;
        this.categoryCode = categoryCode;
        this.featured = featured;
        this.updatedAt = now;
    }

    /** Indexes a document a library ships, held in its file: where the file stands, so the index says where to read it. */
    public void holdShipped(String library, String path) {
        this.holding = Holding.SHIPPED;
        this.shippedLibrary = library;
        this.shippedPath = path;
    }

    public void replaceRelations(List<Relation> relations) {
        this.relations.clear();
        this.relations.addAll(relations);
    }

    public void replaceTags(Set<String> tags) {
        this.tags.clear();
        this.tags.addAll(tags);
    }

    /** Where the document stands now, as its head text says; the first time it is published is remembered. */
    public void changeStatus(DocumentStatus status, Instant now) {
        if (status == DocumentStatus.PUBLISHED && this.publishedAt == null) {
            this.publishedAt = now;
        }
        this.status = status;
    }

    /**
     * What the catalogue finds wrong with the head text as its file holds it — filed in what is no category, a drawing
     * that disagrees with its parts, a text that does not read —; none for a text it takes. A file is the truth, so a text
     * put in the folder by hand is indexed even when it is refused — never dropped, never stopping the catalogue —, and
     * says why until a revision that is taken.
     */
    public void judge(Optional<String> refusal) {
        this.refusal = refusal.orElse(null);
    }

    /** Why the catalogue refuses the head text as its file holds it; empty for a text it takes. */
    public Optional<String> refusal() {
        return Optional.ofNullable(refusal);
    }

    /** Its address: the one a text names it by and a route reaches it by. */
    public String getId() {
        return slug;
    }

    public String getSlug() {
        return slug;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getCode() {
        return code;
    }

    /** The word its head opens with: {@code component}, {@code circuit}, {@code footprint}. */
    public String getKind() {
        return kind;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public Holding getHolding() {
        return holding;
    }

    public String getShippedLibrary() {
        return shippedLibrary;
    }

    public String getShippedPath() {
        return shippedPath;
    }

    /** The folder a shipped document's file stands in under what ships it; null for one of the library folder, or at its root. */
    public String getFolder() {
        if (shippedPath == null) {
            return null;
        }
        int last = shippedPath.lastIndexOf('/');
        return last <= 0 ? null : shippedPath.substring(0, last);
    }

    /** The head revision of the text: what {@code slug@revision} names. */
    public int getRevision() {
        return revision;
    }

    /** The head revision as a person says it: {@code 20261023_001}. */
    public String getRevisionLabel() {
        return revisionLabel;
    }

    public String getSourceChecksum() {
        return sourceChecksum;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public boolean isFeatured() {
        return featured;
    }

    /** Who the document belongs to; empty for the shared catalogue. */
    public Optional<DocumentOwner> owner() {
        return ownerType == null ? Optional.empty() : Optional.of(DocumentOwner.of(ownerType, ownerId));
    }

    /** Whether the document is the shared catalogue's rather than an owner's. */
    public boolean isCatalogue() {
        return ownerType == null;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    /** What a write is checked against: the head revision it was read at. */
    public long getVersion() {
        return revision;
    }

    public List<Relation> getRelations() {
        return relations;
    }

    public Set<String> getTags() {
        return tags;
    }

    /**
     * One document the head names, and how: the base it extends, the symbol it is drawn with, a part it places.
     *
     * @param relatedDocumentId the document named, by its address
     * @param kind              the language's word for how it is named: {@code use}, {@code symbol}, {@code part}
     * @param position          its place among the head's relations
     */
    public record Relation(String relatedDocumentId, String kind, int position) {
    }
}
