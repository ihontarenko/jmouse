package org.jmouse.j2js.teavm.supplement;

/**
 * 🧩 What a {@link Supplement supplement} takes the place of in its JDK class.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public enum SupplementPlace {
    /** Only a gap: a method the class lacks, or a native one TeaVM has no body for. A method with a body keeps it. */
    GAP,
    /**
     * The method's body as well: TeaVM's own answers rightly but at a cost the page does better natively — the supplement
     * answers exactly alike, only faster.
     */
    BODY
}
