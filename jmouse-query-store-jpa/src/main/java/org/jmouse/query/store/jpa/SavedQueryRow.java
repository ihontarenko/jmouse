package org.jmouse.query.store.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.jmouse.query.store.QueryOwner;
import org.jmouse.query.store.SavedQuery;
import org.jmouse.query.store.SavedQueryDraft;

import java.time.LocalDateTime;

/**
 * 💾 A saved query as a row.
 *
 * <h2>⚠️ The body is a sized column, not a large object</h2>
 *
 * <p>A large object is streamed by both engines, cannot be compared or indexed, and comes back through
 * a driver path that several tools render as a handle rather than as text. A saved query is a paragraph
 * somebody typed — read on every listing, searched, and diffed when a shared one changes.</p>
 *
 * <p>⚠️ The column is sized in <em>bytes</em> and the library's cap is in <em>characters</em>, at a
 * quarter of it. That gap is deliberate: without it a query written in Cyrillic would reach the column's
 * limit at a quarter of the length an English one does, and the database's answer to that is to
 * truncate.</p>
 *
 * <h2>⚠️ No scope column, and that is a decision rather than an omission</h2>
 *
 * <p>The obvious shape is {@code PERSONAL | PROJECT | GLOBAL}. It was refused: every product hangs
 * saved queries off something different, so an enum here would mean a release of this library each time
 * one of them found a new thing to hang them off. {@link QueryOwner} is the product's own vocabulary
 * instead, and nothing here reads it.</p>
 *
 * <h2>⚠️ The source is a name, and knows no tables</h2>
 *
 * <p>{@code issues}, {@code inventory} — the name of a described source. What it reaches is resolved by
 * whichever product's engine runs the query. That is the whole of what lets one table serve every
 * product: two installations differ in what their sources mean, never in the rows.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Entity
@Table(name = "saved_queries")
public class SavedQueryRow implements SavedQuery {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    /**
     * Which described source the jMQ is written against.
     *
     * <p>⚠️ Not updatable: a query moved to another source is a different query wearing the same name,
     * and everything pointing at it by identifier would follow the move without being asked.</p>
     */
    @Column(name = "source_key", length = 64, nullable = false, updatable = false)
    private String source;

    @Embedded
    private OwnerColumns owner;

    @Column(name = "name", length = SavedQueryDraft.MAXIMUM_NAME_LENGTH, nullable = false)
    private String name;

    @Column(name = "description", length = SavedQueryDraft.MAXIMUM_DESCRIPTION_LENGTH)
    private String description;

    @Column(name = "body", length = SavedQueryDraft.BODY_COLUMN_LENGTH, nullable = false)
    private String body;

    /**
     * Whoever wrote it, as the product's own identifier for a person.
     *
     * <p>⚠️ A string rather than an association, because this library has no people table and must not
     * acquire one. Every product already has its own, and a foreign key into one of them from here would
     * make adopting this library a schema negotiation instead of a dependency.</p>
     */
    @Column(name = "author", length = 64, nullable = false)
    private String author;

    @Column(name = "is_shared", nullable = false)
    private boolean shared;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected SavedQueryRow() {
    }

    SavedQueryRow(String identifier, SavedQueryDraft draft) {
        this.id     = identifier;
        this.source = draft.source();
        this.owner  = OwnerColumns.of(draft.owner());

        rewriteWith(draft);
    }

    /**
     * ✏️ Take on what a draft says, leaving identity, source and owner where they are.
     *
     * @param draft what this query should now say
     */
    void rewriteWith(SavedQueryDraft draft) {
        this.name        = draft.name();
        this.description = draft.description();
        this.body        = draft.body();
        this.author      = draft.author();
        this.shared      = draft.shared();
        this.sortOrder   = draft.sortOrder();
    }

    @PrePersist
    void stampCreation() {
        LocalDateTime moment = LocalDateTime.now();

        this.createdAt = moment;
        this.updatedAt = moment;
    }

    @PreUpdate
    void stampChange() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String getIdentifier() {
        return id;
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public QueryOwner getOwner() {
        return owner.toReference();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getBody() {
        return body;
    }

    @Override
    public String getAuthor() {
        return author;
    }

    @Override
    public boolean isShared() {
        return shared;
    }

    @Override
    public int getSortOrder() {
        return sortOrder;
    }

    @Override
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "%s '%s' on %s".formatted(id, name, source);
    }
}
