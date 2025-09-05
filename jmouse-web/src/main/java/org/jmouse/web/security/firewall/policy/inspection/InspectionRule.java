package org.jmouse.web.security.firewall.policy.inspection;

import java.util.regex.Pattern;

/**
 * Represents a single inspection rule.
 * <p>Implemented as sealed interface with type-safe subtypes.</p>
 */
public sealed interface InspectionRule
        permits ContainsRule, RegularExpressionRule {

    /**
     * Factory: case-insensitive "contains".
     */
    static InspectionRule containsIgnoreCase(String id, String needle) {
        return new ContainsRule(id, needle, true);
    }

    /**
     * Factory: case-sensitive "contains".
     */
    static InspectionRule contains(String id, String needle) {
        return new ContainsRule(id, needle, false);
    }

    /**
     * Factory: regex pattern.
     */
    static InspectionRule regularExpression(String id, Pattern pattern) {
        return new RegularExpressionRule(id, pattern);
    }

    /**
     * Identifier for logging/debugging.
     */
    String id();

    /**
     * Evaluates the rule against a given value.
     */
    boolean test(String value);
}
