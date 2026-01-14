package org.jmouse.crawler.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class InMemoryDecisionLog implements DecisionLog {

    public record Entry(boolean accepted, String code, String message) {}

    private final List<Entry> entries = new ArrayList<>();

    @Override
    public void accept(String code, String message) {
        entries.add(new Entry(true, code, message));
    }

    @Override
    public void reject(String code, String message) {
        entries.add(new Entry(false, code, message));
    }

    public List<Entry> entries() {
        return Collections.unmodifiableList(entries);
    }
}
