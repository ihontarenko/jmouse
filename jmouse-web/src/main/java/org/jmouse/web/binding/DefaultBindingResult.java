package org.jmouse.web.binding;

import org.jmouse.validator.Errors;

public record DefaultBindingResult<T>(T target, Errors errors) implements BindingResult<T> { }
