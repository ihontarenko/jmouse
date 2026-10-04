package org.jmouse.script.stage;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * What a stage hands a rule: the names, and how to read each one from the event.
 *
 * <h2>⚠️ ONE SOURCE OF TRUTH FOR NAMES, AND IT IS WHY THIS IS NOT JUST A MAP</h2>
 *
 * <p>Two things need the names a stage publishes: the dispatcher, which must bind them at run time, and
 * the editor, which offers them for completion and puts them into a new document's starter body. Written
 * twice — once in a {@code bind} method, once in a list beside it — they drift, and the failure is
 * somebody writing a rule against a name the editor offered and the stage does not bind. That rule
 * loads, runs, reads nothing, and its guard silently answers false for ever.</p>
 *
 * <p>Holding the readers rather than the values means the names cannot disagree with the bindings,
 * because they <em>are</em> the bindings.</p>
 *
 * <h2>⚠️ NULL IS A VALUE HERE</h2>
 *
 * <p>{@code Map.of} refuses null, and every workaround for that is a branch that eventually gets written
 * as "leave the key out". A guard reading a name the host left out answers false rather than throwing —
 * so a dropped key produces rules that stop running under conditions nobody can see. Every declared name
 * is bound on every path, {@code null} included.</p>
 *
 * @param <E> the event this reads from
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class StageBinding<E> {

    private final Map<String, Function<E, Object>> readers = new LinkedHashMap<>();

    private StageBinding() {
    }

    /**
     * Starts a binding with its first name.
     *
     * @param name   what a rule reads
     * @param reader how to get it out of the event
     * @param <E>    the event type
     * @return the binding
     */
    public static <E> StageBinding<E> of(String name, Function<E, Object> reader) {
        return new StageBinding<E>().and(name, reader);
    }

    /**
     * Adds one more name.
     *
     * @param name   what a rule reads
     * @param reader how to get it out of the event
     * @return this binding
     */
    public StageBinding<E> and(String name, Function<E, Object> reader) {
        readers.put(name, reader);

        return this;
    }

    /**
     * Every name this stage publishes, in declaration order.
     *
     * <p>⚠️ Insertion-ordered rather than sorted, so a completion list shows the names in the order the
     * stage thought about them — identity first, then detail — which is the order somebody reads them
     * in.</p>
     *
     * @return the names
     */
    public List<String> names() {
        return List.copyOf(readers.keySet());
    }

    /**
     * Reads every name out of one event.
     *
     * @param event what happened
     * @return the values, by name, nulls included
     */
    public Map<String, Object> read(E event) {
        Map<String, Object> values = new LinkedHashMap<>();

        readers.forEach((name, reader) -> values.put(name, reader.apply(event)));

        return Collections.unmodifiableMap(values);
    }

}
