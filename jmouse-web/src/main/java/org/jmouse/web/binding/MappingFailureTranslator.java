package org.jmouse.web.binding;

import org.jmouse.core.mapping.plugin.MappingFailure;
import org.jmouse.validator.Errors;

public interface MappingFailureTranslator {
    void translate(MappingFailure failure, Errors errors);
}
