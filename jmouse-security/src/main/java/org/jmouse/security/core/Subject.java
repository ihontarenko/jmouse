package org.jmouse.security.core;

import java.util.Map;
import java.util.Set;

/**
 * 👤 Security subject (user, service, system).
 *
 * <p>Identified by an {@link #id()}, has a {@link #kind()},
 * optional claims, and a set of granted authorities.</p>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * Subject user = new Subject() {
 *     @Override
 *     public String id() {
 *         return "42";
 *     }
 *
 *     @Override
 *     public String kind() {
 *         return "USER";
 *     }
 *
 *     @Override
 *     public Map<String, Object> claims() {
 *         return Map.of("email", "alice@example.com");
 *     }
 *
 *     @Override
 *     public Set<String> authorities() {
 *         return Set.of("ROLE_USER", "READ_PRIVILEGES");
 *     }
 * };
 *
 * System.out.println(user.kind());        // USER
 * System.out.println(user.id());          // 42
 * System.out.println(user.authorities()); // [ROLE_USER, READ_PRIVILEGES]
 * System.out.println(user.claims());      // {email=alice@example.com}
 * }</pre>
 */
public interface Subject {

    /**
     * 🆔 Unique identifier (user id, client id, etc.).
     */
    String id();

    /**
     * 🏷️ Kind of subject (USER, SERVICE, SYSTEM, ...).
     */
    String kind();

    /**
     * 📜 Claims (arbitrary attributes like email, dept, device).
     */
    Map<String, Object> claims();

    /**
     * 🔑 Granted authorities (roles, scopes, permissions).
     */
    Set<String> authorities();
}
