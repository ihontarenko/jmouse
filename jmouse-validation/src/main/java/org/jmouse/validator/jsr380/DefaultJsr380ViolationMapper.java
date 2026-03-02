package org.jmouse.validator.jsr380;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import org.jmouse.util.Strings;
import org.jmouse.validator.Errors;

/**
 * Default implementation of {@link Jsr380ViolationMapper} that converts Jakarta Validation
 * {@link ConstraintViolation} into jMouse {@link Errors}. 🔁
 *
 * <p>
 * Mapping rules:
 * </p>
 * <ul>
 *   <li><b>Field path</b>: derived from {@link ConstraintViolation#getPropertyPath()} and converted
 *       into dot-notation with optional index/key segments (e.g. {@code "items[0].name"}).</li>
 *   <li><b>Error code</b>: derived from the constraint annotation simple name
 *       (e.g. {@code NotBlank}, {@code Size}) and normalized to underscored form
 *       (e.g. {@code "not_blank"}, {@code "size"}).</li>
 *   <li><b>Message</b>: taken from {@link ConstraintViolation#getMessage()}.</li>
 *   <li>If the path is blank → registers a global error via {@link Errors#reject(String, String)}.</li>
 *   <li>Otherwise → registers a field error via {@link Errors#rejectValue(String, String, String)}.</li>
 * </ul>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * // Violation: @NotBlank on "email" with message "must not be blank"
 * // Produces:
 * // errors.rejectValue("email", "not_blank", "must not be blank");
 * }</pre>
 *
 * <p>
 * Notes:
 * </p>
 * <ul>
 *   <li>Iterable nodes are supported: index and map key are rendered in {@code [...]}.</li>
 *   <li>Null violations / errors are ignored.</li>
 * </ul>
 */
public final class DefaultJsr380ViolationMapper implements Jsr380ViolationMapper {

    /**
     * Maps a single Jakarta Validation violation into {@link Errors}.
     *
     * @param violation constraint violation (nullable)
     * @param errors    error collector (nullable)
     */
    @Override
    public void apply(ConstraintViolation<?> violation, Errors errors) {
        if (violation == null || errors == null) {
            return;
        }

        String path    = toPathString(violation.getPropertyPath());
        String message = violation.getMessage();

        // code: use annotation simple name, e.g. NotBlank, Size, EnumPattern
        String code = violation.getConstraintDescriptor()
                .getAnnotation()
                .annotationType()
                .getSimpleName();

        code = Strings.underscored(code, true);

        if (path.isBlank()) {
            errors.reject(code, message);
        } else {
            errors.rejectValue(path, code, message);
        }
    }

    /**
     * Converts a {@link Path} into a dot-separated string.
     *
     * <p>
     * Examples:
     * </p>
     * <ul>
     *   <li>{@code user.email} → {@code "user.email"}</li>
     *   <li>{@code items[0].name} → {@code "items[0].name"}</li>
     *   <li>{@code map[key].value} → {@code "map[key].value"}</li>
     * </ul>
     *
     * @param path Jakarta validation property path
     * @return normalized string path (never {@code null})
     */
    private static String toPathString(Path path) {
        if (path == null) {
            return "";
        }

        StringBuilder buffer = new StringBuilder();

        for (Path.Node node : path) {
            if (node == null) {
                continue;
            }

            String name = node.getName();

            if (name == null) {
                continue;
            }

            if (!buffer.isEmpty()) {
                buffer.append('.');
            }

            buffer.append(name);

            // indexed/iterable support
            if (node.getIndex() != null) {
                buffer.append('[').append(node.getIndex()).append(']');
            } else if (node.getKey() != null) {
                buffer.append('[').append(node.getKey()).append(']');
            }
        }

        return buffer.toString();
    }
}