package org.jmouse.tx.infrastructure;

import org.jmouse.tx.core.TransactionSession;

/**
 * Result of propagation resolution.
 */
public final class TransactionParticipationContext {

    private final TransactionParticipation participation;
    private final TransactionSession       session;

    public TransactionParticipationContext(
            TransactionParticipation participation,
            TransactionSession session
    ) {
        this.participation = participation;
        this.session = session;
    }

    public TransactionParticipation getParticipation() {
        return participation;
    }

    public TransactionSession getSession() {
        return session;
    }
}
