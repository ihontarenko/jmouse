package org.jmouse.cad.altium;

import org.jmouse.cad.document.TechnicalClass;
import org.jmouse.cad.exchange.imported.ImportedNames;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * What a document converted from Altium is called — every slug opens with {@code altium-}, as KiCad's with
 * {@code kicad-} — and what it writes as its source: a part by its library reference, after the library it was placed from
 * where the sheet names that.
 */
final class AltiumNames {

    /** What every slug of a document converted from Altium opens with. */
    private static final String ALTIUM = "altium";

    private AltiumNames() {
    }

    /** A design's circuit: {@code 3_mpcie} is {@code altium-3-mpcie}. */
    static String design(String title) {
        return ImportedNames.slug(ALTIUM, title);
    }

    /** {@code LED_0603_GREEN} is {@code altium-led-0603-green}. */
    static String component(String libraryReference) {
        return ImportedNames.slug(ALTIUM, libraryReference);
    }

    static String symbol(String libraryReference) {
        return ImportedNames.slug(ALTIUM, libraryReference, "symbol");
    }

    /**
     * The code a converted symbol is named by, and the component drawn with it names it by: in the namespace of the
     * library it was placed from — {@code Miscellaneous Devices.IntLib}'s {@code Res2} is {@code MISCELLANEOUS-DEVICES:RES2:S} —,
     * in Altium's own where the sheet names none.
     */
    static String symbolCode(Optional<String> library, String libraryReference) {
        return ImportedNames.code(TechnicalClass.SYMBOL, library.map(AltiumNames::withoutFileKind).orElse(ALTIUM), libraryReference);
    }

    /** The code a footprint of a PCB library is named by: {@code rcl.PcbLib}'s {@code R0402} is {@code RCL:R0402:F}. */
    static String footprintCode(String library, String footprint) {
        return ImportedNames.code(TechnicalClass.FOOTPRINT, withoutFileKind(library), footprint);
    }

    /** The code a model a PCB library embeds is named by, in the library's namespace: the file it was, without its kind. */
    static String modelCode(String library, String fileName) {
        return ImportedNames.code(TechnicalClass.MODEL, withoutFileKind(library), withoutFileKind(fileName));
    }

    /** A library's or a file's name without the kind of file it is: {@code rcl.PcbLib} is {@code rcl}. */
    static String withoutFileKind(String file) {
        String name = file.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        return name.lastIndexOf('.') > 0 ? name.substring(0, name.lastIndexOf('.')) : name;
    }

    /** How a converted drawing's source begins, by what it was in Altium. */
    private static final String SYMBOL_SOURCE = "Altium symbol ";
    private static final String COMPONENT_SOURCE = "Altium part ";
    private static final String FOOTPRINT_SOURCE = "Altium footprint ";
    private static final String MODEL_SOURCE = "Altium model ";

    /** What stands between the library a part was placed from and its reference there. */
    private static final char IN_LIBRARY = ':';

    /** What a converted symbol writes as its source: {@code Altium symbol Miscellaneous Devices.IntLib:Res2}, its library where the sheet names one. */
    static String symbolSource(Optional<String> library, String libraryReference) {
        return SYMBOL_SOURCE + placed(library, libraryReference);
    }

    static String componentSource(Optional<String> library, String libraryReference) {
        return COMPONENT_SOURCE + placed(library, libraryReference);
    }

    /** What a footprint converted from a PCB library writes as its source: {@code Altium footprint rcl.PcbLib:R0402}. */
    static String footprintSource(String library, String footprint) {
        return FOOTPRINT_SOURCE + library + IN_LIBRARY + footprint;
    }

    /** What a model a PCB library embeds writes as its source: {@code Altium model rcl.PcbLib:0402 Res.step}. */
    static String modelSource(String library, String fileName) {
        return MODEL_SOURCE + library + IN_LIBRARY + fileName;
    }

    /** The slug a converted footprint is stored under: {@code rcl.PcbLib}'s {@code R0402} is {@code altium-rcl-r0402-footprint}. */
    static String footprint(String library, String footprint) {
        return ImportedNames.slug(ALTIUM, withoutFileKind(library), footprint, "footprint");
    }

    /** The slug an embedded model is stored under. */
    static String model(String library, String fileName) {
        return ImportedNames.slug(ALTIUM, withoutFileKind(library), withoutFileKind(fileName), "model");
    }

    private static String placed(Optional<String> library, String libraryReference) {
        return library.map(name -> name + IN_LIBRARY + libraryReference).orElse(libraryReference);
    }

    /**
     * The Altium library a converted drawing comes from, as its source says it, without the kind of file it is kept in:
     * {@code Miscellaneous Devices.IntLib} is {@code Miscellaneous Devices}. Empty for a source that names none.
     */
    static Optional<String> libraryOf(String source) {
        if (source == null) {
            return Optional.empty();
        }
        return Stream.of(SYMBOL_SOURCE, COMPONENT_SOURCE, FOOTPRINT_SOURCE, MODEL_SOURCE).filter(source::startsWith).findFirst()
                .map(prefix -> source.substring(prefix.length()))
                .filter(written -> written.indexOf(IN_LIBRARY) > 0)
                .map(written -> written.substring(0, written.indexOf(IN_LIBRARY)))
                .map(AltiumNames::withoutFileKind);
    }
}
