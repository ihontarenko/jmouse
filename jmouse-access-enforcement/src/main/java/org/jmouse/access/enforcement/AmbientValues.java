package org.jmouse.access.enforcement;

import org.jmouse.access.spi.DeferredValue;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

/**
 * The two readings of a set of {@link AmbientAccessValues} — their names, and their values.
 *
 * <p>Both come from calling {@code publish} against a different {@link AmbientPublication}, which is
 * why the registration seam is worth having: <strong>the names are answerable without working any
 * value out</strong>, so a catalogue can be built at startup from the same declaration that serves
 * the request.
 */
public final class AmbientValues {

    private AmbientValues() {
    }

    /**
     * Every name these publishers attach — asked once, at startup.
     *
     * <p>⚠️ Suppliers are registered and never called here. An implementation whose {@code publish}
     * body reads something would fail at boot, which is exactly where that mistake should surface.
     */
    public static Set<String> namesOf(Collection<AmbientAccessValues> publishers) {
        Names names = new Names();

        publishers.forEach(publisher -> publisher.publish(names));

        return Set.copyOf(names.collected);
    }

    /**
     * The bag for one call: ordinary values worked out, deferred ones still folded up.
     *
     * <p>Nothing here resolves. A {@link DeferredValue} travels through the context scope and the
     * condition context as an ordinary reference, and is unwrapped by whoever finally reads the name
     * — which, for a condition, is a reader that knows which names the rule mentions and skips the
     * rest.
     */
    public static Map<String, Object> publishedBy(Collection<AmbientAccessValues> publishers) {
        Bag bag = new Bag();

        publishers.forEach(publisher -> publisher.publish(bag));

        return bag.attached;
    }

    /** One publisher's worth, for the common case of exactly one. */
    public static Map<String, Object> publishedBy(AmbientAccessValues publisher) {
        return publishedBy(List.of(publisher));
    }

    /** Collects names and discards everything else. */
    private static final class Names implements AmbientPublication {

        private final Set<String> collected = new TreeSet<>();

        @Override
        public AmbientPublication attach(String name, Object value) {
            collected.add(name);
            return this;
        }

        @Override
        public AmbientPublication attachLazy(String name, Supplier<Object> value) {
            collected.add(name);
            return this;
        }
    }

    /** Collects what one call publishes, deferring what was registered as deferred. */
    private static final class Bag implements AmbientPublication {

        private final Map<String, Object> attached = new LinkedHashMap<>();

        @Override
        public AmbientPublication attach(String name, Object value) {
            attached.put(name, value);
            return this;
        }

        @Override
        public AmbientPublication attachLazy(String name, Supplier<Object> value) {
            attached.put(name, DeferredValue.of(value));
            return this;
        }
    }
}
