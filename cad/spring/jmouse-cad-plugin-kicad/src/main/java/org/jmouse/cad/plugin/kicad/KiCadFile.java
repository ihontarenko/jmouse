package org.jmouse.cad.plugin.kicad;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The files of KiCad a reading starts from, by what they are: a root schematic and the design it places, a project file
 * and the root schematic beside it of the same name, or one footprint of a library. Every name the format offers is one
 * of these, so a file the format says it reads, it reads.
 */
enum KiCadFile {

    SCHEMATIC(".kicad_sch", Reading.WITH_FOLDER, Work.DESIGN),
    PROJECT(".kicad_pro", Reading.WITH_FOLDER, Work.DESIGN),
    PCB(".kicad_pcb", Reading.WITH_FOLDER, Work.DESIGN),
    FOOTPRINT(".kicad_mod", Reading.ALONE, Work.LIBRARY),
    SYMBOL_LIBRARY(".kicad_sym", Reading.ALONE, Work.LIBRARY);

    /** Whether a file of the kind is read with the files beside it — a design's sheets and libraries — or on its own. */
    enum Reading {
        WITH_FOLDER,
        ALONE
    }

    /** Whose a file of the kind is: a design's, carrying the drawings it was made with, or a library, somebody's work. */
    enum Work {
        DESIGN,
        LIBRARY
    }

    private final String  suffix;
    private final Reading reading;
    private final Work    work;

    KiCadFile(String suffix, Reading reading, Work work) {
        this.suffix = suffix;
        this.reading = reading;
        this.work = work;
    }

    String suffix() {
        return suffix;
    }

    /** The name without this kind's suffix: {@code amplifier} of {@code amplifier.kicad_pro}. */
    String stemOf(String name) {
        return name.substring(0, name.length() - suffix.length());
    }

    /** The kind of a file by its name, when it is one a reading starts from. */
    static Optional<KiCadFile> of(String name) {
        String lowered = name.toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(kind -> lowered.endsWith(kind.suffix)).findFirst();
    }

    /** Every kind, as a file chooser filters them: {@code *.kicad_sch}, … */
    static List<String> fileNames() {
        return Arrays.stream(values()).map(kind -> "*" + kind.suffix).toList();
    }

    /** The kinds read on their own: a footprint, which needs none of its library's others. */
    static List<String> fileNamesReadAlone() {
        return Arrays.stream(values()).filter(kind -> kind.reading == Reading.ALONE).map(kind -> "*" + kind.suffix).toList();
    }

    /** The kinds that are a library: whose they are, the reader says. */
    static List<String> libraryFileNames() {
        return Arrays.stream(values()).filter(kind -> kind.work == Work.LIBRARY).map(kind -> "*" + kind.suffix).toList();
    }
}
