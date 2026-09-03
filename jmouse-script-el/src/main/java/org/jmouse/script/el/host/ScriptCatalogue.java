package org.jmouse.script.el.host;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Everything a host is willing to let a script reach — <strong>the closed list, and the whole of it</strong>.
 *
 * <h2>⚠️ Closed is the entire point</h2>
 *
 * <p>{@code .jmp} solves the same problem the other way round: it <em>strips</em> {@code @bean.method}
 * from a policy, so a rule cannot call {@code @userRepository.deleteAll()}. A script has to call the
 * host — that is what a script is for — so jMS keeps the syntax and closes the set of names instead.</p>
 *
 * <p>Which means this object is the security boundary. A catalogue that resolves a name it was never
 * given, or that falls back to an application container when it misses, is remote code execution with a
 * grammar in front of it — and it looks identical to one that does not. There is no fallback anywhere
 * in this package, and there is not going to be one.</p>
 *
 * <p>An empty catalogue is legal. A host that has declared nothing yet gets a document that binds to
 * nothing, which is a better answer than a document that binds to everything.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class ScriptCatalogue {

    private static final ScriptCatalogue EMPTY = builder().build();

    private final Map<String, Object> facades;
    private final Set<String>         events;
    private final Set<String>         functions;

    private ScriptCatalogue(Map<String, Object> facades, Set<String> events, Set<String> functions) {
        this.facades = Map.copyOf(facades);
        this.events = Set.copyOf(events);
        this.functions = Set.copyOf(functions);
    }

    /**
     * A catalogue declaring nothing.
     *
     * @return the empty catalogue
     */
    public static ScriptCatalogue empty() {
        return EMPTY;
    }

    /**
     * Starts building one.
     *
     * @return a fresh builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the object an {@code @name} resolves to.
     *
     * @param name the facade name, without the {@code @}
     * @return the object, or {@code null} when this catalogue declares no such facade
     */
    public Object facade(String name) {
        return facades.get(name);
    }

    /**
     * Whether {@code @name} may appear in a script at all.
     *
     * @param name the facade name, without the {@code @}
     * @return {@code true} when the host declared it
     */
    public boolean declaresFacade(String name) {
        return facades.containsKey(name);
    }

    /**
     * Whether a handler may be written against an event.
     *
     * @param name the event name
     * @return {@code true} when the host declared it
     */
    public boolean declaresEvent(String name) {
        return events.contains(name);
    }

    /**
     * Whether an expression may call a function by this name.
     *
     * @param name the function name
     * @return {@code true} when the host declared it
     */
    public boolean declaresFunction(String name) {
        return functions.contains(name);
    }

    /**
     * Returns the facade names, for a message that has to list what <em>is</em> available.
     *
     * @return the declared facade names
     */
    public Set<String> facadeNames() {
        return facades.keySet();
    }

    /**
     * Returns the event names.
     *
     * @return the declared event names
     */
    public Set<String> eventNames() {
        return events;
    }

    /**
     * Returns the function names.
     *
     * @return the declared function names
     */
    public Set<String> functionNames() {
        return functions;
    }

    /**
     * Collects what a host declares.
     *
     * <p>Insertion-ordered, so a refusal listing what was available reads in the order the host wrote
     * it rather than in whatever order a hash produced.</p>
     */
    public static final class Builder {

        private final Map<String, Object> facades   = new LinkedHashMap<>();
        private final Set<String>         events    = new LinkedHashSet<>();
        private final Set<String>         functions = new LinkedHashSet<>();

        private Builder() {
        }

        /**
         * Declares a facade — a named object an {@code @} call may reach.
         *
         * @param name  what a script writes after the {@code @}
         * @param bean  the object its methods are invoked on
         * @return this builder
         */
        public Builder facade(String name, Object bean) {
            facades.put(name, bean);
            return this;
        }

        /**
         * Declares an event a handler may be written against.
         *
         * @param name the event name
         * @return this builder
         */
        public Builder event(String name) {
            events.add(name);
            return this;
        }

        /**
         * Declares a function an expression may call.
         *
         * @param name the function name
         * @return this builder
         */
        public Builder function(String name) {
            functions.add(name);
            return this;
        }

        /**
         * Builds the catalogue.
         *
         * @return an immutable catalogue
         */
        public ScriptCatalogue build() {
            return new ScriptCatalogue(facades, events, functions);
        }
    }
}
