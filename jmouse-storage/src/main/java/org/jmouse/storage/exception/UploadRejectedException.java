package org.jmouse.storage.exception;

/**
 * ⛔ Content was refused by the acceptance policy and was never written.
 *
 * <p>Carrying its own type is what lets a product answer {@code 400} for a rejected upload while
 * still answering {@code 500} for a genuine storage failure.</p>
 */
public class UploadRejectedException extends StorageException {

    /**
     * 🏗️ Report rejected content.
     *
     * @param message the first rule the content broke
     */
    public UploadRejectedException(String message) {
        super(message);
    }
}
