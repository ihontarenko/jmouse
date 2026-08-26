package org.jmouse.mapper.el;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Whether a rule's right-hand side is a bare path read straight off the source. 🛣️
 *
 * <h2>⚠️ One question, and it is asked from two places that used to answer it differently</h2>
 *
 * <p>The binder asks it to decide whether a rule can be a {@link org.jmouse.mapper.binding.PropertyMapping.Reference}
 * — resolved through the source accessor, with no expression tree, no evaluation context and no property
 * sweep. The validator asks it to decide whether the value is a name it may check against the source's
 * readable properties.</p>
 *
 * <p>They are the same question, and while each carried its own copy of the test they gave different
 * answers: the binder treated any block with a {@code let} in it as unable to hold a bare path, and the
 * validator did not know bindings existed at all — so a rule reading a binding by name was refused by one
 * and handled correctly by the other. The language's own documented example was the casualty.</p>
 *
 * <h2>⚠️ Why the binding names are part of the question rather than a separate check</h2>
 *
 * <p>{@code buyerName : full} is a bare path by every syntactic measure, and it does not read the source
 * at all — {@code full} is a name the block bound. Nothing about the text says which; only the block's
 * bindings do. So the test takes them, and the two sets are guaranteed disjoint because a binding may not
 * shadow a readable source property (that is refused when the file is read), which is what makes this
 * exact rather than a heuristic.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class SourcePath {

    /**
     * A value that is only a path — {@code id}, {@code buyer.email}, {@code total_amount}.
     *
     * <p>⚠️ Deliberately syntactic and nothing more. Anything carrying an operator, a call or a filter is
     * an expression, and its bare words are not necessarily properties of anything.</p>
     */
    private static final Pattern PLAIN =
            Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*");

    private SourcePath() {
    }

    /**
     * Whether this value is a bare path into the source.
     *
     * @param value      the right-hand side as written, or {@code null} for an {@code ignore}
     * @param boundNames what this block's {@code let} lines name
     * @return {@code true} only when it is a path <em>and</em> its root is not one of those names
     */
    public static boolean readsSource(String value, Set<String> boundNames) {
        return value != null && PLAIN.matcher(value).matches() && !boundNames.contains(root(value));
    }

    /**
     * The first segment of a path.
     *
     * <p>⚠️ Only the first. {@code buyer.email} names {@code buyer} and nothing walks into the buyer's
     * type: a source may legitimately be a map, an untyped object, or a generic whose element a file
     * cannot see, and refusing those would break the check for exactly the loose sources a text mapping
     * is most useful for.</p>
     *
     * @param path the path as written
     * @return its first segment
     */
    public static String root(String path) {
        int dot = path.indexOf('.');

        return dot < 0 ? path : path.substring(0, dot);
    }
}
