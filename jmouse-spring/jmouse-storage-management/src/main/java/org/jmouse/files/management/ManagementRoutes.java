package org.jmouse.files.management;

import org.jmouse.core.management.ManagementEndpoints;

/**
 * 🛣️ Where this module's routes live.
 *
 * <p>⚠️ <strong>Stated once and read by everything.</strong> The address appears in the controller, in
 * a product's {@code ExternalAccessRules}, in its security configuration and in whatever the interface
 * is built against — and when those drift the failure is not an error, it is a screen that renders as
 * an empty installation. That has already happened once to the AI management screen.</p>
 *
 * <h2>⚠️ Under a prefix of the library's own, and NOT under a product's {@code /api}</h2>
 *
 * <p>These routes used to answer at {@code /api/files} and {@code /api/directories} — which is where
 * every product that has files already serves its own. The consequence was not a style problem: the
 * module could not be switched on at all, because two controllers on one path is an ambiguous mapping
 * and the context refuses to start. Innoventa documented it in its {@code pom.xml} and left the flag
 * off; Kiwi hit it and resolved it by deleting its own copies (KW-0079).</p>
 *
 * <p>An address nobody would invent by accident is deliberate, the same way
 * {@code jmouse-ai-management}'s is: a route that is visibly not the product's is a route a reader will
 * go and look up. And a product that wants these somewhere else says so in one line rather than
 * choosing between the library and its own routes.</p>
 *
 * <h2>⚠️ How a constant can be configurable at all</h2>
 *
 * <p>{@link #PREFIX} is a Spring placeholder, and the concatenation below is still a compile-time
 * constant — which is what an annotation attribute requires. {@code RequestMappingHandlerMapping} is
 * {@code EmbeddedValueResolverAware} and resolves {@code ${…}} in a mapping when it registers the
 * handler, so the value arrives from configuration and the annotation never notices.</p>
 *
 * <p>⚠️ <strong>The cost, said out loud, because this codebase has already paid it once.</strong> A
 * placeholder is not a path: a product's security configuration, its dev-server proxy and its interface
 * cannot read this constant and must be told the same value again. Three places, and nothing checks
 * that they agree — when they drift, every call 404s and the screens render as an installation with no
 * files rather than as a routing mistake. That is exactly what happened to the AI screens, and it is
 * why each product should hold the address in ONE place of its own.</p>
 *
 * <p>⚠️ <strong>Authorization is NOT one of those places</strong>, and that is what makes this safe to
 * move at all: {@code FilesAccessRules} gates by controller class and method name, never by path. A
 * product that re-prefixes these routes does not touch its rules, and cannot un-gate them by moving
 * them.</p>
 */
public final class ManagementRoutes {

    /**
     * This module's own segment under the shared root — the module, then its API.
     *
     * <p>⚠️ <strong>{@code /api} is part of the segment rather than part of the root</strong>, and it is
     * what keeps the resource names unambiguous. With the module segment alone, the tree would sit at
     * {@code …/files/directories} — and that <em>matches {@code /files/{fileId}} with the identifier
     * "directories"</em>. Spring prefers the literal and it would work; it is still a mapping one
     * refactor away from resolving somewhere nobody meant. With {@code /api} between them the two
     * collections are siblings and nothing overlaps.
     *
     * <p>⚠️ <strong>It also makes the default read like the AI module's.</strong> That one has answered
     * at its own visible prefix since it was written; this one answered at {@code /api/files}, which is
     * the product's space and is why it could not be switched on anywhere.
     */
    public static final String SEGMENT = "/files/api";

    /**
     * Where the controllers answer. Resolved by Spring, so a product only sets it in configuration.
     *
     * <p>Two levels, and a product may use either — see {@link ManagementEndpoints}:
     * {@code jmouse.management.prefix} moves every library at once,
     * {@code jmouse.files.management.prefix} moves this one alone. The default composes to
     * {@code /jmouse/files/api}.
     *
     * <p>⚠️ <strong>The default is itself a placeholder</strong>, and that nesting is the mechanism
     * rather than a trick: Spring resolves {@code ${…}} inside a default value, so the module override
     * wins if it is set, the shared root wins if that is, and the compiled-in default answers otherwise.
     */
    public static final String PREFIX =
            "${jmouse.files.management.prefix:" + ManagementEndpoints.ROOT + SEGMENT + "}";

    /** Everything this module serves about files sits under here. */
    public static final String BASE = PREFIX + "/files";

    /** One file. */
    public static final String ONE = BASE + "/{fileId}";

    /** The bytes of one file. */
    public static final String CONTENT = ONE + "/content";

    /** Fetching a file from a web address rather than sending its bytes. */
    public static final String IMPORT = BASE + "/import";

    /** Where one file is filed. */
    public static final String BINDING = ONE + "/binding";

    /** Whether a file is listed only to whoever may already reach it. */
    public static final String PRIVACY = ONE + "/private";

    /** The directory tree. */
    public static final String DIRECTORIES = PREFIX + "/directories";

    /** One directory. */
    public static final String DIRECTORY = DIRECTORIES + "/{directoryId}";

    /** One directory and everything under it. */
    public static final String DIRECTORY_SUBTREE = DIRECTORY + "/subtree";

    /** Where one directory sits. */
    public static final String DIRECTORY_PARENT = DIRECTORY + "/parent";

    private ManagementRoutes() {
    }
}
