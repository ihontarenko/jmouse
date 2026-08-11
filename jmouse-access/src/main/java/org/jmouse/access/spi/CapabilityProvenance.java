package org.jmouse.access.spi;

/**
 * Why a capability grant exists, and what issued it.
 *
 * <h2>⚠️ Not {@link GrantOrigin}, and the two must not be merged</h2>
 *
 * <p>{@link GrantOrigin} answers <em>where the rule is kept</em> — a file or a table — because that is
 * what decides which editor a screen may offer. This answers <em>what produced it</em>. A grant
 * issued by a bundle can be a row or a line, and a hand-written gift can be either too, so folding
 * them into one enum would double it and make every reader handle four cases where there are two
 * questions with two answers. The same argument {@code GrantOrigin} already makes about
 * {@code PermissionSource.Kind}.
 *
 * <h2>Why {@link #reference} is not optional in practice</h2>
 *
 * <p>{@link #source} says a bundle issued this. {@link #reference} says <strong>which</strong> — and
 * without it, "what is this account on" can only be answered by reverse-matching a set of grants
 * against a catalogue and hoping the match is unique. It also makes withdrawal precise: retiring one
 * bundle's grants must not touch a gift that happens to be about the same capability, and a
 * comparison on {@link #source} alone cannot tell them apart from each other.
 *
 * <h2>The vocabulary belongs to the product</h2>
 *
 * <p>{@link #source} is a string, not an enum, for the same reason {@code ScopeKind} is an interface:
 * one product sells subscriptions and top-ups, another issues internal allocations that nobody pays
 * for, and a library that enumerated either would be a library only one of them can adopt. What the
 * engine needs is that two grants can be told apart and that a withdrawal can name a set — and a
 * string does both.
 *
 * @param source    what kind of thing produced this, in the product's own vocabulary
 * @param reference which one — the bundle's code, an order number, a ticket — or null where the
 *                  source is answer enough on its own
 */
public record CapabilityProvenance(String source, String reference) {

    /** What a store answers when it knows nothing more than "somebody recorded this". */
    public static final String RECORDED = "RECORDED";

    private static final CapabilityProvenance UNATTRIBUTED = new CapabilityProvenance(RECORDED, null);

    public CapabilityProvenance {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException(
                    "A capability grant has to say what produced it. A grant with no provenance cannot "
                    + "be withdrawn as a set, and cannot be explained to whoever it is about.");
        }
    }

    /** Recorded by hand, with nothing further to attribute it to. */
    public static CapabilityProvenance unattributed() {
        return UNATTRIBUTED;
    }

    public static CapabilityProvenance of(String source) {
        return new CapabilityProvenance(source, null);
    }

    public static CapabilityProvenance of(String source, String reference) {
        return new CapabilityProvenance(source, reference);
    }

    /**
     * Whether this grant came from the named source, and — where one is given — that exact issuer.
     *
     * <p>The question a withdrawal asks. Passing a null {@code reference} matches every grant of the
     * source, which is how "retire everything this bundle gave them" is written without naming the
     * bundle twice.
     */
    public boolean cameFrom(String otherSource, String otherReference) {
        if (!source.equals(otherSource)) {
            return false;
        }

        return otherReference == null || otherReference.equals(reference);
    }

    /** What the control room prints beside a grant. */
    public String describe() {
        return reference == null ? source : source + ":" + reference;
    }
}
