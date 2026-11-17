package org.jmouse.cad.simulation;

import org.jspecify.annotations.Nullable;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.DocumentStatus;
import org.jmouse.cad.document.MetaLineNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.el.grammar.Nodes;

import java.util.Optional;

/**
 * A project, as its own document says it: one design of sheets — or its owner's drafts — with a title and a sentence of what it
 * is. What it holds is not written here: a project's documents are those owned by it, or, for a project a library ships, those
 * shipped beside it. Whether it is shipped or kept is where its document is held, never a word of its text; whether it is
 * archived is its status ({@code meta { status archived }}), as every document's is.
 *
 * @param slug    its address — {@code /projects/<slug>} — and the owner its documents are kept under
 * @param title   what it is called
 * @param summary what it is, or null
 * @param purpose a design, or its owner's drafts
 * @param status  published, or archived — kept, listed no more, taking nothing new
 */
public record ProjectDocument(String slug, String title, @Nullable String summary, ProjectPurpose purpose, DocumentStatus status) {

    /** The project a document says it is; refuses a document of another kind. */
    public static ProjectDocument of(DocumentNode document) {
        if (!document.kind().equals(SimulationModule.PROJECT)) {
            throw new IllegalArgumentException("'" + document.slug() + "' is a " + document.kind().word() + ", not a project");
        }
        Optional<MetaNode> meta = document.block(MetaNode.class);
        return new ProjectDocument(document.slug(), document.title(), meta.flatMap(found -> found.text(MetaNode.SUMMARY)).orElse(null),
                ProjectPurpose.written(meta.flatMap(found -> found.text(MetaNode.PURPOSE))),
                DocumentStatus.written(meta.flatMap(found -> found.text(MetaNode.STATUS))));
    }

    /** Whether it is archived: listed no more, and taking nothing new. */
    public boolean archived() {
        return status == DocumentStatus.ARCHIVED;
    }

    /** The same project called and described anew. */
    public ProjectDocument described(String title, String summary) {
        return new ProjectDocument(slug, title, summary, purpose, status);
    }

    /** The same project archived, or brought back. */
    public ProjectDocument archived(boolean archived) {
        return new ProjectDocument(slug, title, summary, purpose, archived ? DocumentStatus.ARCHIVED : DocumentStatus.PUBLISHED);
    }

    /** The project as the tree of its document, which the language writes: what it does not say, it leaves unwritten. */
    public DocumentNode document() {
        DocumentNode document = new DocumentNode(SimulationModule.PROJECT, title, slug, null, 1);
        MetaNode meta = new MetaNode();
        if (summary != null && !summary.isBlank()) {
            meta.addExpression(MetaLineNode.of(MetaNode.SUMMARY, Nodes.text(summary.strip())));
        }
        if (purpose != ProjectPurpose.DESIGN) {
            meta.addExpression(MetaLineNode.of(MetaNode.PURPOSE, Nodes.word(purpose.word())));
        }
        if (status != DocumentStatus.PUBLISHED) {
            meta.addExpression(MetaLineNode.of(MetaNode.STATUS, Nodes.word(status.word())));
        }
        document.addExpression(meta);
        return document;
    }
}
