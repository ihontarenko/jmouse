package org.jmouse.java.index.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Library-owned persistence row. Access and transaction boundaries belong to the caller. */
@Entity
@Table(name = "java_index_references")
public class ReferenceRow {
    @Id
    @Column(name = "id", nullable = false, length = 64)
    String id;

    @Column(name = "contribution_id", nullable = false, length = 64)
    String contributionId;

    @Column(name = "reference_number", nullable = false)
    int number;

    @Column(name = "role", nullable = false, length = 64)
    String role;

    @Column(name = "origin", nullable = false, length = 32)
    String origin;

    @Column(name = "written_target", nullable = false, length = 8192)
    String target;

    @Column(name = "target_hash", nullable = false, length = 64)
    String targetHash;

    @Column(name = "owner_symbol", nullable = true, length = 4096)
    String owner;

    @Column(name = "is_reliable", nullable = false)
    boolean reliable;

    @Column(name = "start_offset", nullable = false)
    int startOffset;

    @Column(name = "end_offset", nullable = false)
    int endOffset;

    protected ReferenceRow() {}
}
