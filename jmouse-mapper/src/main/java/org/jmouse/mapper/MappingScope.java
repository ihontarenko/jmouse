package org.jmouse.mapper;

import org.jmouse.core.access.PropertyPath;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Mapping scope holder that tracks the root source object and the current property path. 🧭
 *
 * <p>{@code MappingScope} is used to propagate contextual information through nested mapping calls,
 * primarily for diagnostics and error reporting.</p>
 *
 * <p>The scope holds no target: which object a value is on its way into changes with every nested
 * call and is described by {@link MappingDestination}, which is handed to plugins alongside the
 * value itself.</p>
 *
 * <p>The scope is immutable: {@link #withPath(PropertyPath)} and {@link #append(String)} return new
 * instances.</p>
 *
 * <h3>⚠️ The path is not built until somebody asks for it</h3>
 * <p>A scope is created for every property of every mapped object, purely so that <em>if</em> something
 * fails the failure can say {@code customer.profile.fullName}. Building a {@link PropertyPath} eagerly
 * meant parsing the segment, looking it up in a cache and merging two entry tables - per property, per
 * object, on the overwhelmingly common path where nothing fails and no plugin is listening. Profiling
 * put that at roughly a fifth of the engine's time.</p>
 *
 * <p>So a nested scope holds its parent and its own segment, and materializes the path on the first
 * {@link #path()} - which is a failure being located, a plugin being handed a value, or somebody
 * asking outright. Depth is counted as it goes, because the only caller that wanted the path in order
 * to measure it was the depth guard.</p>
 *
 * <p>The memo is a plain field and not volatile. A scope belongs to one mapping invocation and never
 * leaves the thread performing it, so there is no publication to arrange.</p>
 */
public final class MappingScope {

    private final Object              sourceRoot;
    private final Map<Object, Object> inProgress;
    private final MappingScope        parent;
    private final String              segment;
    private final int                 depth;

    private PropertyPath path;

    private MappingScope(
            Object sourceRoot,
            Map<Object, Object> inProgress,
            MappingScope parent,
            String segment,
            int depth,
            PropertyPath path
    ) {
        this.sourceRoot = sourceRoot;
        this.inProgress = inProgress;
        this.parent = parent;
        this.segment = segment;
        this.depth = depth;
        this.path = path;
    }

    /**
     * Create a root scope for the given root source.
     *
     * @param sourceRoot root source object (may be {@code null})
     * @return root mapping scope with {@link PropertyPath#empty()}
     */
    public static MappingScope root(Object sourceRoot) {
        return new MappingScope(
                sourceRoot, new IdentityHashMap<>(4), null, null, 0, PropertyPath.empty());
    }

    /**
     * Root source object of the mapping invocation.
     *
     * @return the object the whole invocation started from, or {@code null}
     */
    public Object sourceRoot() {
        return sourceRoot;
    }

    /**
     * Source objects currently being mapped, keyed by identity, each holding the target being built
     * for it.
     *
     * <p>⚠️ Carried by <em>reference</em> into every nested scope, deliberately. Everything else about
     * a scope is copied per nested call; this one thing has to be shared, because the question it
     * answers - "am I already inside this object?" - is about the whole invocation and not about one
     * step of it. It is written and read on a single thread, the one doing the mapping, and it never
     * leaves the invocation that created it.</p>
     *
     * @return the ancestor chain of objects being mapped
     */
    public Map<Object, Object> inProgress() {
        return inProgress;
    }

    /**
     * How deep this scope sits, counted in nested mapping steps.
     *
     * <p>⚠️ This counts <em>steps</em>, which is not always the number of segments in the path: one
     * step named by a map key that itself contains a dot contributes two segments. Steps are what the
     * depth limit is about, and they are also free to count.</p>
     *
     * @return number of nested steps taken to reach here
     */
    public int depth() {
        return depth;
    }

    /**
     * The path reached so far, built on first request.
     *
     * @return current property path
     */
    public PropertyPath path() {
        PropertyPath resolved = path;

        if (resolved == null) {
            resolved = parent.path().append(segment);
            path = resolved;
        }

        return resolved;
    }

    /**
     * Create a new scope carrying the given property path.
     *
     * @param path property path to carry
     * @return new scope instance
     */
    public MappingScope withPath(PropertyPath path) {
        return new MappingScope(sourceRoot, inProgress, null, null, path.size(), path);
    }

    /**
     * Append a single segment to the current property path.
     *
     * @param segment path segment to append
     * @return new scope instance, whose path is not built until it is asked for
     */
    public MappingScope append(String segment) {
        return new MappingScope(sourceRoot, inProgress, this, segment, depth + 1, null);
    }

    @Override
    public String toString() {
        return "MappingScope: '%s' at depth %d".formatted(path(), depth);
    }
}
