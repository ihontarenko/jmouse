package org.jmouse.licence;

/** Whether a work's credit — who holds it, under what licence — must travel with it. */
public enum Attribution implements TermWord {

    REQUIRED("required"),

    NOT_REQUIRED("not-required");

    private final String word;

    Attribution(String word) {
        this.word = word;
    }

    @Override
    public String word() {
        return word;
    }
}
