package org.jmouse.cad.store.problem;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A failure the API answers in words. The subtype decides the status; the message is safe to show a reader, and the
 * details are what a reader needs to act on it — the line and column of a document that does not read, the
 * contradictions of a drawing that disagrees with its netlist.
 *
 * <p>Every refusal inside jmCAD's services is one of these, the host's included: a {@code CadAuthority} or
 * {@code CadPlaces} saying no answers with this type, so a service that catches a refusal catches this one only.</p>
 */
public abstract sealed class CadProblem extends RuntimeException
        permits CadProblem.NotFound, CadProblem.Conflict, CadProblem.Invalid, CadProblem.Refused,
                CadProblem.Unauthenticated, CadProblem.Unprocessable {

    private final String              code;
    private final Map<String, Object> details;

    protected CadProblem(String code, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.details = Collections.unmodifiableMap(new LinkedHashMap<>(details));
    }

    /** What kind of refusal it is, as the API names it: {@code invalid-document}, {@code conflict}. */
    public String code() {
        return code;
    }

    /** What the problem carries beside its code, by name; empty for most refusals. */
    public Map<String, Object> details() {
        return details;
    }

    /** The HTTP status a web layer answers this problem with: named as a number, not as a framework type. */
    public abstract int status();

    public static NotFound notFound(String what) {
        return new NotFound(what + " was not found.");
    }

    public static Conflict conflict(String message) {
        return new Conflict("conflict", message);
    }

    public static Conflict staleVersion(String what) {
        return new Conflict("conflict", what + " was changed by somebody else. Reload it and try again.");
    }

    public static Invalid invalid(String message) {
        return new Invalid("invalid-request", message, Map.of());
    }

    /**
     * A document that does not read, or reads into something the engine refuses, refused where it went wrong.
     *
     * @param line   the 1-based line of the document, or 0 when the refusal names no line
     * @param column the 1-based column, or 0 when only the line is known
     */
    public static Invalid invalidDocument(String message, int line, int column) {
        Map<String, Object> position = new LinkedHashMap<>();
        position.put("line", line);
        position.put("column", column);
        return new Invalid("invalid-document", message, position);
    }

    /** A circuit whose drawing joins nets its netlist keeps apart. */
    public static Invalid layoutDisagrees(List<String> contradictions) {
        return new Invalid("layout-disagrees", "The drawing does not agree with the parts: " + contradictions.getFirst(),
                           Map.of("contradictions", List.copyOf(contradictions)));
    }

    public static Refused refused(String message) {
        return new Refused(message);
    }

    public static final class NotFound extends CadProblem {

        public NotFound(String message) {
            super("not-found", message, Map.of());
        }

        @Override
        public int status() {
            return 404;
        }
    }

    public static final class Conflict extends CadProblem {

        public Conflict(String code, String message) {
            super(code, message, Map.of());
        }

        @Override
        public int status() {
            return 409;
        }
    }

    public static final class Invalid extends CadProblem {

        public Invalid(String code, String message, Map<String, Object> details) {
            super(code, message, details);
        }

        @Override
        public int status() {
            return 400;
        }
    }

    public static final class Refused extends CadProblem {

        public Refused(String message) {
            super("forbidden", message, Map.of());
        }

        @Override
        public int status() {
            return 403;
        }
    }

    public static final class Unauthenticated extends CadProblem {

        public Unauthenticated(String message) {
            super("unauthenticated", message, Map.of());
        }

        @Override
        public int status() {
            return 401;
        }
    }

    /** A circuit that reads but cannot be run: it does not converge, or a part it needs cannot be compiled. */
    public static final class Unprocessable extends CadProblem {

        public Unprocessable(String code, String message) {
            super(code, message, Map.of());
        }

        @Override
        public int status() {
            return 422;
        }
    }
}
