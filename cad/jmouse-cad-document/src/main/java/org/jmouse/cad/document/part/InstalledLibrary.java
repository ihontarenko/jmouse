package org.jmouse.cad.document.part;

import java.util.List;
import java.util.Set;
import java.util.Optional;

/**
 * 📚 A library a format installs beside the server — KiCad's {@code share/kicad}, a LibrePCB workspace's {@code *.lplib} — as what
 * a format alone knows of it: whether it is there, what it holds, and one of its parts made into documents. How it becomes a place
 * parts are found — read once in the background, its states, its scoring — is {@link InstalledLibraryParts}', the same for every
 * format.
 */
public interface InstalledLibrary {

    /** How it is named in an address: {@code kicad}, {@code libre-pcb}. */
    String id();

    /** How it is named to a reader. */
    String name();

    /** The kinds of part it holds: what it is asked for. */
    Set<PartCandidate.Kind> offers();

    /** Whether any of its folders is there to read. */
    boolean present();

    /** What a reader is told while none is: which setting names its folders, and where the format keeps them. */
    String absence();

    /**
     * Every part it holds, read from its folders: long — the libraries walked whole —, so asked once, in the background.
     *
     * @throws RuntimeException naming what could not be read
     */
    List<IndexedPart> read();

    /**
     * The one licence everything it holds stands under, as an SPDX expression; empty where each of its libraries has its own,
     * which its parts then carry ({@link PartCandidate#license()}).
     */
    Optional<String> license();

    /**
     * One of its parts, made into documents the reader may keep.
     *
     * @param identifier the part's {@link PartCandidate#identifier()}
     * @throws IllegalArgumentException when it holds no such part
     */
    FetchedPart fetch(String identifier);
}
