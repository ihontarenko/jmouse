package org.jmouse.telegram.management;

import org.jmouse.telegram.TelegramException;
import org.jmouse.telegram.TelegramRefusal;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * What a client of these controllers sees when something is refused — and nothing else's.
 *
 * <p>⚠️ <strong>Scoped to this module's controllers by {@code assignableTypes}</strong>, so it never
 * intercepts the product's own: a library's global advice either swallows the product's handling or is
 * swallowed by it, depending on a precedence that is tied rather than ordered.
 *
 * <p>Every body is an RFC 9457 problem with a {@code refusal} property naming the case, so a panel can
 * branch on a word rather than on a sentence.
 */
@RestControllerAdvice(assignableTypes = {
        TelegramAccountController.class,
        TelegramBindingController.class,
        TelegramJournalController.class,
        TelegramChatController.class
})
public class TelegramManagementProblems {

    @ExceptionHandler(TelegramException.class)
    public ResponseEntity<ProblemDetail> refused(TelegramException exception) {
        TelegramRefusal refusal = exception.refusal();
        HttpStatus      status  = statusOf(refusal);
        ProblemDetail   problem = ProblemDetail.forStatusAndDetail(status, refusal.message());

        problem.setTitle("Telegram refused");
        problem.setProperty("refusal", refusal.getClass().getSimpleName());

        ResponseEntity.BodyBuilder response = ResponseEntity.status(status);

        if (refusal instanceof TelegramRefusal.FloodWait wait) {
            response.header(HttpHeaders.RETRY_AFTER, String.valueOf(Math.max(1, wait.retryAfter().toSeconds())));
        }

        return response.body(problem);
    }

    @ExceptionHandler(AccountsUnavailableException.class)
    public ProblemDetail unavailable(AccountsUnavailableException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Not kept in the database");
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail invalid(IllegalArgumentException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Invalid request");
        return problem;
    }

    /**
     * ⚠️ Never 401 or 403: those say <em>the caller</em> may not, and a panel would send its user to sign in
     * again over a bot token that is wrong.
     */
    private static HttpStatus statusOf(TelegramRefusal refusal) {
        return switch (refusal) {
            case TelegramRefusal.FloodWait wait -> HttpStatus.SERVICE_UNAVAILABLE;
            case TelegramRefusal.TransportFailure failure -> HttpStatus.BAD_GATEWAY;
            case TelegramRefusal.ChatNotFound missing -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
    }
}
