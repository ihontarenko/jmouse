package org.jmouse.web.security.firewall.policy;

import org.jmouse.web.http.HttpStatus;
import org.jmouse.web.http.request.values.RequestValues;
import org.jmouse.web.security.firewall.Decision;
import org.jmouse.web.security.firewall.EvaluationInput;
import org.jmouse.web.security.firewall.FirewallPolicy;
import org.jmouse.web.security.firewall.InspectionPolicies;

import java.util.*;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

/**
 * Abstract base class for inspection policies.
 *
 * <p>Collects {@link InspectionRule}s from {@link InspectionPolicies.InspectionGroup}
 * and applies them against request values extracted from {@link EvaluationInput}.</p>
 *
 * <p>❗ Extending classes should only override {@link #extractValues(EvaluationInput)}
 * if they want to inspect a different set of request sources.</p>
 */
public abstract class AbstractInspectionPolicy implements FirewallPolicy {

    /**
     * Maximum inspected value length (longer strings will be truncated).
     */
    protected static final int MAX_VALUE_LENGTH = 4_096;

    private final List<InspectionRule> rules;
    private final HttpStatus           blockStatus;
    private final String               policyName;

    /**
     * Creates a new inspection policy.
     *
     * @param group       inspection group providing "contains" and "regex" definitions
     * @param blockStatus HTTP status returned when a match is detected
     * @param policyName  logical name of the policy (used for logging/audit)
     */
    protected AbstractInspectionPolicy(InspectionPolicies.InspectionGroup group, HttpStatus blockStatus, String policyName) {
        this.blockStatus = requireNonNull(blockStatus, "blockStatus");
        this.policyName = requireNonNull(policyName, "policyName");

        requireNonNull(group, "group");
        final List<InspectionRule> assembled = new ArrayList<>();

        // contains rules
        for (Map.Entry<String, String> e : group.getContains().entrySet()) {
            assembled.add(InspectionRule.containsIgnoreCase(e.getKey(), e.getValue()));
        }

        // regex rules
        for (Map.Entry<String, Pattern> e : group.getExpression().entrySet()) {
            assembled.add(InspectionRule.regex(e.getKey(), e.getValue()));
        }

        this.rules = Collections.unmodifiableList(assembled);
    }

    /**
     * Applies the inspection against request values.
     *
     * @param input request evaluation input
     * @return {@link Decision#block(HttpStatus, String)} if any rule matches,
     * otherwise {@link Decision#allow()}
     */
    @Override
    public final Decision apply(EvaluationInput input) {
        final Map<String, Set<String>> bySource = extractValues(input);

        if (bySource == null || bySource.isEmpty()) {
            return Decision.allow();
        }

        for (Map.Entry<String, Set<String>> source : bySource.entrySet()) {
            final String sourceKey = source.getKey();
            for (String raw : source.getValue()) {
                final String value = normalize(raw);

                if (value == null) {
                    continue;
                }

                for (InspectionRule rule : rules) {
                    if (rule.test(value)) {
                        return Decision.block(blockStatus, "BLOCKED BY %s[%s]; SOURCE=%s"
                                .formatted(policyName, rule.id(), sourceKey));
                    }
                }
            }
        }
        return Decision.allow();
    }

    /**
     * Extracts request values for inspection.
     * <p>By default, delegates to {@link RequestValues#defaults()}.</p>
     *
     * @param input evaluation input
     * @return map of source → values
     */
    protected Map<String, Set<String>> extractValues(EvaluationInput input) {
        return RequestValues.defaults().getStringMap(input.requestContext().request());
    }

    /**
     * Normalizes inspected string values.
     * <ul>
     *     <li>Trims whitespace</li>
     *     <li>Skips empty strings</li>
     *     <li>Truncates values above {@link #MAX_VALUE_LENGTH}</li>
     * </ul>
     */
    protected String normalize(String string) {
        if (string == null) {
            return null;
        }

        final String trimmed = string.trim();

        if (trimmed.isEmpty()) {
            return null;
        }

        return trimmed.length() > MAX_VALUE_LENGTH ? trimmed.substring(0, MAX_VALUE_LENGTH) : trimmed;
    }

    // ---------------------------------------------------------------------
    // Rule abstraction
    // ---------------------------------------------------------------------

    /**
     * Represents a single inspection rule.
     * <p>Implemented as sealed interface with type-safe subtypes.</p>
     */
    public sealed interface InspectionRule permits AbstractInspectionPolicy.ContainsRule, RegularExpressionRule {

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
        static InspectionRule regex(String id, Pattern pattern) {
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

    /**
     * "Contains" inspection rule.
     * <p>Checks whether a string contains a given substring.</p>
     */
    static final class ContainsRule implements InspectionRule {
        private final String  id;
        private final String  needle;
        private final boolean ignoreCase;

        ContainsRule(String id, String needle, boolean ignoreCase) {
            this.id = requireNonNull(id);
            this.needle = requireNonNull(needle);
            this.ignoreCase = ignoreCase;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public boolean test(String value) {
            if (value == null) return false;
            return ignoreCase ? value.toLowerCase(Locale.ROOT)
                    .contains(needle.toLowerCase(Locale.ROOT)) : value.contains(needle);
        }
    }

    /**
     * Regular expression inspection rule.
     * <p>Uses a pre-compiled {@link Pattern} and matches with {@link java.util.regex.Matcher#find()}.</p>
     */
    static final class RegularExpressionRule implements InspectionRule {

        private final String  id;
        private final Pattern pattern;

        RegularExpressionRule(String id, Pattern pattern) {
            this.id = requireNonNull(id);
            this.pattern = requireNonNull(pattern);
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public boolean test(String value) {
            return value != null && pattern.matcher(value).find();
        }
    }
}
