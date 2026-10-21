package org.jmouse.taxonomy.management;

import org.jmouse.taxonomy.TaxonomyException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 🚑 What a taxonomy refusal answers with: an RFC 9457 problem whose status its kind names, its sentence as the detail,
 * and a {@code code} an interface branches on — {@code not-found}, {@code invalid}, {@code conflict}.
 *
 * <p>Ordered in the libraries' tier, ahead of a product's unordered advice: exactly one advice answers a
 * {@link TaxonomyException}.</p>
 */
@RestControllerAdvice
@Order(TaxonomyProblemDetails.LIBRARY_PRECEDENCE)
public class TaxonomyProblemDetails {

    /** The libraries' tier: ahead of an unordered product advice, with room for a product to slot one between. */
    public static final int LIBRARY_PRECEDENCE = Ordered.LOWEST_PRECEDENCE - 100;

    @ExceptionHandler(TaxonomyException.class)
    public ProblemDetail handleRefusal(TaxonomyException refusal) {
        HttpStatus status = switch (refusal) {
            case TaxonomyException.NotFound ignored -> HttpStatus.NOT_FOUND;
            case TaxonomyException.Conflict ignored -> HttpStatus.CONFLICT;
            case TaxonomyException.Invalid ignored -> HttpStatus.UNPROCESSABLE_CONTENT;
            default -> HttpStatus.UNPROCESSABLE_CONTENT;
        };
        String code = switch (refusal) {
            case TaxonomyException.NotFound ignored -> "not-found";
            case TaxonomyException.Conflict ignored -> "conflict";
            default -> "invalid";
        };
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, refusal.getMessage());
        detail.setProperty("code", code);
        return detail;
    }
}
