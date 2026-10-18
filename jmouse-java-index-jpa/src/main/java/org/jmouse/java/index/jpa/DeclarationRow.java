package org.jmouse.java.index.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Library-owned persistence row. Access and transaction boundaries belong to the caller. */
@Entity
@Table(name = "java_index_declarations")
public class DeclarationRow {
    @Id
    @Column(name = "id", nullable = false, length = 64)
    String id;

    @Column(name = "contribution_id", nullable = false, length = 64)
    String contributionId;

    @Column(name = "declaration_number", nullable = false)
    int number;

    @Column(name = "symbol_id", nullable = false, length = 4096)
    String symbolId;

    @Column(name = "qualified_name", nullable = false, length = 4096)
    String qualifiedName;

    @Column(name = "name_hash", nullable = false, length = 64)
    String nameHash;

    @Column(name = "kind", nullable = false, length = 64)
    String kind;

    @Column(name = "is_type", nullable = false)
    boolean type;

    @Column(name = "start_offset", nullable = false)
    int startOffset;

    @Column(name = "start_line", nullable = false)
    int startLine;

    @Column(name = "start_column", nullable = false)
    int startColumn;

    @Column(name = "end_offset", nullable = false)
    int endOffset;

    @Column(name = "end_line", nullable = false)
    int endLine;

    @Column(name = "end_column", nullable = false)
    int endColumn;

    protected DeclarationRow() {}
}
