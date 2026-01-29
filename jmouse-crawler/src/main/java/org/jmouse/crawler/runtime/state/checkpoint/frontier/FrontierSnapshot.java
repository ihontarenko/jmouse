package org.jmouse.crawler.runtime.state.checkpoint.frontier;

import java.util.List;
import java.util.Map;

public record FrontierSnapshot(List<Map<String, Object>> tasks) {

    public static FrontierSnapshot empty() {
        return new FrontierSnapshot(List.of());
    }
}
