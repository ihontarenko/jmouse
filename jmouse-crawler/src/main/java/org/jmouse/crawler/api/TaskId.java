package org.jmouse.crawler.api;

import org.jmouse.core.IdGenerator;
import org.jmouse.core.PrefixedIdGenerator;

public record TaskId(String value) {

    public static final IdGenerator<String, String> ID_GENERATOR = PrefixedIdGenerator.defaultGenerator();

    public static TaskId random() {
        return new TaskId(ID_GENERATOR.generate());
    }
}
