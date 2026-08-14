package org.jmouse.access.enforcement;

import org.jmouse.access.VariableCatalog;
import org.jmouse.access.VariableKind;
import org.jmouse.access.spi.DeferredValue;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;

/**
 * The readings of a set of {@link AmbientAccessValues} — their names, their kinds, and their values.
 *
 * <p>All of them come from calling {@code publish} against a different {@link AmbientPublication},
 * which is why the registration seam is worth having: <strong>the names are answerable without
 * working any value out</strong>, so a catalogue can be built at startup from the same declaration
 * that serves the request.
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
        return kindsOf(publishers).keySet();
    }

    /**
     * The same names, each with the one thing a reader of a rule needs to know about it.
     *
     * <p>⚠️ <strong>The kind is read off how the value was registered, never declared twice.</strong>
     * {@code attach} is a value already in hand and {@code attachLazy} is one that has to be worked
     * out, which is exactly the {@code constant} / {@code dynamic} distinction a policy file writes —
     * so a file claiming the wrong one can be refused without anybody maintaining a second list for it
     * to be refused against.
     *
     * @return every attached name, in a stable order, mapped to where its value comes from
     */
    public static Map<String, VariableKind> kindsOf(Collection<AmbientAccessValues> publishers) {
        Kinds kinds = new Kinds();

        publishers.forEach(publisher -> publisher.publish(kinds));

        return Map.copyOf(kinds.collected);
    }

    /**
     * The same reading, as the catalogue a policy file is checked against.
     *
     * <p>⚠️ <strong>Here rather than in each product, deliberately.</strong> The mapping from
     * "how it was registered" to "what the file must call it" is one fact, and a product that built
     * its own catalogue would be restating it — which is the shape of every drift this whole seam
     * exists to prevent.
     *
     * @param publishers everything attaching values to a decision, or empty where nothing does
     */
    public static VariableCatalog catalogOf(Collection<AmbientAccessValues> publishers) {
        return VariableCatalog.of(kindsOf(publishers));
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

    /** Collects names and how each was registered, and discards the values themselves. */
    private static final class Kinds implements AmbientPublication {

        private final Map<String, VariableKind> collected = new TreeMap<>();

        @Override
        public AmbientPublication attach(String name, Object value) {
            collected.put(name, VariableKind.CONSTANT);
            return this;
        }

        @Override
        public AmbientPublication attachLazy(String name, Supplier<Object> value) {
            collected.put(name, VariableKind.DYNAMIC);
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
