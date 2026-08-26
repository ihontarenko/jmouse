package org.jmouse.storage.spring;

import org.jmouse.storage.exception.ObjectNotFoundException;
import org.jmouse.storage.exception.StorageException;
import org.jmouse.storage.exception.StorageKeyException;
import org.jmouse.storage.exception.UploadRejectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 🚑 What a storage refusal looks like on the wire, stated once.
 *
 * <p>Every product that adopted the library wrote these four mappings again, and by the time there
 * were four of them they had already stopped agreeing: two mapped both exceptions, one caught them
 * inline in its ingestion service, and one had <strong>no mapping at all</strong> — so a perfectly
 * ordinary rejected upload there answered 500, and nothing about the code looked wrong. That is the
 * shape of this bug in general: a copied mapping does not fail loudly when a copy is missing, it
 * fails as a status code somebody notices months later.</p>
 *
 * <h3>Why these statuses</h3>
 *
 * <p><strong>400 for a rejected upload</strong>, even when the cause is size. The library raises one
 * exception for every way an upload can be unacceptable and its message already says which; splitting
 * the status by re-reading that message would be guessing, and 413 would be wrong for a type
 * refusal.</p>
 *
 * <p><strong>404 for a missing object.</strong> From the caller's side, bytes that are registered but
 * absent from the bucket are indistinguishable from something that was never there — and 500 would
 * claim the product is broken when what happened is that somebody deleted a file out from under
 * it.</p>
 *
 * <p><strong>500 for anything else, without the message.</strong> A backend failure's text can carry a
 * bucket name, an endpoint or a key layout, and none of that is the caller's business. It is logged
 * in full and answered with a sentence.</p>
 *
 * <h3>⚠️ How a product overrides one</h3>
 *
 * <p>This advice sits at {@link ProblemDetailAdvices#LIBRARY_PRECEDENCE} — ahead of an unordered
 * product advice, and deliberately so. A product that means to answer one of these differently gives
 * its own advice an explicit {@code @Order} ahead of that; a product that does not still has to keep
 * its catch-all in an advice of its own at {@link Ordered#LOWEST_PRECEDENCE}, or the catch-all claims
 * these exceptions before this class is ever consulted. {@link ProblemDetailAdvices} carries the whole
 * reasoning and is the file to read before changing either number.</p>
 *
 * <p>⚠️ This used to sit at {@link Ordered#LOWEST_PRECEDENCE}, described as a tie that was
 * <em>harmless today</em>. It was not: the tie is broken by bean registration order, which puts a
 * component-scanned product advice first, so every refusal below was answered as an internal error
 * with the message thrown away.</p>
 */
@RestControllerAdvice
@Order(ProblemDetailAdvices.LIBRARY_PRECEDENCE)
public class StorageProblemDetails {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageProblemDetails.class);

    /** 🚫 The upload policy refused this content — wrong type, too large, or nothing to store. */
    @ExceptionHandler(UploadRejectedException.class)
    public ProblemDetail handleUploadRejected(UploadRejectedException refusal) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, refusal.getMessage());
    }

    /** 🔍 Nothing is stored where the registry says it is. */
    @ExceptionHandler(ObjectNotFoundException.class)
    public ProblemDetail handleObjectNotFound(ObjectNotFoundException missing) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, missing.getMessage());
    }

    /** 🔑 The key a caller supplied could not name an object. */
    @ExceptionHandler(StorageKeyException.class)
    public ProblemDetail handleUnusableKey(StorageKeyException unusable) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, unusable.getMessage());
    }

    /**
     * 💥 The backend failed.
     *
     * <p>⚠️ The message is logged and <strong>not</strong> returned: it is the one exception here whose
     * text is written for an operator rather than for a caller, and it routinely names infrastructure.</p>
     */
    @ExceptionHandler(StorageException.class)
    public ProblemDetail handleStorageFailure(StorageException failure) {
        LOGGER.error("Storage failed while serving a request", failure);

        return ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "The file store could not complete this request.");
    }
}
