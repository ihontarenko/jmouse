package org.jmouse.core.context.result;

/**
 * ❌ Error descriptor.
 *
 * <p>
 * Lightweight, immutable error representation
 * carrying a machine-readable code and
 * a human-readable message.
 * </p>
 *
 * <p>
 * {@code cause} is optional and may be {@code null}.
 * </p>
 *
 * @param code    error code (stable identifier)
 * @param message error message (human-readable)
 * @param cause   underlying cause (optional)
 */
public record ErrorDetails(String code, String message, Throwable cause) {

    /**
     * 🏗 Create error without cause.
     *
     * @param code    error code
     * @param message error message
     */
    public ErrorDetails(String code, String message) {
        this(code, message, null);
    }
}
