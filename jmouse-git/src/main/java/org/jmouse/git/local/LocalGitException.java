package org.jmouse.git.local;

/** Typed local failures; remote credentials and transport messages are never involved. */
public final class LocalGitException extends RuntimeException {
    public enum Reason {
        NOT_A_REPOSITORY,
        /** A branch or a commit named that is not there. */
        NOT_FOUND,
        CONFLICT,
        INVALID_PATH,
        TOO_LARGE,
        IDENTITY_REQUIRED,
        UNSUPPORTED_STATE,
        STORAGE
    }

    private final Reason reason;

    public LocalGitException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
