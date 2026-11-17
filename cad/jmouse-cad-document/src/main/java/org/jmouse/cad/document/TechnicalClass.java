package org.jmouse.cad.document;

import java.util.Locale;
import java.util.Optional;

/**
 * What a library object is, as its technical code ends: {@code SOIC:8-P1.27-W3.9:F} is a footprint, {@code SOIC:8-P1.27-W3.9:P}
 * the same package's portrait, {@code SOIC:8-P1.27-W3.9:M3D} its 3D model. A code is written {@code FAMILY:…:NAME:KIND}, all in
 * capitals: the designation — the package's or the part's own, its family first — then the kind, one letter where one is free, else
 * three. One table's {@code designation} template names a footprint, its portrait and its model alike.
 */
public enum TechnicalClass {

    FOOTPRINT("F"),
    FRAME("FRM"),
    MODEL("M3D"),
    PORTRAIT("P"),
    SYMBOL("S"),
    FAMILY("FML"),
    PCB("PCB");

    /** What parts a code: the family from the name, the designation from the kind. */
    private static final String SEPARATOR = ":";

    private final String word;

    TechnicalClass(String word) {
        this.word = word;
    }

    /** The word a code of this class ends in: {@code F} of {@code CHIP:R0805:F}. */
    public String word() {
        return word;
    }

    /** The code of an object of this class with the given designation: {@code SOIC:8} and {@link #FOOTPRINT} → {@code SOIC:8:F}. */
    public String code(String designation) {
        return designation + SEPARATOR + word;
    }

    /**
     * The name a code of this class ends in before its kind, after its family — {@code R0805} of {@code CHIP:R0805:F} —: what another
     * product calls the object without saying which library it is of. Empty for a word that is no code of this class, or one that
     * writes no family.
     */
    public Optional<String> nameIn(String code) {
        if (ofCode(code).filter(this::equals).isEmpty()) {
            return Optional.empty();
        }
        String designation = code.substring(0, code.length() - SEPARATOR.length() - word.length());
        int family = designation.indexOf(SEPARATOR);
        return family < 0 || family == designation.length() - 1 ? Optional.empty() : Optional.of(designation.substring(family + 1));
    }

    /**
     * The class the documents of a kind are coded in — a {@code footprint}'s {@link #FOOTPRINT} —; empty for a kind whose documents
     * are named by their slug alone: a circuit, a component.
     */
    public static Optional<TechnicalClass> ofKind(String kind) {
        for (TechnicalClass candidate : values()) {
            if (candidate.name().toLowerCase(Locale.ROOT).equals(kind)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    /**
     * The class a written word is the code of — {@code CHIP:R0805:F} is a footprint's —; empty for a word that is no code, as every
     * slug is: a slug is lowercase, a code is capitals ending in its kind after a colon, a designation before it.
     */
    public static Optional<TechnicalClass> ofCode(String word) {
        if (word == null || !word.equals(word.toUpperCase(Locale.ROOT))) {
            return Optional.empty();
        }
        for (TechnicalClass candidate : values()) {
            String ending = SEPARATOR + candidate.word;
            if (word.endsWith(ending) && word.length() > ending.length()) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }
}
