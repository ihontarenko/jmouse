package org.jmouse.licence;

/** Whether a work may be passed on at all. */
public enum Redistribution implements TermWord {

    /** Passed on under its terms. */
    PERMITTED("permitted"),

    /** Passed on only built into the work it came with; never as itself: a work whose terms are not known. */
    EMBEDDED_ONLY("embedded-only"),

    /** Not passed on. */
    FORBIDDEN("forbidden");

    private final String word;

    Redistribution(String word) {
        this.word = word;
    }

    @Override
    public String word() {
        return word;
    }
}
