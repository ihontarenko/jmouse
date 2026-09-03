package org.jmouse.validator.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * One {@code .jmv} document, kept. 📄
 *
 * <h2>⚠️ The source, not a parsed form of it</h2>
 *
 * <p>What is stored is the text somebody wrote — comments, blank lines and all. Storing a parsed tree
 * instead would make this table the second implementation of the language: it would have to be
 * migrated whenever the grammar grew, and a document written by one version and read by another would
 * mean two different things. The text is the record; the parser is a reader of it.</p>
 *
 * <h2>⚠️ Addressed by NAME, the way the language addresses it</h2>
 *
 * <p>{@code validation "innoventa/part" { … }} is the identity, and the loader resolves by it. Keeping
 * that as the key is what lets a document be renamed on disk, moved between products, or shared by two
 * subjects without a migration.</p>
 *
 * <h2>⚠️ No owner column, deliberately</h2>
 *
 * <p>This library has no people table and no workspace table, and must not acquire one — every
 * product's idea of an owner is different, and a nullable foreign key to a table that may not exist is
 * how a library stops being usable outside the product it was written in. A product that needs
 * documents scoped keeps that in its own schema and points here.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Entity
@Table(name = "validation_documents")
public class ValidationDocument {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "name", length = 255, nullable = false, unique = true)
    private String name;

    @Column(name = "source", nullable = false, columnDefinition = "TEXT")
    private String source;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ValidationDocument() {
    }

    /**
     * A new document.
     *
     * @param name   what it calls itself
     * @param source the {@code .jmv}, as written
     * @param now    when it is being written
     */
    public ValidationDocument(String name, String source, Instant now) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.source = source;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Replaces what the document says.
     *
     * <p>⚠️ The name is not changed here. A document is addressed by it, so renaming one is somebody
     * pointing an address at different rules — a decision, not an edit, and one this method would make
     * invisible.</p>
     *
     * @param source the new text
     * @param now    when it is being written
     */
    public void rewrite(String source, Instant now) {
        this.source = source;
        this.updatedAt = now;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSource() {
        return source;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "validation document \"" + name + "\"";
    }
}
