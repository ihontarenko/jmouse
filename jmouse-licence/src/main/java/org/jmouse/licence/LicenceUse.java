package org.jmouse.licence;

/** What is done with a work taken under a licence: what a ruling is asked for. */
public enum LicenceUse implements TermWord {

    /** Built into another work and passed on in it: a footprint in a board and the board's files, a model in its 3D. */
    EMBEDDED("embedded"),

    /** Passed on as itself: the work's own file handed over, published, shared. */
    REDISTRIBUTED("redistributed"),

    /** Taken into a collection under that collection's own terms, as if it were the collection's. */
    ADOPTED("adopted");

    private final String word;

    LicenceUse(String word) {
        this.word = word;
    }

    @Override
    public String word() {
        return word;
    }
}
