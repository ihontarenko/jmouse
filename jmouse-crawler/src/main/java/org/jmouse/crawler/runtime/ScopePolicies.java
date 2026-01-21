package org.jmouse.crawler.runtime;

import org.jmouse.crawler.api.ProcessingTask;
import org.jmouse.crawler.api.ScopePolicy;

public final class ScopePolicies {

    private ScopePolicies() {}

    public static ScopePolicy allowAll() {
        return new AllowAllScopePolicy();
    }

    private static final class AllowAllScopePolicy implements ScopePolicy {

        @Override
        public boolean isAllowed(ProcessingTask task) {
            return true;
        }

        @Override
        public String denyReason(ProcessingTask task) {
            return null;
        }
    }
}
