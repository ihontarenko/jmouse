package org.jmouse.web.binding;

import org.jmouse.validator.Errors;
import org.jmouse.validator.FieldError;
import org.jmouse.validator.ObjectError;

public final class ErrorsMerging {

    private ErrorsMerging() {
    }

    public static void merge(Errors target, Errors source) {
        if (target == null || source == null) {
            return;
        }

        for (ObjectError e : source.getGlobalErrors()) {
            target.reject(baseCode(e), e.getDefaultMessage(), e.getArguments());
        }

        for (FieldError e : source.getErrors()) {
            target.rejectValue(e.getField(), baseCode(e), e.getDefaultMessage(), e.getArguments());
        }
    }

    private static String baseCode(ObjectError error) {
        String[] codes = error.getCodes();
        return (codes == null || codes.length == 0)
                ? "validation.failed" : codes[codes.length - 1];
    }
}
