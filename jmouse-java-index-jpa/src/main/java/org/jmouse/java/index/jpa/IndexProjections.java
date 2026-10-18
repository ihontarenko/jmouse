package org.jmouse.java.index.jpa;

import jakarta.persistence.EntityManager;

import org.jmouse.java.index.FileIndex;
import org.jmouse.java.index.SourceReferences;

import java.util.ArrayList;
import java.util.List;

/** SQL lookup projections are written in the same transaction as their immutable payload. */
final class IndexProjections {
    private IndexProjections() {}

    static void write(EntityManager entityManager, String contributionId, FileIndex file) {
        List<Object> inserted = new ArrayList<>();
        int number = 0;
        for (var declaration : file.declarations()) {
            DeclarationRow row = new DeclarationRow();
            row.id = IndexIdentities.hash(contributionId + ":declaration:" + number);
            row.contributionId = contributionId;
            row.number = number++;
            row.symbolId = declaration.symbolId().value();
            row.qualifiedName = declaration.qualifiedName();
            row.nameHash = IndexIdentities.hash(row.qualifiedName);
            row.kind = declaration.kind().name();
            row.type = declaration.isType();
            var span = declaration.nameSpan();
            row.startOffset = span.start().offset();
            row.startLine = span.start().line();
            row.startColumn = span.start().column();
            row.endOffset = span.end().offset();
            row.endLine = span.end().line();
            row.endColumn = span.end().column();
            entityManager.persist(row);
            inserted.add(row);
        }
        number = 0;
        for (var reference : SourceReferences.of(file)) {
            ReferenceRow row = new ReferenceRow();
            row.id = IndexIdentities.hash(contributionId + ":reference:" + number);
            row.contributionId = contributionId;
            row.number = number++;
            row.role = reference.role().name();
            row.origin = reference.origin().name();
            row.target = reference.target();
            row.targetHash = IndexIdentities.hash(row.target);
            row.owner = reference.owner() == null ? null : reference.owner().value();
            row.reliable = reference.reliable();
            row.startOffset = reference.span().start().offset();
            row.endOffset = reference.span().end().offset();
            entityManager.persist(row);
            inserted.add(row);
        }
        entityManager.flush();
        inserted.forEach(entityManager::detach);
    }
}
