package org.jmouse.cad.document;

import java.util.Locale;
import java.util.Optional;

/**
 * What a library object is, as its technical code begins: {@code FP.SOIC:8-P1.27-W3.9} is a footprint,
 * {@code PT.SOIC:8-P1.27-W3.9} the same package's portrait. The part after the class — the designation — is the
 * package's or the part's own, so one table's {@code designation} template names a footprint, its portrait and its
 * 3D model ({@code MD}) alike.
 */
public enum TechnicalClass {

    FOOTPRINT("FP"),
    FRAME("FR"),
    MODEL("MD"),
    PORTRAIT("PT"),
    SYMBOL("SY");

    private static final String SEPARATOR = ".";

    /** What ends a code's namespace, before the name: {@code CHIP} and {@code R0805} of {@code FP.CHIP:R0805}. */
    private static final String NAMESPACE_SEPARATOR = ":";

    private final String prefix;

    TechnicalClass(String prefix) {
        this.prefix = prefix;
    }

    /** The code of an object of this class with the given designation: {@code FP} and {@code SOIC:8} → {@code FP.SOIC:8}. */
    public String code(String designation) {
        return prefix + SEPARATOR + designation;
    }

    /**
     * The name a code of this class ends in, after its namespace — {@code R0805} of {@code FP.CHIP:R0805} —: what another
     * product calls the object without saying which library it is of. Empty for a word that is no code of this class, or one
     * that writes no namespace.
     */
    public Optional<String> nameIn(String code) {
        if (ofCode(code).filter(this::equals).isEmpty()) {
            return Optional.empty();
        }
        int separator = code.indexOf(NAMESPACE_SEPARATOR);
        return separator < 0 || separator == code.length() - 1 ? Optional.empty() : Optional.of(code.substring(separator + 1));
    }

    /**
     * The class the documents of a kind are coded in — a {@code footprint}'s {@link #FOOTPRINT} —; empty for a kind whose
     * documents are named by their slug alone: a circuit, a component, a pcb.
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
     * The class a written word is the code of — {@code FP.CHIP:R0805} is a footprint's —; empty for a word that is no
     * code, as every slug is: a slug is lowercase, a code opens with its class's capitals and a dot.
     */
    public static Optional<TechnicalClass> ofCode(String word) {
        if (word == null) {
            return Optional.empty();
        }
        for (TechnicalClass candidate : values()) {
            String opening = candidate.prefix + SEPARATOR;
            if (word.startsWith(opening) && word.length() > opening.length()) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }
}
