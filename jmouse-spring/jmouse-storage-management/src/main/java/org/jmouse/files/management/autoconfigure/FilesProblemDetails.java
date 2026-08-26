package org.jmouse.files.management.autoconfigure;

import org.jmouse.files.exception.DirectoryException;
import org.jmouse.files.exception.FileHeldException;
import org.jmouse.files.exception.ManagedFileTooLargeException;
import org.jmouse.files.exception.RemoteFetchException;
import org.jmouse.files.exception.FileBindingException;
import org.jmouse.files.exception.ManagedFileNotFoundException;
import org.jmouse.storage.spring.ProblemDetailAdvices;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 🚑 What a filing refusal answers with.
 *
 * <p>Beside {@code StorageProblemDetails} rather than inside it, because the two mean different things
 * to whoever has to fix them: storage's 404 says the bytes are gone from the bucket, this one says the
 * row was never there.</p>
 *
 * <p>⚠️ Same precedence rule as its neighbour: this sits at
 * {@link ProblemDetailAdvices#LIBRARY_PRECEDENCE}, ahead of an unordered product advice, and a product
 * that means to answer differently gives its own advice an explicit {@code @Order} ahead of that.
 * {@link ProblemDetailAdvices} carries the reasoning — including why a product's catch-all has to live
 * alone in an advice of its own, which no setting here can arrange for it.</p>
 */
@RestControllerAdvice
@Order(ProblemDetailAdvices.LIBRARY_PRECEDENCE)
public class FilesProblemDetails {

    /** 🔍 No such file row. */
    @ExceptionHandler(ManagedFileNotFoundException.class)
    public ProblemDetail handleFileNotFound(ManagedFileNotFoundException missing) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, missing.getMessage());
    }

    /** 🔗 A binding that could not be made, or could not be read as one. */
    @ExceptionHandler(FileBindingException.class)
    public ProblemDetail handleUnusableBinding(FileBindingException unusable) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, unusable.getMessage());
    }

    /**
     * 🌳 The tree refused something.
     *
     * <p>400 rather than 409 even when the cause is a name already taken: the library raises one
     * exception for every way a directory operation can be refused and its message says which, so
     * splitting the status by re-reading that message would be guessing.</p>
     */
    @ExceptionHandler(DirectoryException.class)
    public ProblemDetail handleDirectoryRefusal(DirectoryException refusal) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, refusal.getMessage());
    }

    /**
     * 🔒 Something is holding the file, so it may not stop being reachable.
     *
     * <p>⚠️ <strong>409, not 403.</strong> The caller owns it and holds every permission over it —
     * what stands in the way is a state they can change, not an authority they lack.</p>
     *
     * @param held the refusal
     * @return the problem detail
     */
    @ExceptionHandler(FileHeldException.class)
    public ProblemDetail handleHeldFile(FileHeldException held) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, held.getMessage());
    }

    /**
     * 🌐 A file could not be fetched from the address it was asked for.
     *
     * <p>⚠️ <strong>400, not 502.</strong> Every reason is the caller's address or the remote server's
     * behaviour — a gateway error would say this installation is broken, which sends whoever reads it to
     * the wrong place entirely.</p>
     *
     * @param unreachable the refusal
     * @return the problem detail
     */
    @ExceptionHandler(RemoteFetchException.class)
    public ProblemDetail handleUnreachableSource(RemoteFetchException unreachable) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, unreachable.getMessage());
    }

    /**
     * 📏 The file is real and readable, and reading it whole is more than the caller asked to be handed.
     *
     * @param tooLarge the refusal
     * @return the problem detail
     */
    @ExceptionHandler(ManagedFileTooLargeException.class)
    public ProblemDetail handleFileTooLarge(ManagedFileTooLargeException tooLarge) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, tooLarge.getMessage());
    }
}
