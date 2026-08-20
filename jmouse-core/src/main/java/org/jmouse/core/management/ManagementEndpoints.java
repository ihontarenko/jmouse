package org.jmouse.core.management;

/**
 * 🛣️ Where a library's own management routes answer, and how a product moves them.
 *
 * <h2>⚠️ Why libraries do not serve under a product's {@code /api}</h2>
 *
 * <p>A library that publishes controllers is publishing them into somebody else's URL space. Two of
 * them chose {@code /api/files} and {@code /api/agents} — which is exactly where a product with files
 * or agents already serves its own — and the result was not a style problem: two controllers on one
 * path is an ambiguous mapping, and the context refuses to start. One module could not be switched on
 * in any product for that reason alone.</p>
 *
 * <p>So every library management surface answers under a prefix that is obviously the library's. An
 * address nobody would invent by accident is deliberate: a route that is visibly not the product's is
 * a route a reader will go and look up.</p>
 *
 * <h2>Two levels, and a product may use either</h2>
 *
 * <ul>
 *   <li>{@code jmouse.management.prefix} — <strong>one knob that moves every library at once.</strong>
 *       Set it to {@code /platform/api} and files, AI and whatever comes next all follow.
 *   <li>{@code jmouse.<namespace>.management.prefix} — one library, on its own. Set when a product has
 *       a reason for that surface specifically, not as a way of listing them all.
 * </ul>
 *
 * <p>Defaults compose: {@code /jmouse/files/api}, {@code /jmouse/ai/api}. Nothing has to be configured
 * for a product to work, and a product that configures the root gets every module in one line.</p>
 *
 * <h2>⚠️ How a CONSTANT can be configurable at all</h2>
 *
 * <p>An annotation attribute must be a compile-time constant, and the concatenation below is one — a
 * {@code static final String} built from other {@code static final String}s stays constant. What
 * arrives at runtime is a Spring placeholder: {@code RequestMappingHandlerMapping} is
 * {@code EmbeddedValueResolverAware} and resolves {@code ${…}} in a mapping as it registers the
 * handler. The annotation never notices.</p>
 *
 * <p>⚠️ <strong>The default is itself a placeholder, and that nesting is the whole mechanism.</strong>
 * A module writes {@code ${jmouse.files.management.prefix:${jmouse.management.prefix:/jmouse}/files/api}}
 * — Spring resolves placeholders inside a default value, so the module override wins if set, the root
 * wins if that is set, and the compiled-in default answers otherwise.</p>
 *
 * <h2>⚠️ What a placeholder cannot do, said out loud</h2>
 *
 * <p>It is not a path. A product's security configuration, its dev-server proxy and its interface
 * cannot read these constants and must be told the same value again — three places, with nothing
 * checking that they agree. When they drift every call 404s, and the screens render as an installation
 * with nothing in it rather than as a routing mistake. That is not hypothetical: it is what happened to
 * the AI management screens, and it is why a product should hold the address in ONE place of its own.</p>
 *
 * <p>⚠️ <strong>Authorization is not one of those places</strong>, and that is what makes moving these
 * routes safe: an {@code ExternalAccessRules} declaration names a controller class and a method, never
 * a path. Re-prefixing cannot un-gate anything.</p>
 *
 * <h2>How a module uses this</h2>
 *
 * <pre>{@code
 * public static final String PREFIX =
 *     ManagementEndpoints.moduleOverrideOpens("files") + ManagementEndpoints.ROOT + "/files"
 *     + ManagementEndpoints.CLOSE;
 * }</pre>
 *
 * <p>…except that a method call is not a constant, so a module spells it instead:</p>
 *
 * <pre>{@code
 * public static final String PREFIX =
 *     "${jmouse.files.management.prefix:" + ManagementEndpoints.ROOT + "/files}";
 * }</pre>
 *
 * <p>⚠️ Two things about that line are load-bearing and neither is obvious: the module's own property
 * is {@code jmouse.<namespace>.management.prefix} — the convention every module follows so a product
 * can guess the name — and the closing {@code }} is the placeholder's, not the string's.</p>
 */
public final class ManagementEndpoints {

    /** The property a product sets to move every library management surface at once. */
    public static final String ROOT_PROPERTY = "jmouse.management.prefix";

    /**
     * Where they answer when nothing is configured.
     *
     * <p>⚠️ Deliberately not {@code /api}: that belongs to the product, and taking it is what made one
     * of these modules unmountable.
     */
    public static final String DEFAULT_ROOT = "/jmouse";

    /**
     * The root, as a placeholder a module builds its own prefix from.
     *
     * <p>⚠️ <strong>Use this rather than writing the property name again.</strong> A module that spells
     * {@code ${jmouse.management.prefix:/jmouse}} itself is a second copy of both the property and
     * the default, and the two drift the first time either changes.
     */
    public static final String ROOT = "${" + ROOT_PROPERTY + ":" + DEFAULT_ROOT + "}";

    /**
     * The property one module answers to, by convention.
     *
     * <p>⚠️ <strong>Documentation, not something a mapping can use</strong> — a method call is not a
     * compile-time constant, so an annotation cannot take its result. It exists so the convention has
     * one written definition and a product can be told the name rather than shown an example.
     *
     * @param namespace the module's short name — {@code files}, {@code ai}
     * @return the property that moves that module alone
     */
    public static String propertyFor(String namespace) {
        return "jmouse." + namespace + ".management.prefix";
    }

    private ManagementEndpoints() {
    }
}
