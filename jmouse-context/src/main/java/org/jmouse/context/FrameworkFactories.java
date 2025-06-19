package org.jmouse.context;

import org.jmouse.core.env.PropertySource;
import org.jmouse.core.reflection.Reflections;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 🧩 Maps interfaces to factory implementations via `.properties` files.
 *
 * Used for dynamic class resolution at bootstrap time. Expected format:
 * <pre>{@code
 * com.example.Service = com.example.ServiceImpl1, com.example.ServiceImpl2
 * }</pre>
 *
 * The key is an interface or abstract class. The value is a comma-separated list of implementing class names.
 * Classes are resolved reflectively and stored for DI/IoC purposes.
 *
 * Example usage:
 * <pre>{@code
 * FrameworkFactories props = FrameworkFactories.load(MyApp.class);
 * Set<Class<?>> impls = props.getFactories(MyService.class);
 * }</pre>
 *
 * 🔍 File name is derived from the passed class name, e.g. `MyApp` → `MyApp.properties`.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @see PropertySource
 */
public class FrameworkFactories {

    private final Map<Class<?>, Set<Class<?>>> factories = HashMap.newHashMap(8);

    /**
     * 📥 Load mapping file and parse factory class references.
     *
     * @param source base class to derive the `.properties` filename
     * @return parsed mapping of interfaces to their implementations
     */
    public static FrameworkFactories load(Class<?> source) {
        FrameworkFactories properties = new FrameworkFactories();

        if (source != null) {
            PropertySource<Map<String, Object>> values = PropertySource.loadProperties(source);

            for (String name : values.getPropertyNames()) {
                Class<?> type    = Reflections.getClassFor(name);
                String   classes = values.getProperty(name).toString();

                if (classes != null && !classes.isBlank()) {
                    Set<Class<?>> implementations = Arrays.stream(classes.split(",")).map(Reflections::getClassFor)
                            .collect(Collectors.toSet());

                    properties.addFactories(type, implementations);
                }
            }
        }

        return properties;
    }

    /**
     * 📦 Get factories for given interface/class.
     *
     * @param clazz type key
     * @return set of factories (never null)
     */
    public Set<Class<?>> getFactories(Class<?> clazz) {
        return factories.computeIfAbsent(clazz, k -> new HashSet<>());
    }

    /**
     * ➕ Add multiple factories.
     *
     * @param clazz interface key
     * @param factories implementations
     */
    public void addFactories(Class<?> clazz, Set<Class<?>> factories) {
        this.factories.computeIfAbsent(clazz, k -> new HashSet<>()).addAll(factories);
    }

    /**
     * ➕ Add single factory.
     *
     * @param clazz interface key
     * @param factory implementation
     */
    public void addFactory(Class<?> clazz, Class<?> factory) {
        addFactories(clazz, Collections.singleton(factory));
    }
}
