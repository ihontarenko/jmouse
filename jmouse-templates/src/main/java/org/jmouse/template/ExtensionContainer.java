package org.jmouse.template;

public interface ExtensionContainer<E extends NameKeeper> {

    void register(E extension);

    void remove(E extension);

    E get(String name);

    default boolean contains(E extension) {
        return get(extension.getName()) != null;
    }

}
