package org.jmouse.mapper.el.translate;

import org.jmouse.el.translate.Capability;

/**
 * What a {@code .jmm} destination may declare it can honour — one capability per construct the
 * language has.
 *
 * <h2>⚠️ Why every one of them is qualified</h2>
 *
 * <p>{@link Capability}'s rule is that an unqualified name belongs to the language forever and a
 * product's own carries its namespace. A mapping document is not a query: it has no {@code filter} to
 * declare and no {@code limit} to refuse, so borrowing the query language's built-ins would say
 * something false about every destination here. All of these are {@code mapping.}.</p>
 *
 * <h2>⚠️ The list is the reason the seam is worth having at all</h2>
 *
 * <p>A generated-Java back end (JMF-189) cannot honour what a runtime one can — that is the whole
 * question that epic has to settle — and the only place it can <em>say so</em> is here. A translator
 * that declares nothing refuses nothing, and a mapping silently missing its {@code when} guard is a
 * mapping that writes a property it was told to leave alone.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JmmCapability {

    /** The namespace every capability here carries. */
    public static final String NAMESPACE = "mapping";

    /** {@code use a.b.C} — a type named short for the rest of the file. */
    public static final Capability USE = Capability.named(NAMESPACE, "use");

    /** {@code fragment name { … }} — rules named once and included where they are wanted. */
    public static final Capability FRAGMENT = Capability.named(NAMESPACE, "fragment");

    /** {@code target Type { … }} — what is being built. */
    public static final Capability TARGET = Capability.named(NAMESPACE, "target");

    /** {@code always { … }} — rules that apply whatever the source is. */
    public static final Capability ALWAYS = Capability.named(NAMESPACE, "always");

    /** {@code from Type { … }} — one source of a target. */
    public static final Capability FROM = Capability.named(NAMESPACE, "from");

    /** {@code from Type : via(…)} — the whole pair converted rather than mapped property by property. */
    public static final Capability VIA = Capability.named(NAMESPACE, "via");

    /** {@code property : expression} — the ordinary rule. */
    public static final Capability RULE = Capability.named(NAMESPACE, "rule");

    /** {@code property : ignore} — a property deliberately left unwritten. */
    public static final Capability IGNORE = Capability.named(NAMESPACE, "ignore");

    /** {@code … when condition} — a rule that leaves the property alone rather than writing null. */
    public static final Capability WHEN = Capability.named(NAMESPACE, "when");

    /** {@code let name = expression} — a value named once inside a block. */
    public static final Capability LET = Capability.named(NAMESPACE, "let");

    /** {@code include fragment} — a fragment's rules pulled in. */
    public static final Capability INCLUDE = Capability.named(NAMESPACE, "include");

    /** {@code refuse source|target before|after { … }} — an assertion that stops the mapping. */
    public static final Capability REFUSE = Capability.named(NAMESPACE, "refuse");

    /** {@code unmapped fail} — a target property nothing fills is an error rather than a silence. */
    public static final Capability UNMAPPED = Capability.named(NAMESPACE, "unmapped");

    private static final Capability[] EVERY = {
            USE, FRAGMENT, TARGET, ALWAYS, FROM, VIA,
            RULE, IGNORE, WHEN, LET, INCLUDE, REFUSE, UNMAPPED
    };

    private JmmCapability() {
    }

    /**
     * Every capability a mapping document can ask for.
     *
     * <p>Copied on the way out — a shared array is a set of constants anybody can quietly edit.</p>
     *
     * @return all thirteen
     */
    public static Capability[] every() {
        return EVERY.clone();
    }

    /**
     * How a capability is written in a file, for a refusal somebody has to read.
     *
     * @param capability one of the constants above
     * @return its name without the namespace
     */
    public static String keyword(Capability capability) {
        String name = capability.name();

        return name.substring(name.indexOf('.') + 1);
    }
}
