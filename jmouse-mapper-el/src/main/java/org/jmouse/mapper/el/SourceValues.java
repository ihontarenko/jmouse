package org.jmouse.mapper.el;

import org.jmouse.core.access.descriptor.structured.ObjectDescriptor;
import org.jmouse.core.access.descriptor.structured.PropertyDescriptor;
import org.jmouse.el.evaluation.ScopeValues;

import java.util.HashMap;
import java.util.Map;

/**
 * One mapped object, seen by an expression as a scope of names — read when a name is asked for. 🔍
 *
 * <h2>⚠️ Why lazily, and what it replaces</h2>
 *
 * <p>Every computed rule used to be evaluated against a context that had been filled by reading
 * <strong>every readable property of the source</strong> first. That is not once per object: it is once
 * per computed rule per object, because each rule hands the engine its own lambda and each lambda built
 * its own context. A thirteen-property source with four computed rules read fifty-two properties to serve
 * the handful the expressions actually named, and allocated four contexts to hold them.</p>
 *
 * <p>Nothing about the language needs that. An expression names two or three roots; the rest were read
 * so that they would be there if it had asked. So this reads one when it is asked for, and the sweep is
 * gone.</p>
 *
 * <h2>⚠️ Written names shadow the object, and that is the whole write path</h2>
 *
 * <p>A scope is not read-only: {@code let} bindings, and a lambda's parameter where no scope was pushed
 * for it, are set by name. Those go into an overlay that is consulted first — so a name somebody set
 * means what they set it to, and every other name means the object's property. Two sources of one name
 * is exactly the ambiguity the language refuses at load time, so in practice they never collide.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
final class SourceValues implements ScopeValues {

    /**
     * The name an expression may write to reach the whole object.
     *
     * <p>⚠️ Answered here rather than set into the scope. Setting it would put it in the overlay, which
     * is fine — until a source has a property of the same name, at which point which one wins depends on
     * the order two unrelated lines happened to run in.</p>
     */
    private static final String SOURCE = "source";

    private final Object                   source;
    private final ObjectDescriptor<Object> descriptor;
    private final Map<String, Object>      written = new HashMap<>();

    SourceValues(Object source, ObjectDescriptor<Object> descriptor) {
        this.source = source;
        this.descriptor = descriptor;
    }

    /**
     * {@inheritDoc}
     *
     * <p>⚠️ Only what has been written, never the object's own properties. Enumerating those is the sweep
     * this class exists to remove, and nothing on the mapping path asks: the one caller in the expression
     * language is {@code ScopedChain.merge}, which a mapping never performs.</p>
     */
    @Override
    public Map<String, Object> getValues() {
        return written;
    }

    @Override
    public Object get(String name) {
        if (written.containsKey(name)) {
            return written.get(name);
        }

        if (SOURCE.equals(name)) {
            return source;
        }

        PropertyDescriptor<Object> property = descriptor.getProperty(name);

        if (property == null || !property.isReadable()) {
            return null;
        }

        return property.getAccessor().readValue(source);
    }

    @Override
    public void set(String name, Object value) {
        written.put(name, value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>⚠️ Answered without reading the property. The inherited default is {@code get(name) != null},
     * which would read the object once to decide whether to read it again — and would answer "no" for a
     * property that is present and holds {@code null}, which is a different thing from absent.</p>
     */
    @Override
    public boolean contains(String name) {
        return written.containsKey(name) || SOURCE.equals(name) || descriptor.getProperty(name) != null;
    }
}
