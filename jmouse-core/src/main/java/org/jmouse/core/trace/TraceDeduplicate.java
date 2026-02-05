package org.jmouse.core.trace;

import org.jmouse.core.context.ContextKey;
import org.jmouse.core.context.execution.ExecutionContext;
import org.jmouse.core.context.execution.ExecutionContextHolder;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class TraceDeduplicate {

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final ContextKey<Set<String>> SEEN_KEYS = ContextKey.of("trace.deduplicate.seen", (Class) Set.class);

    private TraceDeduplicate() {
    }

    public static boolean firstTime(String key) {
        if (key == null || key.isBlank()) {
            return true;
        }

        ExecutionContext context = ExecutionContextHolder.current();
        if (context == null) {
            return true;
        }

        Set<String> seen = context.get(SEEN_KEYS);

        if (seen == null) {
            seen = Collections.newSetFromMap(new ConcurrentHashMap<>());
            ExecutionContextHolder.replace(context.with(SEEN_KEYS, seen));
        }

        boolean first = seen.add(key);

        return first;
    }
}
