package org.jmouse.cad.document.source;

import org.jmouse.cad.document.DocumentOwner;

/**
 * 📍 Where a copy of a document is held — the database, a jar that ships it, a file in a mounted folder, another
 * product's file read in place. Not where its content was taken from or whose work it is: that is its
 * {@link org.jmouse.cad.document.Provenance}. The engine never sees a location; the source layer and the reader do.
 * Whether a copy can be written follows from where it is held ({@link Access}), never from a flag beside it.
 */
public sealed interface DocumentLocation {

    /** The kind of place, as a reader is told it. */
    enum Kind { STORED, SHIPPED, MOUNTED, FOREIGN }

    /** What a place lets a reader do with what it holds. */
    enum Access { READ_WRITE, READ_ONLY }

    Kind kind();

    /**
     * What the place lets be done with a copy held there — whether a reader may is the catalogue's to decide; a copy in a
     * read-only place is changed by saving a copy elsewhere.
     */
    Access access();

    /**
     * In the database.
     *
     * @param owner    whose it is; null for the shared catalogue's
     * @param id       its stored identifier
     * @param revision the revision held
     */
    record Stored(DocumentOwner owner, String id, int revision) implements DocumentLocation {

        @Override
        public Kind kind() {
            return Kind.STORED;
        }

        @Override
        public Access access() {
            return Access.READ_WRITE;
        }
    }

    /**
     * A resource of a jar that ships it: read only — a change is a fork.
     *
     * @param library what ships it — {@code jmouse-cad-library}, the product's catalogue
     * @param path    its path in the jar — {@code footprints/…/soic-8-footprint.footprint.jm}
     */
    record Shipped(String library, String path) implements DocumentLocation {

        @Override
        public Kind kind() {
            return Kind.SHIPPED;
        }

        @Override
        public Access access() {
            return Access.READ_ONLY;
        }
    }

    /**
     * A file in a folder an explorer mount opens.
     *
     * @param mount the mount's id
     * @param key    the file's path in the mount — {@code amplifier/pcb.pcb.jm}
     * @param access what the mount lets a reader do
     */
    record Mounted(String mount, String key, Access access) implements DocumentLocation {

        @Override
        public Kind kind() {
            return Kind.MOUNTED;
        }

    }

    /**
     * Another product's file, read and converted in place: read only — a change is an import.
     *
     * @param format the exchange format that reads it — {@code kicad}
     * @param file   where the file is, as its source names it
     * @param entry  what in the file this document is — a symbol, a sheet —; null for the file as a whole
     */
    record Foreign(String format, String file, String entry) implements DocumentLocation {

        @Override
        public Kind kind() {
            return Kind.FOREIGN;
        }

        @Override
        public Access access() {
            return Access.READ_ONLY;
        }
    }
}
