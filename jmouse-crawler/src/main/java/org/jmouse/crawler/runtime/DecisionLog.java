package org.jmouse.crawler.runtime;

public interface DecisionLog {
    void accept(String code, String message);
    void reject(String code, String message);
}
