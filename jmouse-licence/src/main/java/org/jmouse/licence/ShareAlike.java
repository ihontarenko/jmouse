package org.jmouse.licence;

/** How far a work's licence reaches into what is made with it. */
public enum ShareAlike implements TermWord {

    /** Not at all: what is made with it is its maker's to license. */
    NONE("none"),

    /** The work itself keeps its licence, changed or not; what it is built into does not take it. */
    WEAK("weak"),

    /** What the work is built into, or adapted to, takes its licence too. */
    STRONG("strong");

    private final String word;

    ShareAlike(String word) {
        this.word = word;
    }

    @Override
    public String word() {
        return word;
    }
}
