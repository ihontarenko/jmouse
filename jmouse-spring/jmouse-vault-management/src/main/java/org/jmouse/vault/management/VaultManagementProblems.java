package org.jmouse.vault.management;

import org.jmouse.vault.MissingSecretException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 🧯 The vault API's refusals as problem details. ⚠️ Messages here never quote a value. */
@RestControllerAdvice(assignableTypes = {
        VaultReadController.class,
        VaultWriteController.class,
        VaultRevealController.class
})
public class VaultManagementProblems {

    @ExceptionHandler(MissingSecretException.class)
    public ProblemDetail missing(MissingSecretException exception) {
        return problem(HttpStatus.NOT_FOUND, "Not configured", exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail invalid(IllegalArgumentException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail conflict(IllegalStateException exception) {
        return problem(HttpStatus.CONFLICT, "Not possible here", exception.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);

        problem.setTitle(title);

        return problem;
    }
}
