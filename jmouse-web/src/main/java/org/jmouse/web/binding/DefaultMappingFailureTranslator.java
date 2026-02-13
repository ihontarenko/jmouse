package org.jmouse.web.binding;

import org.jmouse.core.access.PropertyPath;
import org.jmouse.core.mapping.errors.MappingException;
import org.jmouse.core.mapping.plugin.MappingFailure;
import org.jmouse.validator.Errors;

public final class DefaultMappingFailureTranslator implements MappingFailureTranslator {

    public static final String MAPPING_FAILED_MESSAGE = "MAPPING FAILED!";
    public static final String MAPPING_FAILED_CODE    = "mapping.failed";

    @Override
    public void translate(MappingException exception, PropertyPath path, Errors errors) {
        if (errors == null) {
            return;
        }

        String code    = exception == null ? MAPPING_FAILED_CODE : exception.code();
        String message = exception == null ? MAPPING_FAILED_MESSAGE : exception.getMessage();

        String p = path == null ? "" : path.path();

        if (p.isBlank()) {
            errors.reject(code, message);
        } else {
            errors.rejectValue(p, code, message);
        }
    }
}
