package org.jmouse.crawler.dsl;

import org.jmouse.core.Verify;
import org.jmouse.crawler.runtime.CrawlHint;

@FunctionalInterface
public interface CrawlHintAdapter {

    CrawlHint adapt(Object clientHint);

    static CrawlHintAdapter defaultAdapter() {
        return object -> switch (object) {
            case null -> null;
            case CrawlHint hint -> hint;
            case Enum<?> enumValue -> new SimpleHint(enumValue.name());
            case String string -> new SimpleHint(string);
            default -> new SimpleHint(object.toString());
        };
    }

    record SimpleHint(String id) implements CrawlHint {
        public SimpleHint {
            Verify.nonNull(id, "id");
        }
    }
}
