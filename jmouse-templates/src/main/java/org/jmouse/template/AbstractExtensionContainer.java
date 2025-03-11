package org.jmouse.template;

import java.util.HashMap;
import java.util.Map;

/**
 * 🏗️ Abstract implementation of {@link ExtensionContainer}, providing a basic storage mechanism
 * for managing extensions using a key-value structure.
 *
 * @param <K> 🔑 the type of the key used to identify extensions
 * @param <E> 🧩 the type of the extension being managed
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public abstract class AbstractExtensionContainer<K, E> implements ExtensionContainer<K, E> {

    /**
     * 🗂️ Stores registered extensions mapped by their corresponding keys.
     */
    private final Map<K, E> extensions;

    /**
     * 🏗️ Constructs an empty extension container.
     */
    protected AbstractExtensionContainer() {
        this.extensions = new HashMap<>();
    }

    /**
     * ➕ Registers a new extension into the container.
     *
     * @param extension 🛠️ the extension to register
     */
    @Override
    public void register(E extension) {
        extensions.put(key(extension), extension);
    }

    /**
     * ❌ Removes an extension from the container if it exists.
     *
     * @param extension 🗑️ the extension to remove
     */
    @Override
    public void remove(E extension) {
        if (contains(extension)) {
            extensions.remove(key(extension));
        }
    }

    /**
     * 🔍 Retrieves an extension by its key.
     *
     * @param name 🔑 the key associated with the extension
     * @return 🧩 the extension corresponding to the key, or {@code null} if not found
     */
    @Override
    public E get(K name) {
        return extensions.get(name);
    }
}
