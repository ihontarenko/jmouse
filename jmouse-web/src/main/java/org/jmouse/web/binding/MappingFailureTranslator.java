package org.jmouse.web.binding;

import org.jmouse.core.access.PropertyPath;
import org.jmouse.core.mapping.errors.MappingException;
import org.jmouse.validator.Errors;

public interface MappingFailureTranslator {
    void translate(MappingException exception, PropertyPath path, Errors errors);
}
