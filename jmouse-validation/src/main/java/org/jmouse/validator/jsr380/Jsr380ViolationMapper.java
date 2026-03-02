package org.jmouse.validator.jsr380;

import jakarta.validation.ConstraintViolation;
import org.jmouse.validator.Errors;

/**
 * Strategy interface for mapping Jakarta Validation
 * {@link ConstraintViolation} instances into jMouse {@link Errors}. 🔄
 *
 * <p>
 * This abstraction allows decoupling Bean Validation from the internal
 * error representation used by the jMouse validation layer.
 * </p>
 *
 * <h3>Typical responsibilities</h3>
 * <ul>
 *     <li>Extract property path from {@link ConstraintViolation#getPropertyPath()}</li>
 *     <li>Extract message from {@link ConstraintViolation#getMessage()}</li>
 *     <li>Register field/global errors in {@link Errors}</li>
 *     <li>Optionally enrich with rejected value or error codes</li>
 * </ul>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * public final class DefaultMapper implements Jsr380ViolationMapper {
 *
 *     @Override
 *     public void apply(ConstraintViolation<?> violation, Errors errors) {
 *         String field = violation.getPropertyPath().toString();
 *         String message = violation.getMessage();
 *
 *         errors.rejectValue(field, "validation", message);
 *     }
 * }
 * }</pre>
 */
public interface Jsr380ViolationMapper {

    /**
     * Maps a single constraint violation into the provided {@link Errors}.
     *
     * @param violation constraint violation reported by Jakarta Validation
     * @param errors    error collector
     */
    void apply(ConstraintViolation<?> violation, Errors errors);

}