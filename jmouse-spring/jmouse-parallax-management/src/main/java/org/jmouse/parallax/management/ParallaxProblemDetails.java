package org.jmouse.parallax.management;

import org.jmouse.formula.FormulaException;
import org.jmouse.parallax.problem.ParallaxProblem;
import org.jmouse.script.el.budget.ScriptBudgetExceededException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 🚑 What a Parallax refusal answers with: an RFC 9457 problem whose status the refusal names, its sentence as the
 * detail, and — beyond what the other libraries send — its {@code code} and whatever it carries beside it.
 *
 * <h3>⚠️ Why this one adds {@code code}</h3>
 *
 * <p>The files and storage advices answer with a status and a sentence only. Parallax's interfaces branch on the code
 * — {@code stale-revision} reloads, {@code invalid-document} points at {@code line} and {@code column}, {@code busy}
 * retries — so the code is the contract here, and the advice that renders a {@link ParallaxProblem} has to carry it.
 * Every entry of {@link ParallaxProblem#details()} becomes a property of its own, which is how {@code line},
 * {@code column}, {@code position} and {@code fields} reach a reader.</p>
 *
 * <h3>⚠️ Precedence</h3>
 *
 * <p>Ordered at {@link #LIBRARY_PRECEDENCE} — the same tier as {@code ProblemDetailAdvices.LIBRARY_PRECEDENCE} in the
 * storage libraries, restated here rather than imported so that Parallax does not carry the storage starter for one
 * constant: ahead of a product's unordered advice, behind anything a product orders deliberately. A product's own
 * handler for {@link ParallaxProblem} would therefore never be reached; it should not keep one, so exactly one advice
 * answers.</p>
 */
@RestControllerAdvice
@Order(ParallaxProblemDetails.LIBRARY_PRECEDENCE)
public class ParallaxProblemDetails {

    /** The libraries' tier: ahead of an unordered product advice, with room for a product to slot one between. */
    public static final int LIBRARY_PRECEDENCE = Ordered.LOWEST_PRECEDENCE - 100;

    /**
     * 🧾 Any refusal Parallax names — the host's included, which leave as this same type.
     *
     * @param problem the refusal
     * @return the problem detail
     */
    @ExceptionHandler(ParallaxProblem.class)
    public ProblemDetail handleProblem(ParallaxProblem problem) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(problem.status()), problem.getMessage());

        detail.setProperty("code", problem.code());
        problem.details().forEach(detail::setProperty);

        return detail;
    }

    /**
     * 🧮 A formula that does not read — the reader is pointed at the {@code position} where it stopped.
     *
     * @param exception the formula refusal
     * @return the problem detail
     */
    @ExceptionHandler(FormulaException.class)
    public ProblemDetail handleFormula(FormulaException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, exception.getMessage());

        detail.setProperty("code", "invalid-formula");
        detail.setProperty("position", exception.position());

        return detail;
    }

    /**
     * ⏱ A jMS program that spent its budget — the document's own doing, so it is answered as invalid input.
     *
     * @param exception the budget refusal
     * @return the problem detail
     */
    @ExceptionHandler(ScriptBudgetExceededException.class)
    public ProblemDetail handleProgramBudget(ScriptBudgetExceededException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, exception.getMessage());

        detail.setProperty("code", "program-budget-exceeded");

        return detail;
    }
}
