package org.jmouse.security.jwt;

import org.jmouse.security.core.Authority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * 🧭 JwtAuthorities
 * <p>
 * Utility for mapping JWT claims (such as {@code scope} or {@code roles})
 * into application-specific authorities.
 *
 * <p>Conventions:</p>
 * <ul>
 *   <li>🔑 {@code scope} claim → space-delimited string → {@code SCOPE_x} authorities</li>
 *   <li>🎭 {@code roles} claim → array of values → {@code ROLE_x} authorities</li>
 * </ul>
 */
public final class JwtAuthorities {

    /**
     * 🔑 Prefix used for authorities derived from scopes.
     */
    public static final String SCOPE_PREFIX = "SCOPE_";

    /**
     * 🔑 Claim name commonly used in OAuth2 for scopes.
     */
    public static final String SCOPE_ATTRIBUTE = "scope";

    /**
     * 🎭 Prefix used for authorities derived from roles.
     */
    public static final String ROLE_PREFIX = "ROLE_";

    private JwtAuthorities() { }

    /**
     * 🔑 Extract OAuth2-style "scope" claim and map to strings with {@code SCOPE_} prefix.
     *
     * <p>Example: {@code "openid profile"} → {@code ["SCOPE_openid", "SCOPE_profile"]}</p>
     *
     * @return mapping function from {@link Jwt} to collection of scope-based authorities
     */
    public static Function<Jwt, Collection<String>> scopeClaim() {
        return jwt -> {
            Object value = jwt.claims().get(SCOPE_ATTRIBUTE);

            if (value instanceof String stringScope && !stringScope.isBlank()) {
                String[]     splitted = stringScope.split("\\s+");
                List<String> output   = new ArrayList<>(splitted.length);
                for (String scope : splitted) {
                    output.add(SCOPE_PREFIX + scope);
                }
                return List.copyOf(output);
            }

            return List.of();
        };
    }

    /**
     * 🔑 Extract OAuth2-style "scope" claim and map to {@link Authority}.
     *
     * @return mapping function from {@link Jwt} to authorities
     */
    public static Function<Jwt, Collection<Authority>> scopeAuthorities() {
        return scopeClaim().andThen(Authority::ofCollection);
    }

    /**
     * 🎭 Extract custom "roles" claim (array/collection) and map to strings with {@code ROLE_} prefix.
     *
     * <p>Example: {@code ["admin","user"]} → {@code ["ROLE_admin","ROLE_user"]}</p>
     *
     * @param claimName JWT claim name that holds roles (e.g. {@code "roles"}, {@code "groups"})
     * @return mapping function from {@link Jwt} to role-based strings
     */
    public static Function<Jwt, Collection<String>> rolesArray(String claimName) {
        return jwt -> {
            Object value = jwt.claims().getOrDefault(claimName, List.of());

            if (value instanceof Collection<?> collection) {
                List<String> output = new ArrayList<>(collection.size());
                for (Object role : collection) {
                    output.add(ROLE_PREFIX + role);
                }
                return output;
            }

            return List.of();
        };
    }

    /**
     * 🎭 Extract custom "roles" claim (array/collection) and map to {@link Authority}.
     *
     * @param claimName JWT claim name that holds roles (e.g. {@code "roles"}, {@code "groups"})
     * @return mapping function from {@link Jwt} to authorities
     */
    public static Function<Jwt, Collection<Authority>> rolesAuthorities(String claimName) {
        return rolesArray(claimName).andThen(Authority::ofCollection);
    }
}
