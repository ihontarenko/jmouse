package org.jmouse.query.store.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.jmouse.query.store.AuthoredSource;
import org.jmouse.query.store.QueryOwner;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 📐 An authored declaration as a row — the {@code structure} and {@code mapping} for one source.
 *
 * <h2>⚠️ One body column holding both halves</h2>
 *
 * <p>They are one document. A {@code structure} column beside a {@code mapping} column could hold a
 * pair in which the mapping binds an attribute the structure never declared — which the language
 * refuses at load time, so the row would be unloadable and the screen that wrote it would have had no
 * way to know: neither half is wrong on its own. Kept whole, a save either parses or never becomes a
 * row.</p>
 *
 * <h2>⚠️ Only AUTHORED sources are here, and the absence of a row is meaningful</h2>
 *
 * <p>No row means <em>nobody has written one</em>, and the source falls back to whatever the product
 * built in code. It never means the source does not exist — which is why removing a declaration is not
 * the destructive act it looks like: it restores the one that ships with the product.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Entity
@Table(name = "query_sources")
public class QuerySourceRow {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    /**
     * Which source this declares.
     *
     * <p>⚠️ Not updatable, and not a foreign key. A declaration moved to another source is a different
     * declaration, and this library has no catalogue of sources to point at.</p>
     */
    @Column(name = "source_key", length = 64, nullable = false, updatable = false)
    private String sourceKey;

    @Embedded
    private OwnerColumns owner;

    @Column(name = "body", nullable = false, length = AuthoredSource.MAXIMUM_BODY_LENGTH)
    private String body;

    @Column(name = "author", length = QueryOwner.MAXIMUM_LENGTH, nullable = false)
    private String author;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected QuerySourceRow() {
    }

    QuerySourceRow(String id, String sourceKey, QueryOwner owner) {
        this.id        = id;
        this.sourceKey = sourceKey;
        this.owner     = OwnerColumns.of(owner);
    }

    void write(String body, String author) {
        this.body   = body;
        this.author = author == null || author.isBlank() ? QueryOwner.INSTALLATION_KEY : author;
    }

    /**
     * ⚠️ {@code UTC}, because {@link AuthoredSource} carries an {@link java.time.Instant} and the column
     * carries a {@link LocalDateTime}. Reading it back in the server's default zone would move the stamp
     * by however many hours that zone is from UTC, twice a year by a different amount.
     */
    AuthoredSource toReference() {
        return new AuthoredSource(
                sourceKey, owner.toReference(), body, author, updatedAt.toInstant(ZoneOffset.UTC));
    }

    @PrePersist
    void stampCreation() {
        createdAt = LocalDateTime.now(ZoneOffset.UTC);
        updatedAt = createdAt;
    }

    @PreUpdate
    void stampUpdate() {
        updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }
}
