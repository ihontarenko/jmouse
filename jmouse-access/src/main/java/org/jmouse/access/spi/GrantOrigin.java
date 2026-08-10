package org.jmouse.access.spi;

/**
 * Where a grant is <strong>written down</strong> — which is a different question from where it came
 * from, and the one an administration screen has to answer before it offers to change anything.
 *
 * <p>{@link RoleGrant} and {@link DirectGrant} already carry who granted them and when. What they
 * could not say is whether the thing a screen is about to edit is a row in a table or a line in a
 * file, and the two need different affordances:
 *
 * <ul>
 *   <li>⚠️ <strong>A declared grant must not render as an editable row.</strong> Somebody who deletes
 *       one, finds nothing happens, and comes back tomorrow to the same permission has learned that
 *       the screen lies — which costs more than never having offered the control.
 *   <li>⚠️ <strong>Nor as a dead end.</strong> "You cannot change this" without saying where to go is
 *       the same failure, politely. A declared grant is editable <em>as policy</em>, in the notation
 *       it is written in, and the screen that refuses the row editor should offer that one.
 * </ul>
 *
 * <p>It is deliberately not a {@link PermissionSource.Kind}. How a permission arrived — a role, a
 * personal allow, a denial — and where the rule is kept are independent facts: a role assignment can
 * be a row or a line, and so can a denial. Folding them into one enum would double it and make every
 * reader handle four cases where there are two questions with two answers.
 *
 * @param document what the file is called, or null where a table holds this
 * @param line     which line of it, or {@code 0} where nothing points at one
 */
public record GrantOrigin(String document, int line) {

    private static final GrantOrigin STORED = new GrantOrigin(null, 0);

    /** A row somebody inserted. The default, and what every store answers unless it says otherwise. */
    public static GrantOrigin stored() {
        return STORED;
    }

    /**
     * A line in a policy file.
     *
     * @param document what the file is called
     * @param line     which line of it
     */
    public static GrantOrigin declaredIn(String document, int line) {
        return new GrantOrigin(document, line);
    }

    /** Whether a file holds this rather than a table. */
    public boolean isDeclared() {
        return document != null && !document.isBlank();
    }

    /**
     * Whether a screen editing rows may offer to change this.
     *
     * <p>The one question this type exists for, phrased as the caller asks it.
     */
    public boolean isEditableAsARow() {
        return !isDeclared();
    }

    /** What the control room prints beside a grant. */
    public String describe() {
        if (!isDeclared()) {
            return "stored";
        }

        return line > 0 ? document + ":" + line : document;
    }
}
