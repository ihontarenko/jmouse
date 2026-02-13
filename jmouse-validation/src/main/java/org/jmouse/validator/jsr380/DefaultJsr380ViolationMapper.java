package org.jmouse.validator.jsr380;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import org.jmouse.validator.Errors;

public final class DefaultJsr380ViolationMapper implements Jsr380ViolationMapper {

    @Override
    public void apply(ConstraintViolation<?> violation, Errors errors) {
        if (violation == null || errors == null) {
            return;
        }

        String fieldPath = toPathString(violation.getPropertyPath());
        String message   = violation.getMessage();

        // code: use annotation simple name, e.g. NotBlank, Size, EnumPattern
        String code = violation.getConstraintDescriptor()
                .getAnnotation()
                .annotationType()
                .getSimpleName();

        if (fieldPath.isBlank()) {
            errors.reject(code, message);
        } else {
            errors.rejectValue(fieldPath, code, message);
        }
    }

    private static String toPathString(Path path) {
        if (path == null) {
            return "";
        }

        StringBuilder buffer = new StringBuilder();

        for (Path.Node node : path) {
            if (node == null) continue;

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
