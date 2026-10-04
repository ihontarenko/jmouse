package org.jmouse.script.jpa;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One rule as it is stored: its text, whether it runs, and where.
 *
 * <h2>⚠️ IT CARRIES ITS OWN IDENTITY RATHER THAN EXTENDING A BASE ENTITY</h2>
 *
 * <p>Where this came from it extended the product's own {@code BaseEntity}. A library entity cannot: a
 * product's base class is its conventions — its id type, its timestamp type, its auditing — and
 * inheriting from one would mean a second product had to adopt the first's. Four fields repeated here
 * is the cheaper half of that trade.</p>
 *
 * <h2>⚠️ THE SOURCE IS {@code TEXT}, SPELLED OUT — NOT {@code @Lob} AND NOT {@code MEDIUMTEXT}</h2>
 *
 * <p>It was declared {@code columnDefinition = "MEDIUMTEXT"}, which is MySQL and nothing else, so a
 * library naming it would not start on PostgreSQL. <b>{@code @Lob} is not the fix, and this file said it
 * was.</b> Hibernate maps a {@code @Lob String} to {@code CLOB}, which MySQL's dialect renders as
 * {@code tinytext} — 255 bytes — and {@code ddl-auto: validate} then refuses to start against the long
 * column the migration actually created.</p>
 *
 * <p>{@code TEXT} is spelled the same in MySQL and PostgreSQL, so naming it is dialect-neutral as well as
 * correct. This is the conclusion {@code AccessPolicyRevision} reached first, in a javadoc that says
 * exactly this; the cost of not reading it was a boot failure waiting on one engine.</p>
 *
 * <h2>⚠️ THE TIMESTAMPS ARE {@code LocalDateTime}, WHICH IS THE SAME KIND OF DECISION</h2>
 *
 * <p>{@code Instant} is the better type and the wrong one here. Hibernate maps it through
 * {@code TIMESTAMP_UTC} — {@code datetime(6)} on MySQL, {@code timestamp with time zone} on PostgreSQL —
 * so an entity declaring it validates against a {@code TIMESTAMP(6)} column on one engine and refuses to
 * start on the other. A fault that appears on exactly one of two supported databases is worse than one
 * that appears on both, and every entity in {@code jmouse-files-jpa} and {@code jmouse-storage-jpa}
 * already settled this the same way.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Entity
@Table(name = "script_documents")
public class ScriptDocument {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id = UUID.randomUUID().toString();

    /** ⚠️ What every assignment and every log line names this rule by. Unique across the installation. */
    /**
     * Which tenant this rule belongs to, or {@code null} for the installation as a whole.
     *
     * <h2>⚠️ NULLABLE, AND NULL IS NOT "UNSET"</h2>
     *
     * <p>It means <em>the installation's own rule</em> — answered for every tenant, ahead of that
     * tenant's own. That is also what every row written before this column existed means, which is why
     * the migration adds it nullable and backfills nothing: an installation that already had rules
     * keeps all of them running exactly as they were.
     *
     * <p>⚠️ <strong>The library does not know what it identifies.</strong> A workspace in Innoventa, a
     * household elsewhere. Nothing here compares it to anything but another one of itself.
     */
    @Column(length = 64)
    private String scope;

    /**
     * ⚠️ <strong>Unique per scope, not globally</strong> — and the uniqueness moved when the scope
     * arrived. Two tenants naming a rule <em>low-stock</em> is the ordinary case, not a collision, and
     * a global unique index would have made the second tenant's rule impossible to create with no
     * sentence anybody could act on. The constraint lives in the migration; see it for how null sorts.
     */
    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 512)
    private String description;

    /** The jMS text, exactly as its author wrote it. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String source;

    /**
     * ⚠️ A rule switched off is kept and shown, not deleted.
     *
     * <p>Somebody switching a rule off is usually finding out whether it was the cause of something. A
     * mechanism whose only "off" is delete makes that experiment expensive enough not to run.</p>
     */
    @Column(nullable = false)
    private boolean enabled = true;

    /** Where it sits among the rules at the same stage — see {@link ScriptAssignment}. */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "bind_state", nullable = false, length = 16)
    private ScriptBindState bindState = ScriptBindState.BOUND;

    /**
     * The host's own sentence about why it did not bind.
     *
     * <p>⚠️ Stored rather than recomputed: a rule usually stops binding because the build changed under
     * it, and the message from the build that refused it is the one that says what changed.</p>
     */
    @Column(name = "bind_problem", length = 2000)
    private String bindProblem;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ScriptAssignment> assignments = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected ScriptDocument() {
    }

    public ScriptDocument(String name, String source) {
        this.name = name;
        this.source = source;
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

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public ScriptBindState getBindState() {
        return bindState;
    }

    public void setBindState(ScriptBindState bindState) {
        this.bindState = bindState;
    }

    public String getBindProblem() {
        return bindProblem;
    }

    public void setBindProblem(String bindProblem) {
        this.bindProblem = bindProblem;
    }

    public List<ScriptAssignment> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<ScriptAssignment> assignments) {
        this.assignments = assignments;
    }

    /** The stage names this rule is assigned to, in the order they were given. */
    public List<String> stages() {
        return assignments.stream().map(ScriptAssignment::getStage).toList();
    }

    /**
     * Puts this rule at a moment.
     *
     * @param stage the stage name
     * @return whether anything changed — already assigned is not an error
     */
    public boolean assignTo(String stage) {
        if (stages().contains(stage)) {
            return false;
        }

        assignments.add(new ScriptAssignment(this, stage, assignments.size()));

        return true;
    }

    /**
     * Takes this rule off a moment.
     *
     * <p>⚠️ Removed rather than flagged, and the cascade deletes the row: an assignment has no life of
     * its own, and a "disabled assignment" would be a second off-switch beside {@link #isEnabled()}
     * with no screen able to explain the difference.</p>
     *
     * @param stage the stage name
     * @return whether anything changed
     */
    public boolean unassignFrom(String stage) {
        return assignments.removeIf(assignment -> assignment.getStage().equals(stage));
    }

    /** Records that the host accepted this document. */
    public void bound() {
        this.bindState = ScriptBindState.BOUND;
        this.bindProblem = null;
    }

    /**
     * Records that the host refused it, and why.
     *
     * <p>⚠️ The document is kept and the reason is kept with it. See {@link ScriptBindState}.</p>
     *
     * @param problem the host's own sentence
     */
    public void refused(String problem) {
        this.bindState = ScriptBindState.REFUSED;
        this.bindProblem = problem;
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
