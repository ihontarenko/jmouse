package org.jmouse.template;

import java.util.HashMap;
import java.util.Map;

abstract public class AbstractExtensionContainer<E extends NameKeeper> implements ExtensionContainer<E> {

    private final Map<String, E> extensions;

    protected AbstractExtensionContainer() {
        this.extensions = new HashMap<>();
    }

    @Override
    public void register(E extension) {
        extensions.put(extension.getName(), extension);
    }

    @Override
    public void remove(E extension) {
        if (contains(extension)) {
            extensions.remove(extension.getName());
        }
    }

    @Override
    public E get(String name) {
        return extensions.get(name);
    }

}
