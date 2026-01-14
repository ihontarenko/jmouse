package org.jmouse.crawler.runtime;

import org.jmouse.crawler.spi.ScopePolicy;

public final class ScopePolicies {

    private ScopePolicies() {}

    public static ScopePolicy allowAll() {
        return new AllowAllScopePolicy();
    }

    private static final class AllowAllScopePolicy implements ScopePolicy {

        @Override
        public boolean isAllowed(CrawlTask task) {
            return true;
        }

        @Override
        public String denyReason(CrawlTask task) {
            return null;
        }
    }
}
