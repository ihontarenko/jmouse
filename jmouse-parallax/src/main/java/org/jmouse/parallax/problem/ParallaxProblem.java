package org.jmouse.parallax.problem;


import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A failure the API answers in words. The subtype decides the status; the message is safe to show a reader, and the
 * details are what a reader needs to act on it — the line and column of a document that does not read.
 *
 * <p>Every refusal inside Parallax is one of these, the host's included: a {@code ParallaxAuthority} or
 * {@code ParallaxPlaces} saying no answers with this type, so a service that catches a refusal catches this one only.
 * A host whose own exceptions differ translates them at that boundary through the public constructors, keeping the
 * message and the code.
 */
public abstract sealed class ParallaxProblem extends RuntimeException
        permits ParallaxProblem.NotFound, ParallaxProblem.Conflict, ParallaxProblem.Invalid,
                ParallaxProblem.Refused, ParallaxProblem.Unauthenticated, ParallaxProblem.LimitExceeded, ParallaxProblem.Busy {

    private final String code;
    private final Map<String, Object> details;

    protected ParallaxProblem(String code, String message) {
        this(code, message, Map.of());
    }

    protected ParallaxProblem(String code, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.details = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(details));
    }

    public String code() {
        return code;
    }

    /** What the problem carries beside its code, by name; empty for most refusals. */
    public Map<String, Object> details() {
        return details;
    }

    /** The HTTP status a web layer answers this problem with: the core names it as a number, not as a framework type. */
    public abstract int status();

    public static NotFound notFound(String what) {
        return new NotFound(what + " was not found.");
    }

    public static Conflict conflict(String message) {
        return new Conflict("conflict", message);
    }

    public static Conflict staleRevision(String what) {
        return new Conflict("stale-revision", what + " was changed by somebody else. Reload it and try again.");
    }

    public static Invalid invalid(String message) {
        return new Invalid("invalid", message);
    }

    public static Invalid invalid(String code, String message) {
        return new Invalid(code, message);
    }

    /**
     * A jMX document that does not read or does not compile, refused where it went wrong.
     *
     * @param line   the 1-based line of the document
     * @param column the 1-based column, or 0 when only the line is known
     */
    public static Invalid invalidDocument(String message, int line, int column) {
        Map<String, Object> position = new LinkedHashMap<>();
        position.put("line", line);
        position.put("column", column);
        return new Invalid("invalid-document", message, position);
    }

    public static Refused refused(String message) {
        return new Refused(message);
    }

    /** A shared capacity is taken for now; the same request succeeds later. */
    public static Busy busy(String message) {
        return new Busy(message);
    }

    public static LimitExceeded limit(String message) {
        return new LimitExceeded(message);
    }

    public static final class NotFound extends ParallaxProblem {
        public NotFound(String message) {
            super("not-found", message);
        }

        @Override
        public int status() {
            return 404;
        }
    }

    public static final class Conflict extends ParallaxProblem {
        public Conflict(String code, String message) {
            super(code, message);
        }

        @Override
        public int status() {
            return 409;
        }
    }

    public static final class Invalid extends ParallaxProblem {
        public Invalid(String code, String message) {
            super(code, message);
        }

        public Invalid(String code, String message, Map<String, Object> details) {
            super(code, message, details);
        }

        @Override
        public int status() {
            return 422;
        }
    }

    public static final class Refused extends ParallaxProblem {
        public Refused(String message) {
            super("refused", message);
        }

        @Override
        public int status() {
            return 403;
        }
    }

    public static final class Unauthenticated extends ParallaxProblem {
        public Unauthenticated(String message) {
            super("unauthenticated", message);
        }

        @Override
        public int status() {
            return 401;
        }
    }

    public static final class LimitExceeded extends ParallaxProblem {
        public LimitExceeded(String message) {
            super("limit-exceeded", message);
        }

        @Override
        public int status() {
            return 422;
        }
    }

    public static final class Busy extends ParallaxProblem {
        public Busy(String message) {
            super("busy", message);
        }

        @Override
        public int status() {
            return 503;
        }
    }
}
