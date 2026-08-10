package org.jmouse.el;

import org.jmouse.core.Sorter;

import java.util.*;

/**
 * 🏗️ Abstract implementation of {@link ObjectContainer}, providing a basic storage mechanism
 * for managing extensions using a key-value structure.
 *
 * @param <K> 🔑 the type of the key used to identify extensions
 * @param <E> 🧩 the type of the extension being managed
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public abstract class AbstractObjectContainer<K, E> implements ObjectContainer<K, E> {

    /**
     * 🗂️ Stores registered extensions mapped by their corresponding keys.
     */
    private final Map<K, E> extensions;

    private List<E> sortedValues;

    /**
     * 🏗️ Constructs an empty extension container.
     */
    protected AbstractObjectContainer() {
        this.extensions = new HashMap<>();
    }

    /**
     * ➕ Registers a new extension into the container.
     *
     * @param extension 🛠️ the extension to register
     */
    @Override
    public void register(E extension) {
        extensions.put(keyFor(extension), extension);
    }

    /**
     * ❌ Removes an extension from the container if it exists.
     *
     * @param extension 🗑️ the extension to remove
     */
    @Override
    public void remove(E extension) {
        if (contains(extension)) {
            extensions.remove(keyFor(extension));
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

    /**
     * 📜 Returns a list of all stored objects in the container.
     *
     * @return 📦 a list of registered objects
     */
    @Override
    public List<E> values() {
        List<E> values = List.copyOf(extensions.values());

        if (sortedValues == null) {
            sortedValues = new ArrayList<>(values);
            sortedValues.sort(Sorter.PRIORITY_COMPARATOR);
        }

        return sortedValues;
    }

    /**
     * 🔑 Returns a set of all keys currently registered in the container.
     *
     * @return 🗂️ a set of all keys
     */
    @Override
    public Set<K> keys() {
        return extensions.keySet();
    }

    @Override
    public String toString() {
        return "[%d]".formatted(keys().size());
    }
}
