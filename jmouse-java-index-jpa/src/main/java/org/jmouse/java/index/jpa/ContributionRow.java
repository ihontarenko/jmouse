package org.jmouse.java.index.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Library-owned persistence row. Access and transaction boundaries belong to the caller. */
@Entity
@Table(name = "java_index_contributions")
public class ContributionRow {
    @Id
    @Column(name = "id", nullable = false, length = 64)
    String id;

    @Column(name = "content_hash", nullable = false, length = 64)
    String contentHash;

    @Column(name = "configuration_key", nullable = false, length = 256)
    String configuration;

    @Column(name = "payload", nullable = false, length = 134217728)
    byte[] payload;

    @Column(name = "catalog_payload", nullable = false, length = 134217728)
    byte[] catalog;

    protected ContributionRow() {}
}
