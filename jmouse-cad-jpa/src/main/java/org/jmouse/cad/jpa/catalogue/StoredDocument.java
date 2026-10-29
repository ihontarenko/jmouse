package org.jmouse.cad.jpa.catalogue;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jmouse.revisions.RevisionLabel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 📇 The index of a document of the catalogue or of an owner's: what lists, searches and authorization need without
 * reading it. The document itself is its jM text, every revision of it in {@code cad_document_revisions}; every column
 * here except identity, owner and status is derived from the head revision when it is saved, and never edited on its
 * own. A document with no owner is the shared catalogue's; an owned one belongs to whatever the host names.
 *
 * <p>{@link #getKind() Its kind} and {@link Relation#getKind() a relation's} are the words the language writes them
 * with — {@code component}, {@code footprint} —, so a kind a new module brings needs no change here.</p>
 */
@Entity(name = "StoredDocument")
@Table(name = "cad_documents")
public class StoredDocument {

    public enum Status { DRAFT, PUBLISHED, ARCHIVED }

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, length = 120, updatable = false)
    private String slug;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(length = 500)
    private String summary;

    @Column(nullable = false, length = 40)
    private String kind;

    /** The section it is filed in; null for one filed on no shelf — a family, a function library. */
    @Column(name = "category_code", length = 40)
    private String categoryCode;

    /** The folder a shipped document's file stands in under the library's root; null for one somebody wrote. */
    @Column(length = 200)
    private String folder;

    @Column(nullable = false)
    private int revision;

    /** The head revision's label — {@code 20261023_001} —, indexed with it so a list says it without reading texts. */
    @Column(name = "revision_label", nullable = false, length = 16)
    private String revisionLabel;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "source_checksum", nullable = false, length = 64)
    private String sourceChecksum;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "shipped_checksum", length = 64)
    private String shippedChecksum;

    @Column(name = "shipped_update_pending", nullable = false)
    private boolean shippedUpdatePending;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    @Column(nullable = false)
    private boolean featured;

    @Column(name = "owner_type", length = 64, updatable = false)
    private String ownerType;

    @Column(name = "owner_id", length = 64, updatable = false)
    private String ownerId;

    @Column(name = "created_by", length = 36, updatable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Version
    private long version;

    @ElementCollection
    @CollectionTable(name = "cad_document_relations", joinColumns = @JoinColumn(name = "document_id"))
    @OrderBy("position")
    private List<Relation> relations = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "cad_document_tags", joinColumns = @JoinColumn(name = "document_id"))
    @Column(name = "tag", length = 64)
    private Set<String> tags = new LinkedHashSet<>();

    protected StoredDocument() {
    }

    /**
     * @param owner     null for the shared catalogue
     * @param createdBy the actor who wrote it; null for the shipped library
     */
    public StoredDocument(String id, String slug, DocumentOwner owner, String createdBy, Instant now) {
        this.id = id;
        this.slug = slug;
        this.ownerType = owner == null ? null : owner.type();
        this.ownerId = owner == null ? null : owner.id();
        this.createdBy = createdBy;
        this.status = Status.DRAFT;
        this.revision = 0;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Moves the document to a new head revision and takes the index columns from what that revision describes.
     *
     * @param label    the name the new head is said by, written with its text
     * @param checksum the new head text's checksum
     * @param kind     the word its head opens with
     */
    public void indexRevision(int revision, RevisionLabel label, String checksum, String kind, String title,
                              String summary, String categoryCode, boolean featured, Instant now) {
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

    /**
     * Files a shipped document where its file stands — on every import, as the library's files are moved —; derived
     * from the file, so it is no change of the document's and leaves when it was updated alone.
     */
    public void fileUnder(String folder) {
        this.folder = folder;
    }

    /** The shipped library's file was taken as it is: nothing newer is waiting. */
    public void recordShippedText(String checksum) {
        this.shippedChecksum = checksum;
        this.shippedUpdatePending = false;
    }

    /** The shipped file has a newer text, kept back because a curator has edited the document since. */
    public void recordShippedUpdateKept(String checksum) {
        this.shippedChecksum = checksum;
        this.shippedUpdatePending = true;
    }

    public void replaceRelations(List<Relation> relations) {
        this.relations.clear();
        this.relations.addAll(relations);
    }

    public void replaceTags(Set<String> tags) {
        this.tags.clear();
        this.tags.addAll(tags);
    }

    public void changeStatus(Status status, Instant now) {
        if (status == Status.PUBLISHED && this.publishedAt == null) {
            this.publishedAt = now;
        }
        this.status = status;
        this.updatedAt = now;
    }

    public String getId() {
        return id;
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

    /** The word its head opens with: {@code component}, {@code circuit}, {@code footprint}. */
    public String getKind() {
        return kind;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public String getFolder() {
        return folder;
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

    public String getShippedChecksum() {
        return shippedChecksum;
    }

    public boolean isShippedUpdatePending() {
        return shippedUpdatePending;
    }

    public Status getStatus() {
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

    public long getVersion() {
        return version;
    }

    public List<Relation> getRelations() {
        return relations;
    }

    public Set<String> getTags() {
        return tags;
    }

    /** One document the head names, and how: the base it extends, the symbol it is drawn with, a part it places. */
    @Embeddable
    public static class Relation {

        @Column(name = "related_document_id", nullable = false, length = 36)
        private String relatedDocumentId;

        @Column(name = "relation_kind", nullable = false, length = 40)
        private String kind;

        @Column(nullable = false)
        private int position;

        protected Relation() {
        }

        public Relation(String relatedDocumentId, String kind, int position) {
            this.relatedDocumentId = relatedDocumentId;
            this.kind = kind;
            this.position = position;
        }

        public String getRelatedDocumentId() {
            return relatedDocumentId;
        }

        /** The language's word for how the document names it: {@code use}, {@code symbol}, {@code part}. */
        public String getKind() {
            return kind;
        }

        public int getPosition() {
            return position;
        }
    }
}
