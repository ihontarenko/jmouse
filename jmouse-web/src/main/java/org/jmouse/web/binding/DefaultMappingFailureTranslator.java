package org.jmouse.web.binding;

import org.jmouse.core.mapping.plugin.MappingFailure;
import org.jmouse.validator.Errors;

public final class DefaultMappingFailureTranslator implements MappingFailureTranslator {

    public static final String MAPPING_FAILED_MESSAGE = "MAPPING FAILED!";
    public static final String MAPPING_FAILED_CODE    = "mapping.failed";

    @Override
    public void translate(MappingFailure failure, Errors errors) {
        if (failure == null || errors == null) {
            return;
        }

        String code    = failure.error() == null ? MAPPING_FAILED_CODE : failure.error().code();
        String message = failure.error() == null ? MAPPING_FAILED_MESSAGE : failure.error().getMessage();
        String path    = failure.path() == null ? "" : failure.path().path();

        if (path.isBlank()) {
            errors.reject(code, message);
        } else {
            errors.rejectValue(path, code, message);
        }
    }
}
