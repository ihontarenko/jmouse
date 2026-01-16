package org.jmouse.crawler.dsl;

import org.jmouse.core.Verify;
import org.jmouse.crawler.runtime.RoutingHint;

@FunctionalInterface
public interface CrawlHintAdapter {

    RoutingHint adapt(Object clientHint);

    static CrawlHintAdapter defaultAdapter() {
        return object -> switch (object) {
            case null -> null;
            case RoutingHint hint -> hint;
            case Enum<?> enumValue -> new SimpleHint(enumValue.name());
            case String string -> new SimpleHint(string);
            default -> new SimpleHint(object.toString());
        };
    }

    record SimpleHint(String id) implements RoutingHint {
        public SimpleHint {
            Verify.nonNull(id, "id");
        }
    }
}
