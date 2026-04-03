package org.jmouse.web.binding;

import org.jmouse.validator.Errors;

public record SimpleBindingResult<T>(T target, Errors errors) implements BindingResult<T> { }
