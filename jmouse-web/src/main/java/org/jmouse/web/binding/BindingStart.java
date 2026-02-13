package org.jmouse.web.binding;

public record BindingStart(
        Object sourceRoot,
        Object targetRoot
) {

    public static BindingStart empty() {
        return new BindingStart(null, null);
    }

}