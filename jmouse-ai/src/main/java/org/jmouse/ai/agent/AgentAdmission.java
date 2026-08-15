package org.jmouse.ai.agent;

import java.time.Instant;
import java.util.Optional;

/**
 * Whether an agent may act right now — the four-part question, asked once.
 *
 * <p><strong>Four rows, one answer, and the reason this is not four {@code if} statements in each
 * product.</strong> An agent can be switched off; its connection can be revoked; that connection's
 * renewal credential can have run out; and the agent can have been discarded while a self-contained token
 * naming it is still in somebody's hands. Every one of those is a different sentence to whoever is
 * holding the credential, and every one of them is easy to leave out — the last most of all, because it
 * only happens in the window a token outlives its row.
 *
 * <p><strong>It answers with the sentence, not with an exception.</strong> The same rule is asked at two
 * places that cannot share an exception type: a protocol endpoint refusing a credential owes an OAuth
 * error, and a tool call owes a refusal the mechanism renders. So the decision and the wording live here
 * and each caller throws its own kind of no — which is what keeps two products refusing identically
 * without this module learning what either of their surfaces is.
 *
 * <p>⚠️ <strong>Empty means permitted, and it is deliberately the awkward way round.</strong> A method
 * returning {@code boolean} would let a caller ignore the answer and still compile; one returning the
 * reason makes the refusal the thing you have in your hand.
 */
public final class AgentAdmission {

    private AgentAdmission() {
    }

    /**
     * Why this agent may not act, or nothing at all.
     *
     * @param agent   the agent a credential named — ⚠️ {@code null} where the row is gone, which is a
     *                real state rather than a programming error: a self-contained token can outlive what
     *                it names, and that is the whole reason it names it
     * @param through the connection the call arrived over, or {@code null} where it did not arrive over
     *                one. An in-application assistant has no connection, and passing null says so rather
     *                than inventing a fake one
     */
    public static Optional<String> refusalFor(Agent agent, AgentConnection through, Instant now) {
        if (agent == null) {
            return Optional.of(
                    "The agent this credential was issued for no longer exists. Nothing was changed. "
                    + "Create a new agent and connect again.");
        }

        if (!agent.enabled()) {
            return Optional.of(
                    "Agent '" + agent.name() + "' is switched off, so it may not act. Nothing was "
                    + "changed. Its connections and its permissions are untouched — switching it back on "
                    + "is all that is needed.");
        }

        if (through == null) {
            return Optional.empty();
        }

        if (through.isRevoked()) {
            return Optional.of(
                    "The connection this credential belongs to was ended on " + through.revokedAt()
                    + ", so it no longer works. Nothing was changed. Authorize again to open a new one.");
        }

        if (!through.canBeRenewed(now)) {
            return Optional.of(
                    "The connection this credential belongs to expired on " + through.refreshExpiresAt()
                    + ". Nothing was changed. Authorize again to open a new one.");
        }

        return Optional.empty();
    }

    /** The same question where no connection is involved — an in-application assistant, typically. */
    public static Optional<String> refusalFor(Agent agent) {
        return refusalFor(agent, null, Instant.now());
    }
}
