package org.jmouse.cad.document;

/**
 * What a library object is, as its technical code begins: {@code FP.SOIC:8-P1.27-W3.9} is a footprint,
 * {@code PT.SOIC:8-P1.27-W3.9} the same package's portrait. The part after the class — the designation — is the
 * package's or the part's own, so one table's {@code designation} template names a footprint and a portrait alike.
 */
public enum TechnicalClass {

    FOOTPRINT("FP"),
    FRAME("FR"),
    PORTRAIT("PT"),
    SYMBOL("SY");

    private static final String SEPARATOR = ".";

    private final String prefix;

    TechnicalClass(String prefix) {
        this.prefix = prefix;
    }

    /** The code of an object of this class with the given designation: {@code FP} and {@code SOIC:8} → {@code FP.SOIC:8}. */
    public String code(String designation) {
        return prefix + SEPARATOR + designation;
    }
}
