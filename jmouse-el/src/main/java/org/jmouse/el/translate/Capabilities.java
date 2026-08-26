package org.jmouse.query.translate;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * What a backend can actually honour.
 *
 * <h2>⚠️ A universal language does not mean every backend does everything</h2>
 *
 * <p>SQL can group; a REST translator over somebody else's API cannot; an in-memory evaluator can do
 * nearly everything and will do it slowly. Left implicit, that difference becomes the failure this whole
 * cluster exists to prevent: one day an expression <strong>quietly returns the wrong thing</strong> on a
 * different backend, and nothing says so.</p>
 *
 * <p>So the language is universal, the capability is <strong>declared</strong>, and a document exceeding
 * it is refused at compile time with a sentence naming the translator. ⚠️ That is decision 2 continued
 * rather than weakened — <em>the compiler is the confinement</em> — there are simply several compilers
 * now, and each states its own limits.</p>
 *
 * <p>⚠️ What must never happen is a translator <strong>ignoring</strong> a clause it cannot honour.
 * Refusing is correct; silently dropping a {@code group} and returning ungrouped rows is the bug this
 * type exists to make impossible to write by accident.</p>
 *
 * <h2>⚠️ The set is open</h2>
 *
 * <p>A {@link Capability} is a name rather than an enum constant, so a backend may declare one the
 * language has never heard of — {@code elastic.score} — and be refused for it by the same machinery
 * that refuses {@code group}. See {@link Capability} for the naming rule.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class Capabilities {

    /**
     * ⚠️ The capabilities the LANGUAGE ships with — not "all of them", which an open set cannot mean.
     *
     * <p>This is what {@link #everything(String)} hands out, and it deliberately excludes anything a
     * product has contributed: a backend claiming somebody else's capability by accident is the failure
     * an open set would otherwise invite.</p>
     */
    private static final List<Capability> BUILT_IN = List.of(
            Capability.FILTER,
            Capability.SORT,
            Capability.PROJECT,
            Capability.AGGREGATE,
            Capability.JOIN,
            Capability.SUBQUERY,
            Capability.CORRELATE,
            Capability.CONVERT,
            Capability.CLOCK,
            Capability.LIMIT);

    private final String           translator;
    private final Set<Capability>  capabilities;

    private Capabilities(String translator, Collection<Capability> capabilities) {
        this.translator = translator;
        this.capabilities = Set.copyOf(capabilities);
    }

    public static Capabilities of(String translator, Capability... capabilities) {
        return new Capabilities(translator, List.of(capabilities));
    }

    /**
     * Every capability the language itself defines — what a full SQL backend offers.
     *
     * <p>⚠️ Not "everything there is". An open set has no such thing, and a backend that wants a
     * product's own capability as well says so with {@link #and(Capability...)}.</p>
     */
    public static Capabilities everything(String translator) {
        return new Capabilities(translator, BUILT_IN);
    }

    /**
     * The same backend, also able to do these — for a product contributing its own vocabulary.
     *
     * <pre>{@code
     * Capabilities.everything("elastic").and(Capability.named("elastic", "score"));
     * }</pre>
     *
     * @param additional what else it can honour
     * @return a new declaration; this one is unchanged
     */
    public Capabilities and(Capability... additional) {
        Set<Capability> merged = new LinkedHashSet<>(capabilities);

        merged.addAll(List.of(additional));

        return new Capabilities(translator, merged);
    }

    public String translator() {
        return translator;
    }

    public boolean has(Capability capability) {
        return capabilities.contains(capability);
    }

    /** What this backend declares, for a caller that wants to show it rather than test it. */
    public Set<Capability> all() {
        return capabilities;
    }

    /**
     * Refuses a capability this translator does not have, naming it and the translator.
     *
     * <p>⚠️ The message names <strong>which translator</strong>, because the same document is expected to
     * run on several and "not supported" without that is a message somebody has to debug rather than
     * read.</p>
     *
     * @param capability what the document asked for
     * @param clause     the clause that asked, for the sentence
     */
    public void require(Capability capability, String clause) {
        if (!has(capability)) {
            throw new UnsupportedQueryException(
                    "'%s' is not supported by the '%s' translator; it can do %s".formatted(
                            clause, translator, describe()));
        }
    }

    /**
     * ⚠️ The language's own first, in the order they are declared above, then anything contributed —
     * so a refusal reads the same everywhere instead of in whatever order a hash set happened to hold.
     */
    private String describe() {
        if (capabilities.isEmpty()) {
            return "nothing";
        }

        Set<String> names = new LinkedHashSet<>();

        for (Capability capability : BUILT_IN) {
            if (capabilities.contains(capability)) {
                names.add(capability.name());
            }
        }

        capabilities.stream()
                .filter(Capability::isQualified)
                .map(Capability::name)
                .sorted()
                .forEach(names::add);

        return String.join(", ", names);
    }

    @Override
    public String toString() {
        return "%s: %s".formatted(translator, describe());
    }
}
