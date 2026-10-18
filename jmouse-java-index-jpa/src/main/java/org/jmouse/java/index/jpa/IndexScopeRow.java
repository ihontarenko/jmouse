package org.jmouse.java.index.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Library-owned persistence row. Access and transaction boundaries belong to the caller. */
@Entity
@Table(name = "java_index_scopes")
public class IndexScopeRow {
    @Id
    @Column(name = "id", nullable = false, length = 36)
    String id;

    @Column(name = "configuration_key", nullable = false, length = 256)
    String configuration;

    @Column(name = "revision", nullable = false)
    long revision;

    @Column(name = "is_sealed", nullable = false)
    boolean sealed;

    protected IndexScopeRow() {}
}
