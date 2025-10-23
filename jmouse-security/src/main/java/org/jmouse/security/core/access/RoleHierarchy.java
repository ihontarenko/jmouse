package org.jmouse.security.core.access;

import org.jmouse.core.Streamable;
import org.jmouse.security.core.Authority;

import java.util.*;

/**
 * 🧭 Role hierarchy with reachability queries.
 *
 * <p>Represents implication relationships between authorities (roles), e.g.
 * {@code ADMIN > USER} means {@code ADMIN} implies (inherits) {@code USER}.</p>
 *
 * <h3>Key operations</h3>
 * <ul>
 *   <li>🔎 {@link #getReachableRoles(Collection)} — compute the reflexive/transitive closure of roles.</li>
 *   <li>➡️ {@link #implies(Authority, Authority)} — test whether one role implies another.</li>
 *   <li>🧱 {@link #parse(String)} — build a hierarchy from a simple DSL.</li>
 * </ul>
 */
@FunctionalInterface
public interface RoleHierarchy {

    /**
     * 🧱 Parses a hierarchy definition (DSL) into a {@link RoleHierarchy}.
     *
     * <p>Format (per line): {@code HIGHER > LOWER1, LOWER2, ...}</p>
     * <ul>
     *   <li>Lines may contain whitespace; empty lines are ignored.</li>
     *   <li>Lines starting with {@code #} are treated as comments.</li>
     *   <li>Multiple lines accumulate implications.</li>
     *   <li>Cycles are allowed; traversal is guarded by a visited set.</li>
     * </ul>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * # Role implications
     * ROLE_ADMIN   > ROLE_MANAGER, ROLE_USER
     * ROLE_MANAGER > ROLE_USER
     * }</pre>
     *
     * @param definition multiline DSL describing role implications
     * @return an immutable {@link RoleHierarchy} instance
     * @throws IllegalArgumentException if an authority token cannot be parsed
     */
    static RoleHierarchy parse(String definition) {
        StringTokenizer                splitter  = new StringTokenizer(definition, "\n\r");
        Map<Authority, Set<Authority>> hierarchy = new HashMap<>();

        while (splitter.hasMoreTokens()) {
            String          line      = splitter.nextToken().trim();
            StringTokenizer tokenizer = new StringTokenizer(line, ">,");
            List<String>    tokens    = new ArrayList<>();

            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }

            while (tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken().trim();
                tokens.add(token);
            }

            Authority      authority   = Authority.of(tokens.getFirst());
            Set<Authority> authorities = Streamable.of(tokens.subList(1, tokens.size())).map(Authority::of).toSet();

            hierarchy.computeIfAbsent(authority, __ -> new HashSet<>()).addAll(authorities);
        }

        return new Authorities(hierarchy);
    }

    /**
     * 🌑 A hierarchy with no implications (identity only).
     *
     * <p>The closure of a set is the set itself; i.e., a role implies only itself.</p>
     *
     * @return a {@link RoleHierarchy} with no edges
     */
    static RoleHierarchy none() {
        return Set::copyOf;
    }

    /**
     * 🔁 Convenience overload that computes the closure for a vararg list of roles.
     *
     * @param authorities roles to start from
     * @return all reachable roles including the inputs (reflexive closure)
     */
    default Set<Authority> getReachableRoles(Authority... authorities) {
        return getReachableRoles(Set.of(authorities));
    }

    /**
     * ➡️ Tests whether {@code hi} implies {@code lo}.
     *
     * <p>Reflexive: a role implies itself.</p>
     *
     * @param hi higher (candidate) role
     * @param lo lower (target) role
     * @return {@code true} if {@code lo} is in the transitive closure of {@code hi}
     */
    default boolean implies(Authority hi, Authority lo) {
        return getReachableRoles(hi).contains(lo);
    }

    /**
     * 🧮 Computes the reflexive/transitive closure of the given roles.
     *
     * @param authorities seed roles
     * @return all reachable roles including the seeds; never {@code null}
     */
    Set<Authority> getReachableRoles(Collection<? extends Authority> authorities);

    /**
     * 📦 Immutable role-hierarchy implementation.
     *
     * <p>Stores an unmodifiable adjacency map of direct implications and computes closures
     * via breadth-first traversal.</p>
     */
    class Authorities implements RoleHierarchy {

        private final Map<Authority, Set<Authority>> hierarchy;

        /**
         * 🏗️ Creates an immutable view of the provided adjacency map.
         *
         * <p>Both keys and values (sets) are defensively copied.</p>
         *
         * @param hierarchy map of {@code higherRole → directLowerRoles}
         */
        public Authorities(Map<Authority, Set<Authority>> hierarchy) {
            Map<Authority, Set<Authority>> temporary = new HashMap<>();
            hierarchy.forEach((k, v) -> temporary.put(k, Set.copyOf(v)));
            this.hierarchy = Map.copyOf(temporary);
        }

        /**
         * 🧭 Breadth-first expansion from the seed roles.
         *
         * <p>Returns a new unmodifiable set that includes the input roles and all roles
         * reachable by following {@code higher → lower} edges.</p>
         *
         * <p><b>Complexity:</b> O(V + E) over the portion of the graph reached from the seeds.</p>
         *
         * @param authorities seed roles (may be empty)
         * @return reflexive/transitive closure; empty set when {@code authorities} is null/empty
         */
        @Override
        public Set<Authority> getReachableRoles(Collection<? extends Authority> authorities) {
            if (authorities == null || authorities.isEmpty()) {
                return Set.of();
            }

            Deque<Authority> queue   = new ArrayDeque<>();
            Set<Authority>   visited = new LinkedHashSet<>();

            for (Authority authority : authorities) {
                if (visited.add(authority)) {
                    queue.addLast(authority);
                }
            }

            while (!queue.isEmpty()) {
                Authority authority = queue.removeFirst();
                if (hierarchy.containsKey(authority)) {
                    Set<Authority> reachable = hierarchy.get(authority);
                    reachable.forEach(queue::addLast);
                    visited.addAll(reachable);
                }
            }

            return Set.copyOf(visited);
        }
    }

    class Builder {

        private final Map<Authority, Set<Authority>> hierarchy = new HashMap<>();

        public Implies authority(String authority) {
            return new Implies(Authority.of(authority));
        }

        public Implies authority(Authority authority) {
            return new Implies(authority);
        }

        public RoleHierarchy build() {
            return new Authorities(hierarchy);
        }

        public class Implies {

            private final Authority parent;

            public Implies(Authority parent) {
                this.parent = parent;
            }

            public Builder implies(String... authorities) {
                return implies(Streamable.of(authorities).map(Authority::of).toArray(Authority[]::new));
            }

            public Builder implies(Authority... authorities) {
                Set<Authority> implied = Builder.this.hierarchy.computeIfAbsent(parent, __ -> new HashSet<>());
                Streamable.of(authorities).forEach(implied::add);
                return Builder.this;
            }

        }

    }

}
