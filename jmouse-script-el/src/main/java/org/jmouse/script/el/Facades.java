package org.jmouse.script.el;

import org.jmouse.core.context.beans.BeanLookup;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The objects a document may reach as {@code @name.method(…)}: a closed set, resolved by name and never by type.
 *
 * <p>Installed into an evaluation context as its bean lookup, this is how every dialect on the statement core
 * reaches its host — a configuration, a data source, a clock — through the context and nothing else. The set is
 * what the host chose to give; a name outside it is refused with the names that do exist.</p>
 *
 * <p>⚠️ <strong>Resolution by type is refused outright.</strong> A document that could ask for "any bean of this
 * class" could reach whatever a container holds; naming is what keeps the boundary visible.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class Facades implements BeanLookup {

    private final Map<String, Object> facades;

    private Facades(Map<String, Object> facades) {
        this.facades = Map.copyOf(facades);
    }

    public static Facades of(Map<String, Object> facades) {
        return new Facades(facades);
    }

    public static Facades none() {
        return new Facades(Map.of());
    }

    public Set<String> names() {
        return new TreeSet<>(facades.keySet());
    }

    @Override
    public <T> T getBean(Class<T> beanClass) {
        throw new FacadeAccessException("a document resolves facades by name, never by type; nothing can ask for '%s'"
                .formatted(beanClass == null ? "null" : beanClass.getName()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getBean(String beanName, Class<T> beanClass) {
        Object facade = facades.get(beanName);
        if (facade == null) {
            throw new FacadeAccessException("'@%s' is not a facade this host gives; it gives %s".formatted(beanName, names()));
        }
        return (T) facade;
    }
}
