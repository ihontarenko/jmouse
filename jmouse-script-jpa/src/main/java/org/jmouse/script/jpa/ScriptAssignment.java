package org.jmouse.script.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One rule, running at one moment.
 *
 * <h2>⚠️ A ROW, RATHER THAN LETTING A DOCUMENT DECIDE FOR ITSELF</h2>
 *
 * <p>The other shape — a document declaring the stages it acts on from inside its own {@code on} and
 * {@code when} clauses, with no screen where it is attached to anything — is right for a product whose
 * moments all cost the same.</p>
 *
 * <p>They usually do not. A per-item stage is asked once per item in a walk of thousands; a
 * post-fetch stage is asked once after a second of network. Without this row, every document would be
 * loaded and asked "do you handle this?" at every moment — cheap per call, and not cheap multiplied by
 * four thousand.</p>
 *
 * <p>⚠️ It is also the answer to the question somebody actually asks about a rule, which is not "what
 * does it handle" but <em>where does it run</em>.</p>
 *
 * <h2>⚠️ THE STAGE IS A NAME, NOT A FOREIGN KEY</h2>
 *
 * <p>Stages are declarations rather than rows — adding one is a class, and there is no table to point
 * at. Which means an assignment can outlive the stage it names, after a rename or a removal. Those
 * should be shown as unknown rather than quietly deleted: somebody who renamed something wants to see
 * where their rule went, not to find it gone.</p>
 *
 * <h2>⚠️ IT CARRIES TIMESTAMPS NOBODY READS, BECAUSE THE COLUMNS ARE {@code NOT NULL}</h2>
 *
 * <p>Nothing shows when an assignment was made. They are here because dropping the product base class
 * this entity used to extend dropped three columns with it, and {@code ddl-auto: validate} does not
 * catch that — it checks that every column the ENTITY declares exists, never that every column the TABLE
 * requires is declared. So the schema passes, the application starts, and the first attempt to place a
 * rule at a moment fails on a {@code NOT NULL} column Hibernate was never told to write.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Entity
@Table(name = "script_assignments")
public class ScriptAssignment {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "script_document_id", nullable = false)
    private ScriptDocument document;

    @Column(nullable = false, length = 64)
    private String stage;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected ScriptAssignment() {
    }

    public ScriptAssignment(ScriptDocument document, String stage, int sortOrder) {
        this.document = document;
        this.stage = stage;
        this.sortOrder = sortOrder;
    }

    @PrePersist
    void onPersist() {
        LocalDateTime now = LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public ScriptDocument getDocument() {
        return document;
    }

    public void setDocument(ScriptDocument document) {
        this.document = document;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }

}
