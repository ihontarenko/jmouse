package org.jmouse.ai.agent;

import java.util.List;

/**
 * What a {@link AgentAuthority#RESTRICTED} agent may do, and where.
 *
 * <h2>Why this port exists at all</h2>
 *
 * <p>Restricting an agent is one click. Filling one was, in one product, a screen it had written itself
 * — and in the other, nothing at all: the shared screen offered <em>Restrict it</em> and the access
 * screen behind it would not accept an agent as a subject, so the button led somewhere an agent could
 * do nothing and there was no way back except undoing it.
 *
 * <p>The storage was never the problem. Both products already keep an agent's grants in the same tables
 * as a person's, keyed on the same opaque subject identifier. What was missing is a way for one screen
 * to <em>edit</em> them without knowing what the product calls the things it is editing.
 *
 * <h2>Three axes, and every one of them is the product's own vocabulary</h2>
 *
 * <ul>
 *   <li><strong>Permissions</strong> — what it may do, installation-wide.
 *   <li><strong>Places</strong> — where it may act. An identifier and a label, never a type: one product
 *       calls a place a workspace and the other calls it a project, and this module does not need to
 *       know which.
 *   <li><strong>Roles</strong> — as <em>what</em> it acts in a place. ⚠️ An axis rather than a constant,
 *       and the first draft of this port had it as a constant: one product's role name written into the
 *       library, which is exactly the hardcoding a port is supposed to prevent. Both products build
 *       their access model out of roles; neither's names belong here.
 * </ul>
 *
 * <p>⚠️ <strong>None of it is the final answer, and a screen has to say so.</strong> Everything here is
 * the agent's <em>own</em> set; the engine intersects it with its owner's on every request and in every
 * scope. Granting an agent something its owner does not hold changes nothing, which is why
 * {@link #offerFor} exists — offering the owner's set is what stops somebody granting into a void.
 */
public interface AgentGrants {

    /** Somewhere an agent can be put to work, named the way the product names it. */
    record Place(String id, String label) {
    }

    /**
     * A role an agent can be given, and where it means anything.
     *
     * @param placeScoped whether it has to be pinned to a place. ⚠️ Both mistakes are worth refusing
     *                    rather than correcting: a place-scoped role granted everywhere is a widening
     *                    nobody asked for, and an installation-wide role pinned to one place confers
     *                    nothing where the person expected everything
     */
    record Role(String name, boolean placeScoped) {
    }

    /** One role held in one place — {@code placeId} null for an installation-wide role. */
    record Placement(String roleName, String placeId) {
    }

    /** What an agent holds now. */
    record Held(List<String> permissions, List<Placement> placements) {
    }

    /**
     * What its owner could hand down — the ceiling, offered rather than assumed.
     *
     * <p>⚠️ A screen that offered everything the installation defines would let somebody grant an agent
     * a permission its owner does not hold, which resolves to nothing and looks exactly like a bug in
     * the agent. Offering only what can actually take effect is the difference between a form that
     * teaches the rule and one that hides it.
     */
    record Offer(List<String> permissions, List<Place> places, List<Role> roles) {
    }

    /** Something this port will not do, in a sentence somebody can act on. */
    class RefusedException extends RuntimeException {

        public RefusedException(String message) {
            super(message);
        }
    }

    Held heldBy(String agentId);

    Offer offerFor(String agentId);

    /**
     * Sets an agent's whole set at once, and answers with what it ends up holding.
     *
     * <p>⚠️ <strong>A replacement rather than a delta, deliberately.</strong> Two screens editing the
     * same agent with deltas silently merge into a set neither person chose; a replacement means the
     * second save wins visibly, which is the failure people can see and correct.
     */
    Held replace(
            String agentId, List<String> permissions, List<Placement> placements, String grantedBy);

    /**
     * Takes everything back, for an agent that is about to stop existing.
     *
     * <h2>⚠️ Why discarding an agent is not enough on its own</h2>
     *
     * <p>An agent's grants are not in this library's tables. They are in whatever store the product's
     * authorization uses, keyed on the same opaque subject identifier and with <strong>no foreign key
     * either way</strong> — that is precisely what let an agent move between two completely different
     * arrangements with its grants intact. Nothing cascades, and nothing warns.
     *
     * <p>So an agent discarded without this leaves its grants behind, resolving quietly for whatever
     * identifier is issued next. Both products had this call in their own delete path before there was a
     * shared one; putting it on the port is what stops the shared one being the version that forgets.
     */
    void revokeAllOf(String agentId);

    /**
     * For a product with no way to grant an agent anything.
     *
     * <p>Every read answers empty and every write refuses with a sentence saying so — which is a screen
     * that explains itself, rather than one that appears to save something going nowhere.
     */
    static AgentGrants unavailable() {
        return new AgentGrants() {

            private static final String WHY =
                    "This installation has no way to grant an agent anything of its own, so an agent "
                    + "here can only act with its owner's authority.";

            @Override
            public Held heldBy(String agentId) {
                return new Held(List.of(), List.of());
            }

            @Override
            public Offer offerFor(String agentId) {
                return new Offer(List.of(), List.of(), List.of());
            }

            @Override
            public Held replace(
                    String agentId, List<String> permissions, List<Placement> placements,
                    String grantedBy) {

                throw new RefusedException(WHY);
            }

            @Override
            public void revokeAllOf(String agentId) {
                // ⚠️ Silent, unlike replace. This is called on the way to discarding an agent, and an
                // installation that never granted one anything has nothing to take back — refusing here
                // would turn a successful discard into a failure over work that was not needed.
            }
        };
    }
}
