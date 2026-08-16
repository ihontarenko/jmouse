package org.jmouse.ai.access;

import org.jmouse.access.Subject;
import org.jmouse.ai.agent.AgentOwners;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Whose agents these are, answered by the access engine — so no product has to write it.
 *
 * <h2>Why one class serves both products</h2>
 *
 * <p>The two put entirely different things into the authentication: one an account entity whose
 * {@code getName()} is an email address, the other a {@code Jwt} whose subject belongs to the identity
 * server rather than to this installation. Neither is an owner reference.
 *
 * <p>But both already answer {@link CurrentSubject}, and both key an agent on the same identifier the
 * engine keys a grant on — Innoventa on its account id, Tessera on its member id. So the owner reference
 * <em>is</em> {@link Subject#principalId()}, in both, by construction. Anything else would mean an
 * agent's grants were stored under one identifier and its ownership under another.
 *
 * <h2>⚠️ An agent may not manage agents</h2>
 *
 * <p>A {@link Subject#isAgent()} caller is refused outright rather than answered with its owner. An agent
 * acting with inherited authority is, to the engine, indistinguishable from the person — which is exactly
 * right for reading their records and exactly wrong for editing the switch that stops it.
 *
 * <p>⚠️ <strong>This is the second lock and not the first.</strong> Both products confine an agent's
 * credential to the protocol endpoint, so an agent cannot reach an {@code /api/ai} route at all. That is
 * the guard that matters; this one is here because a confinement rule is a line of configuration and
 * this is a line of code, and the failure it prevents is an agent switching off the audit of itself.
 */
public final class SubjectAgentOwners implements AgentOwners {

    /**
     * ⚠️ A {@link Supplier} rather than {@code CurrentSubject}, and not for the sake of testability.
     * That interface lives in {@code jmouse-access-enforcement}, and this module is deliberately held to
     * {@code jmouse-access}' core vocabulary — a rule an ArchUnit test in this module enforces, because a
     * bridge that grows a dependency on the HTTP enforcement layer stops being usable by anything that is
     * not a web application. The starter, which is allowed to know about every module, passes the method
     * reference.
     */
    private final Supplier<Subject> subjects;

    public SubjectAgentOwners(Supplier<Subject> subjects) {
        this.subjects = subjects;
    }

    @Override
    public Optional<String> current() {
        Subject subject = subjects.get();

        if (subject == null || !subject.isAuthenticated() || subject.isAgent()) {
            return Optional.empty();
        }

        return Optional.ofNullable(subject.principalId());
    }
}
