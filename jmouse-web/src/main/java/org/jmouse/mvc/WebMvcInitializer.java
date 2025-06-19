package org.jmouse.mvc;

/**
 * ⚙️ Initializes MVC-related objects during context bootstrapping.
 *
 * @param <T> the type of object to initialize (e.g. {@code HandlerMapping}, {@code HandlerAdapter})
 *
 * Used internally to post-process or register MVC components.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
public interface WebMvcInitializer<T> {

    /**
     * 🔧 Apply additional configuration to the target object.
     *
     * @param object instance to initialize
     */
    void initialize(T object);

    /**
     * 📦 Type this initializer applies to.
     *
     * @return class of the initialized type
     */
    Class<T> objectClass();
}
