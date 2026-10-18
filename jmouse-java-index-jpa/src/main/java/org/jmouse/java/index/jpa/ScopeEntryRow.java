package org.jmouse.java.index.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Library-owned persistence row. Access and transaction boundaries belong to the caller. */
@Entity
@Table(name = "java_index_scope_entries")
public class ScopeEntryRow {
    @Id
    @Column(name = "id", nullable = false, length = 64)
    String id;

    @Column(name = "scope_id", nullable = false, length = 36)
    String scopeId;

    @Column(name = "source_hash", nullable = false, length = 64)
    String sourceHash;

    @Column(name = "source_key", nullable = false, length = 2048)
    String source;

    @Column(name = "contribution_id", nullable = false, length = 64)
    String contributionId;

    protected ScopeEntryRow() {}
}
