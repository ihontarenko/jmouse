package org.jmouse.access;

/**
 * Where a variable's value comes from — and therefore what a rule reading it is really reading.
 *
 * <p>Two kinds, because there are exactly two answers to <em>when is this worked out</em>, and the
 * difference is visible in both directions. A publisher states it by choosing {@code attach} or
 * {@code attachLazy}; a policy file states it by writing {@code constant} or {@code dynamic}; and the
 * two are compared, so a file that has stopped being true about it fails at load rather than at the
 * moment somebody trusts it.
 *
 * <p>⚠️ <strong>It is not a type system.</strong> Nothing here says what a variable's value
 * <em>is</em> — only whether asking for it can do work. That is the one property a rule's reader and a
 * rule's cost both turn on, and the reason a third kind has never been needed.
 */
public enum VariableKind {

    /**
     * Settled before any call arrives — configuration, in practice.
     *
     * <p>Which deployment this is, what the installation answers on. Free to read, identical for every
     * decision, and the honest reason such a value is attached at all rather than written into the
     * file as a literal: one file ships to more than one installation.
     */
    CONSTANT,

    /**
     * Worked out from the call being decided.
     *
     * <p>⚠️ Work, and therefore <strong>only done where a rule names it</strong> — a variable nobody
     * mentions costs nothing, because a condition binds the names it writes and no others. It also
     * means absence is ordinary: a call the variable does not apply to publishes nothing, and a rule
     * reading it there simply does not hold.
     */
    DYNAMIC;

    /** Whether reading this can do work — the whole of what the distinction is for. */
    public boolean isWorkedOutPerCall() {
        return this == DYNAMIC;
    }
}
