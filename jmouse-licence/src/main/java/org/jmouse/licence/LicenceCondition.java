package org.jmouse.licence;

/** A condition a licence puts on a use, which an exception may waive. */
public enum LicenceCondition implements TermWord {

    /** The credit travels with the work. */
    ATTRIBUTION("attribution"),

    /** The licence travels with the work, and, as strongly as the licence says, with what it is built into. */
    SHARE_ALIKE("share-alike");

    private final String word;

    LicenceCondition(String word) {
        this.word = word;
    }

    @Override
    public String word() {
        return word;
    }
}
