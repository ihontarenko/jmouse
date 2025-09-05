package org.jmouse.web.security.firewall.policy.inspection;

import java.util.Locale;

record ContainsRule(String id, String needle, boolean ignoreCase) implements InspectionRule {

    @Override
    public boolean test(String value) {
        return ignoreCase ? value.toLowerCase(Locale.ROOT)
                .contains(needle.toLowerCase(Locale.ROOT)) : value.contains(needle);
    }

    @Override
    public String toString() {
        return "CONTAINS[%s]: %s".formatted(id, needle);
    }

}
