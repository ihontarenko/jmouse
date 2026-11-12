package org.jmouse.cad.document;

import java.util.Locale;

/**
 * How much a finding of a document's checks weighs: what stops it being made — a pcb sent to a factory —, what a person
 * should look at, what is only said. The pcb's checks and the sheet's weigh alike.
 */
public enum FindingSeverity {

    ERROR, WARNING, NOTICE;

    /** The word a finding is written with: {@code error}. */
    public String word() {
        return name().toLowerCase(Locale.ROOT);
    }
}
