package org.jmouse.access.enforcement;

import org.jmouse.access.ScopeKind;

import java.util.Optional;

/**
 * The place the caller is currently in, for the routes whose whole subject is <em>here</em>.
 *
 * <p>Most routes name their place in the URL. Some cannot: "the settings of the workspace I am in"
 * has no identifier to put in a path, and the only thing that knows which one is meant is something
 * ambient — a header, a session attribute, a tenant resolved from the hostname.
 *
 * <p><strong>It decides which question is asked; it has never decided the answer.</strong> An ambient
 * place naming somewhere the caller holds nothing resolves to no permissions there, so this is a
 * convenience rather than a way in. It is read <em>last</em>, and only where the route named no place
 * of this kind — a route that spells out its place must not be overridden by a header.
 *
 * <p>Reading it is also gated on the endpoint's declared scope being no wider than {@link #kind()}: an
 * installation-wide route that picked up a workspace header would silently stop being
 * installation-wide the first time a client sent one.
 */
public interface AmbientPlace {

    /** No ambient place at all — every route says where it is aimed, or is aimed nowhere. */
    static AmbientPlace none() {
        return new AmbientPlace() {

            @Override
            public ScopeKind kind() {
                return null;
            }

            @Override
            public Optional<String> instance() {
                return Optional.empty();
            }
        };
    }

    /**
     * Which kind of place this is about — the product's workspace, tenant, or whatever it nests by.
     *
     * <p>Named rather than derived as "the innermost floor", because those are not the same thing: a
     * product that added a floor beneath its workspace would still have a header about the workspace,
     * and the clever version would start applying it to the new floor instead.
     */
    ScopeKind kind();

    /** Which one, for the call being served, or empty where the caller is not anywhere in particular. */
    Optional<String> instance();
}
