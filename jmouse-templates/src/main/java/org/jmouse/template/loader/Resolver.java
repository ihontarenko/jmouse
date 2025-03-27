package org.jmouse.template.loader;

public interface Resolver<I, O> {
    O resolve(I input);
}
