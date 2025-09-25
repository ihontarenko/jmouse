package org.jmouse.security.core.access;

import java.util.*;

/**
 * 🧭 Derives reachable authorities from a given set (e.g., {@code ROLE_ADMIN > ROLE_USER}).
 */
public interface RoleHierarchy {

    Set<String> getReachableRoles(Collection<String> authorities);

    class Default implements RoleHierarchy {

        private final Map<String, Set<String>> hierarchy;

        public Default(Map<String, ? extends Collection<String>> relations) {
            Map<String, Set<String>> copy = new HashMap<>();
            relations.forEach((key, values) -> copy.put(key, Set.copyOf(values)));
            this.hierarchy = Map.copyOf(copy);
        }

        @Override
        public Set<String> getReachableRoles(Collection<String> authorities) {
            Set<String>   result = new LinkedHashSet<>(authorities);
            Deque<String> queue  = new ArrayDeque<>(authorities);

            while (!queue.isEmpty()) {
                String      role     = queue.pop();
                Set<String> children = hierarchy.get(role);
                if (children != null) {
                    for (String child : children) {
                        if (result.add(child)) {
                            queue.addLast(child);
                        }
                    }
                }
            }

            return Set.copyOf(result);
        }
    }

}
