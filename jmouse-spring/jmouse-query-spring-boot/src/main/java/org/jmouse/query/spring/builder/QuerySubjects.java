package org.jmouse.query.spring.builder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Every subject a product registered, by name.
 *
 * <p>⚠️ A name nobody registered is refused with the list that would have worked. A URL segment is
 * caller-supplied, so the alternative is a screen quietly getting an empty vocabulary and drawing a
 * builder with nothing in it — which reads as <em>this form has no fields</em> rather than as
 * <em>you asked for a listing that does not exist</em>.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class QuerySubjects {

    private final Map<String, QuerySubject> byName = new LinkedHashMap<>();

    public QuerySubjects(List<QuerySubject> registered) {
        for (QuerySubject subject : registered) {
            QuerySubject clash = byName.put(subject.name(), subject);

            if (clash != null) {
                throw new IllegalStateException(
                        ("Two subjects are both called '%s' — %s and %s. A subject's name is its address, "
                         + "so one of them would silently never be reachable.")
                                .formatted(subject.name(), clash.getClass().getName(),
                                           subject.getClass().getName()));
            }
        }
    }

    public QuerySubject named(String name) {
        QuerySubject found = byName.get(name);

        if (found == null) {
            throw new UnknownSubjectException(
                    "Nothing here lists '%s'. Registered: %s".formatted(name, String.join(", ", names())));
        }

        return found;
    }

    public Set<String> names() {
        return byName.keySet();
    }

    /** Asked for a listing nobody publishes. */
    public static class UnknownSubjectException extends RuntimeException {

        public UnknownSubjectException(String message) {
            super(message);
        }
    }
}
