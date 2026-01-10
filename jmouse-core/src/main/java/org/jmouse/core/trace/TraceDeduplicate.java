package org.jmouse.core.trace;

import org.jmouse.core.context.ContextKey;
import org.jmouse.core.context.execution.ExecutionContext;
import org.jmouse.core.context.execution.ExecutionContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class TraceDeduplicate {

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final  ContextKey<Set<String>> SEEN_KEYS
            = ContextKey.of("trace.deduplicate.seen", (Class) Set.class);
    private static final Logger                  LOGGER    = LoggerFactory.getLogger(TraceDeduplicate.class);

    private TraceDeduplicate() {
    }

    public static boolean firstTime(String key) {
        if (key == null || key.isBlank()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("firstTime(): blank key -> allow publish");
            }
            return true;
        }

        ExecutionContext context = ExecutionContextHolder.current();
        if (context == null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("firstTime(): no ExecutionContext -> allow publish, key={}", key);
            }
            return true;
        }

        Set<String> seen = context.get(SEEN_KEYS);

        if (seen == null) {
            seen = Collections.newSetFromMap(new ConcurrentHashMap<>());
            ExecutionContextHolder.replace(context.with(SEEN_KEYS, seen));
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("firstTime(): init seen-set, key={}", key);
            }
        }

        boolean first = seen.add(key);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("firstTime(): key={} -> {}", key, (first ? "FIRST" : "DUPLICATE"));
        }

        return first;
    }
}
