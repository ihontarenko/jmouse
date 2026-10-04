package org.jmouse.script.event;

/**
 * What refused, and why.
 *
 * <p>⚠️ BOTH HALVES ARE REQUIRED, and the constructor says why in its own message: a refusal that does
 * not name what refused leaves nobody to go and change it, and one that does not say why is a bare veto
 * with nowhere to go from.</p>
 *
 * @param by     what refused — a rule's name, or a guard's class
 * @param reason a phrase a person can read
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record Refusal(String by, String reason) {

    public Refusal {
        if (by == null || by.isBlank()) {
            throw new IllegalArgumentException(
                    "A refusal must say what refused; otherwise nobody can go and change it.");
        }

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "A refusal must say why; a bare veto leaves the reader with nowhere to go.");
        }
    }

    /** What the refusal reads as, for a log line. */
    public String describe() {
        return "%s — %s".formatted(reason, by);
    }

}
