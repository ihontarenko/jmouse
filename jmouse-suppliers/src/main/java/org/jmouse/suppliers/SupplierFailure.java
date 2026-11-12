package org.jmouse.suppliers;

/** A distributor that could not be asked, or refused: why, and its words with every credential taken out. */
public final class SupplierFailure extends RuntimeException {

    /** Why a search has no answer. */
    public enum Reason {
        /** The access holds no credentials of the kind this distributor takes. */
        NOT_CONFIGURED,
        /** It refused the credentials: 401 or 403. */
        CREDENTIALS_REFUSED,
        /** It refused to answer this often: 429. */
        RATE_LIMITED,
        /** It answered with any other refusal. */
        REFUSED,
        /** It did not answer: too slow, too long, the connection failed. */
        UNREACHABLE,
        /** What it answered cannot be read. */
        UNREADABLE,
        /** The thread waiting for it was interrupted. */
        INTERRUPTED
    }

    private final Reason reason;

    public SupplierFailure(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
