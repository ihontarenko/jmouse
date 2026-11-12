package org.jmouse.cad.exchange;

import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.plugin.Contributions;
import org.jmouse.plugin.Identified;

import java.util.List;

/**
 * A format whose files read as jM documents: a SPICE netlist, a KiCad design. It only converts — the files it is handed
 * are what the reader chose, it opens nothing itself, and storing what it made is the store's.
 */
public interface DocumentImport extends Identified {

    Contributions<DocumentImport> POINT = new Contributions<>("cad.exchange.imports", DocumentImport.class);

    String title();

    /** The files it reads, as a file chooser filters them: {@code *.cir}, {@code *.kicad_sch}. */
    List<String> fileNames();

    /**
     * Those of {@link #fileNames()} that read on their own, nothing beside them asked for — one footprint of a library —;
     * the others are read with the files beside them a reading may need, a schematic's sheets. None, by default.
     */
    default List<String> fileNamesReadAlone() {
        return List.of();
    }

    /**
     * Those of {@link #fileNames()} that travel as their bytes, in Base64, whatever {@link #encoding()} says of the rest: a
     * text format's meshes and archives. None, by default.
     */
    default List<String> fileNamesAsBytes() {
        return List.of();
    }

    /** How its files travel: as their text, by default; as their bytes, for a format whose files are bytes. */
    default FileEncoding encoding() {
        return FileEncoding.TEXT;
    }

    /**
     * Those of {@link #fileNames()} that are a library — somebody's work, whose text does not say whose: the reader is asked
     * its holder and terms ({@link ExchangeFiles#stated()}), and every document converted from it carries them. A design's
     * files carry the drawings it was made with and claim nothing. None, by default.
     */
    default List<String> libraryFileNames() {
        return List.of();
    }

    /**
     * The files as documents.
     *
     * @param catalogue the components a converted part may name, as the reader may see them
     * @throws ExchangeRefusal          when a file does not read, with where
     * @throws IllegalArgumentException when what it reads cannot be carried into jM, with what
     */
    ImportedDocuments read(ExchangeFiles files, CatalogueDocuments catalogue);
}
