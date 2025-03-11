package org.jmouse.template;

/**
 * 🎛️ A container for managing extensions. This interface provides methods for registering,
 * removing, retrieving, and checking extensions by their associated keys.
 *
 * @param <K> 🔑 the type of the key used to identify extensions
 * @param <E> 🧩 the type of the extension being managed
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface ExtensionContainer<K, E> {

    /**
     * ➕ Registers a new extension in the container.
     *
     * @param extension 🛠️ the extension to register
     */
    void register(E extension);

    /**
     * ❌ Removes an extension from the container.
     *
     * @param extension 🗑️ the extension to remove
     */
    void remove(E extension);

    /**
     * 🔍 Retrieves an extension by its key.
     *
     * @param name 🔑 the key associated with the extension
     * @return 🧩 the extension corresponding to the key, or {@code null} if not found
     */
    E get(K name);

    /**
     * 🏷️ Retrieves the key associated with a given extension.
     *
     * @param extension 🧩 the extension to retrieve the key for
     * @return 🔑 the key associated with the extension
     */
    K key(E extension);

    /**
     * ✅ Checks whether the given extension is present in the container.
     *
     * @param extension 🧩 the extension to check
     * @return 🔄 {@code true} if the extension is present, otherwise {@code false}
     */
    default boolean contains(E extension) {
        return get(key(extension)) != null;
    }
}
