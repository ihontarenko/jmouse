package org.jmouse.licence;

/** What a licence answers for a use, from the least owed to the most: each later one owes all an earlier one does. */
public enum Verdict implements TermWord {

    /** Permitted, nothing owed. */
    FREE("free"),

    /** Permitted, its credit travelling with it. */
    ATTRIBUTED("attributed"),

    /** Permitted only under the work's own licence, its credit travelling with it. */
    SAME_TERMS("same-terms"),

    /** Not permitted. */
    REFUSED("refused");

    private final String word;

    Verdict(String word) {
        this.word = word;
    }

    @Override
    public String word() {
        return word;
    }

    /** Whether the use is permitted at all. */
    public boolean permits() {
        return this != REFUSED;
    }

    /** Whether the work's credit must travel with it. */
    public boolean owesCredit() {
        return this == ATTRIBUTED || this == SAME_TERMS;
    }
}
