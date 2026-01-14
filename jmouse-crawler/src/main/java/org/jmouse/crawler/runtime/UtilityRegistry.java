package org.jmouse.crawler.runtime;

public interface UtilityRegistry {

    <T> T get(Class<T> type);

    static UtilityRegistry empty() {
        return new UtilityRegistry() {
            @Override
            public <T> T get(Class<T> type) {
                return null;
            }
        };
    }

}
