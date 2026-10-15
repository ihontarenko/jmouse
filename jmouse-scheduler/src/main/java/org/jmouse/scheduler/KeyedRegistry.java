package org.jmouse.scheduler;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

/** Entries by key, validated and unique — the shared body of the handler and gate registries. */
final class KeyedRegistry<T> {

    private final Map<String, T> entries;

    KeyedRegistry(Collection<? extends T> candidates, Function<T, String> keyOf, String role) {
        Map<String, T> byKey = new TreeMap<>();

        for (T candidate : candidates) {
            String key = Trigger.requireKey(keyOf.apply(candidate), role);
            T previous = byKey.putIfAbsent(key, candidate);

            if (previous != null) {
                throw new IllegalStateException("Two " + role + "s claim the key '" + key + "': "
                        + previous.getClass().getName() + " and " + candidate.getClass().getName() + ".");
            }
        }

        this.entries = Collections.unmodifiableMap(byKey);
    }

    Optional<T> find(String key) {
        return Optional.ofNullable(entries.get(key));
    }

    Set<String> keys() {
        return entries.keySet();
    }
}
