package org.jmouse.java.index;

import org.jmouse.java.parser.model.ReferenceState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Projects retained Java facts once for SQL stores and other consumers; never resolves names. */
public final class SourceReferences {
    private SourceReferences() {}

    public static List<SourceReference> of(FileIndex file) {
        List<SourceReference> result = new ArrayList<>();
        for (IndexedReference reference : file.references()) {
            result.add(
                    new SourceReference(
                            SourceReference.Role.valueOf(reference.role().name()),
                            SourceReference.Origin.HEADER,
                            reference.name(),
                            reference.owner(),
                            reference.scopeType(),
                            reference.span(),
                            file.isStructureComplete()));
        }
        for (var imported : file.imports()) {
            result.add(
                    new SourceReference(
                            SourceReference.Role.IMPORTS,
                            SourceReference.Origin.IMPORT,
                            imported.name() + (imported.onDemand() ? ".*" : ""),
                            null,
                            null,
                            imported.span(),
                            file.isStructureComplete()));
        }
        for (var documentation : file.javaDocs()) {
            var owner = documentation.owner() == null ? null : documentation.owner().symbolId();
            for (var tag : documentation.inlineTags()) {
                if (tag.reference() != null && !tag.reference().isBlank()) {
                    result.add(
                            new SourceReference(
                                    SourceReference.Role.REFERENCES,
                                    SourceReference.Origin.DOCUMENTATION,
                                    tag.reference(),
                                    owner,
                                    null,
                                    tag.span(),
                                    file.isStructureComplete()));
                }
            }
            for (var tag : documentation.blockTags()) {
                if (tag.referenceState() == ReferenceState.UNRESOLVED
                        && tag.argument() != null
                        && !tag.argument().isBlank()) {
                    result.add(
                            new SourceReference(
                                    SourceReference.Role.REFERENCES,
                                    SourceReference.Origin.DOCUMENTATION,
                                    tag.argument(),
                                    owner,
                                    null,
                                    tag.span(),
                                    file.isStructureComplete()));
                }
            }
        }
        for (var body : file.bodies()) {
            for (var reference : body.references()) {
                result.add(
                        new SourceReference(
                                SourceReference.Role.valueOf(reference.role().name()),
                                SourceReference.Origin.BODY,
                                reference.expression().name(),
                                reference.owner(),
                                reference.scopeType(),
                                reference.span(),
                                file.isBodyComplete(reference.owner())));
            }
        }
        result.sort(Comparator.comparingInt(reference -> reference.span().start().offset()));
        return List.copyOf(result);
    }
}
