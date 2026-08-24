package org.jmouse.query.adapter;

import java.util.EnumSet;
import java.util.Set;

/**
 * What a backend can actually honour.
 *
 * <h2>⚠️ A universal language does not mean every backend does everything</h2>
 *
 * <p>SQL can group; a REST adapter over somebody else's API cannot; an in-memory evaluator can do
 * everything and will do it slowly. Left implicit, that difference becomes the failure this whole
 * cluster exists to prevent: one day an expression <strong>quietly returns the wrong thing</strong> on a
 * different backend, and nothing says so.</p>
 *
 * <p>So the language is universal, the capability is <strong>declared</strong>, and a document exceeding
 * it is refused at compile time with a sentence naming the adapter. ⚠️ That is decision 2 continued
 * rather than weakened — <em>the compiler is the confinement</em> — there are simply several compilers
 * now, and each states its own limits.</p>
 *
 * <p>⚠️ What must never happen is an adapter <strong>ignoring</strong> a clause it cannot honour.
 * Refusing is correct; silently dropping a {@code group} and returning ungrouped rows is the bug this
 * type exists to make impossible to write by accident.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class Capabilities {

    /** One thing a document may ask for that a backend may or may not be able to do. */
    public enum Feature {

        /** {@code where} — filtering rows. A backend that cannot do this is not a backend. */
        FILTER,

        /** {@code order} — sorting. */
        SORT,

        /** {@code columns} — returning something other than whole rows. */
        PROJECT,

        /** {@code group} and {@code having} — gathering rows and filtering the groups. */
        AGGREGATE,

        /** Reaching a value that lives in another table. */
        JOIN,

        /** Reading text as a number, and taking a value apart. */
        CONVERT,

        /** {@code now()} and durations. */
        CLOCK
    }

    private final String       adapter;
    private final Set<Feature> features;

    private Capabilities(String adapter, Set<Feature> features) {
        this.adapter = adapter;
        this.features = features.isEmpty() ? EnumSet.noneOf(Feature.class) : EnumSet.copyOf(features);
    }

    public static Capabilities of(String adapter, Feature... features) {
        return new Capabilities(adapter, Set.of(features));
    }

    /** Everything — what a full SQL backend offers. */
    public static Capabilities everything(String adapter) {
        return new Capabilities(adapter, EnumSet.allOf(Feature.class));
    }

    public String adapter() {
        return adapter;
    }

    public boolean has(Feature feature) {
        return features.contains(feature);
    }

    /**
     * Refuses a feature this adapter does not have, naming it and the adapter.
     *
     * <p>⚠️ The message names <strong>which adapter</strong>, because the same document is expected to
     * run on several and "not supported" without that is a message somebody has to debug rather than
     * read.</p>
     *
     * @param feature what the document asked for
     * @param clause  the clause that asked, for the sentence
     */
    public void require(Feature feature, String clause) {
        if (!has(feature)) {
            throw new UnsupportedQueryException(
                    "'%s' is not supported by the '%s' adapter; it can do %s".formatted(
                            clause, adapter, features.isEmpty() ? "nothing" : features.toString()));
        }
    }
}
