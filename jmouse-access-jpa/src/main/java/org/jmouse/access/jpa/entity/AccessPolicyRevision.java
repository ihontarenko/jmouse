package org.jmouse.access.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One saved version of the editable policy document — append-only, read from its newest end.
 *
 * <h2>⚠️ The source is kept whole, not a diff</h2>
 *
 * <p>A diff chain is smaller and cannot answer <em>"show me exactly what was in force in March"</em>
 * without replaying every step, which is the one question a revision history exists for. The diff is
 * computed when somebody asks to see one.
 *
 * <h2>⚠️ The author is an identifier and a label, and both are kept</h2>
 *
 * <p>The account may be deleted and the history must still say who did this. A foreign key could not
 * promise that even before library tables stopped having them, and a label copied at the time is the
 * only thing that survives the account going.
 */
@Entity
@Table(name = "access_policy_revisions")
public class AccessPolicyRevision {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    /**
     * Dense and increasing, assigned by whoever saves.
     *
     * <p>A number somebody can say out loud — <em>"we broke it in 14"</em> — is worth more than a
     * timestamp nobody can.
     */
    @Column(name = "version", nullable = false, unique = true)
    private int version;

    /**
     * ⚠️ {@code TEXT} spelled out rather than {@code @Lob}, and it is not a style choice.
     *
     * <p>Hibernate maps a {@code @Lob String} to {@code CLOB}, which MySQL's dialect renders as
     * {@code tinytext} — 255 bytes, and {@code ddl-auto: validate} refuses to start against the
     * {@code TEXT} column the migration creates. {@code TEXT} is spelled the same in MySQL and
     * PostgreSQL, so naming it is dialect-neutral as well as correct.
     */
    @Column(name = "source", nullable = false, columnDefinition = "TEXT")
    private String source;

    @Column(name = "note", length = 512)
    private String note;

    @Column(name = "author_id", length = 36)
    private String authorId;

    @Column(name = "author_label", length = 255)
    private String authorLabel;

    /** Which version this one restores, where it restores one rather than being written fresh. */
    @Column(name = "reverted_from")
    private Integer revertedFrom;

    @Column(name = "roles", nullable = false)
    private int roles;

    @Column(name = "subjects", nullable = false)
    private int subjects;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AccessPolicyRevision() {
    }

    public AccessPolicyRevision(String id, int version, String source, String note,
                                String authorId, String authorLabel, Integer revertedFrom,
                                int roles, int subjects) {

        this.id           = id;
        this.version      = version;
        this.source       = source;
        this.note         = note;
        this.authorId     = authorId;
        this.authorLabel  = authorLabel;
        this.revertedFrom = revertedFrom;
        this.roles        = roles;
        this.subjects     = subjects;
        this.createdAt    = Instant.now();
    }

    public String getId()           { return id; }
    public int getVersion()         { return version; }
    public String getSource()       { return source; }
    public String getNote()         { return note; }
    public String getAuthorId()     { return authorId; }
    public String getAuthorLabel()  { return authorLabel; }
    public Integer getRevertedFrom(){ return revertedFrom; }
    public int getRoles()           { return roles; }
    public int getSubjects()        { return subjects; }
    public Instant getCreatedAt()   { return createdAt; }
}
