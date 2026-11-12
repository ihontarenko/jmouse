package org.jmouse.licence;

/**
 * What a licence expression answers for a use.
 *
 * @param expression the expression ruled on, as written; {@code null} for a work that states none
 * @param reason     why, in a sentence a person can act on
 */
public record LicenceRuling(String expression, LicenceUse use, Verdict verdict, String reason) {

    /** Whether the use is permitted. */
    public boolean permits() {
        return verdict.permits();
    }
}
