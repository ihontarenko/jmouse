package org.jmouse.cad.management;

import org.jmouse.cad.store.problem.CadProblem;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 🚑 What a jmCAD refusal answers with: an RFC 9457 problem whose status the refusal names, its sentence as the detail,
 * its {@code code}, and whatever it carries beside it — {@code line} and {@code column} of a document that does not
 * read, the {@code contradictions} of a drawing that disagrees with its netlist.
 *
 * <p>The code is the contract: an editor branches on it — {@code conflict} reloads, {@code invalid-document} points at
 * the line —, so it is carried as a property of its own, and so is every entry of {@link CadProblem#details()}.</p>
 *
 * <p>⚠️ Ordered at {@link #LIBRARY_PRECEDENCE}, the libraries' tier: ahead of a product's unordered advice, behind
 * anything a product orders deliberately. A product's own handler for {@link CadProblem} would never be reached; it
 * should not keep one, so exactly one advice answers.</p>
 */
@RestControllerAdvice
@Order(CadProblemDetails.LIBRARY_PRECEDENCE)
public class CadProblemDetails {

    /** The libraries' tier: ahead of an unordered product advice, with room for a product to slot one between. */
    public static final int LIBRARY_PRECEDENCE = Ordered.LOWEST_PRECEDENCE - 100;

    /**
     * 🧾 Any refusal jmCAD names — the host's included, which leave as this same type.
     *
     * @param problem the refusal
     * @return the problem detail
     */
    @ExceptionHandler(CadProblem.class)
    public ProblemDetail handleProblem(CadProblem problem) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(problem.status()), problem.getMessage());
        detail.setProperty("code", problem.code());
        problem.details().forEach(detail::setProperty);
        return detail;
    }
}
