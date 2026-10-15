package org.jmouse.git;

/**
 * A typed refusal of a Git operation. The message is a fixed sentence chosen by the reason: it never carries a
 * credential, a remote address or anything a remote sent, and no underlying exception is attached, because JGit's
 * own messages may quote what the server said.
 */
public final class GitException extends RuntimeException {

    /** Why an operation was refused. */
    public enum Reason {

        /** The address is outside the policy: a scheme not admitted, a private host, credentials in the address, or a token over anything but https. */
        ADDRESS_NOT_ALLOWED("The repository address is not allowed by the configured policy."),

        /** No repository answers at that address, or its host does not resolve. */
        NOT_FOUND("No repository answers at that address."),

        /**
         * The remote demands credentials, or refused the ones given. Hosts commonly answer this way for a repository
         * that does not exist, so as not to reveal which private ones do.
         */
        UNAUTHORISED("The repository refused access; it may be private, or the credentials were rejected."),

        /** The requested branch does not exist, or the remote does not say which branch is its default. */
        BRANCH_NOT_FOUND("The requested branch does not exist on the remote."),

        /** The remote holds no commits at all. */
        EMPTY_REPOSITORY("The repository has no commits."),

        /** The checkout outgrew the configured byte ceiling, while transferring or once complete. */
        TOO_LARGE("The checkout exceeds the configured size ceiling."),

        /** The operation outlived its deadline, or the remote stopped answering. */
        TIMED_OUT("The operation exceeded its time limit."),

        /**
         * The caller's {@link GitCancellation} asked to stop. A clone removes what it wrote; an update is stopped
         * before it touches the working tree, so the checkout stays at the commit it held.
         */
        CANCELLED("The operation was cancelled."),

        /** A clone was asked into a directory that already holds files. */
        DIRECTORY_NOT_EMPTY("The target directory is not empty."),

        /** The directory is not a checkout this library can update: not a repository, or a repository of another remote. */
        NOT_A_CHECKOUT("The directory is not a checkout of that remote."),

        /** Any other transfer or storage failure. */
        TRANSPORT("The repository could not be transferred.");

        private final String message;

        Reason(String message) {
            this.message = message;
        }

        /** The fixed, safe sentence this reason is reported with. */
        public String message() {
            return message;
        }
    }

    private final Reason reason;

    public GitException(Reason reason) {
        super(requireReason(reason).message());
        this.reason = reason;
    }

    /** Why the operation was refused. */
    public Reason reason() {
        return reason;
    }

    /** Whether repeating the same call later may succeed without the caller changing anything. */
    public boolean retryable() {
        return reason == Reason.TIMED_OUT || reason == Reason.TRANSPORT;
    }

    private static Reason requireReason(Reason reason) {
        if (reason == null) {
            throw new IllegalArgumentException("A Git refusal needs a reason.");
        }
        return reason;
    }
}
