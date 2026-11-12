package org.jmouse.cad.folder;

/**
 * 🧾 Where a revision of a document's text came from: what decides whether an import may write over it. An import's own document is
 * refreshed by the next import as long as nobody has written it since; a person's revision is theirs and is kept.
 */
public enum RevisionOrigin {

    /** Written by a person, through an editor or the interface. */
    WRITTEN,

    /** Made by an import or fetched from another program's library: the engine's, not a person's. */
    IMPORTED,

    /** Kept before revisions said where they came from: judged as before, a first revision taken as the engine's. */
    UNRECORDED;

    /** Whether a document whose head is this revision, at this number, is still as an import left it. */
    public boolean leftByAnImport(int revision) {
        return this == IMPORTED || (this == UNRECORDED && revision == 1);
    }
}
