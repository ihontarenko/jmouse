package org.jmouse.web.security.firewall.policy;

import org.jmouse.web.http.HttpStatus;
import org.jmouse.web.http.request.values.RequestValues;
import org.jmouse.web.security.firewall.Decision;
import org.jmouse.web.security.firewall.EvaluationInput;
import org.jmouse.web.security.firewall.FirewallPolicy;
import org.jmouse.web.security.firewall.InspectionPolicies;
import org.jmouse.web.security.firewall.policy.inspection.InspectionRule;

import java.net.URLDecoder;
import java.util.*;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

/**
 * Abstract base class for inspection policies.
 *
 * <p>Collects {@link InspectionRule}s from {@link InspectionPolicies.InspectionGroup}
 * and applies them against request values extracted from {@link EvaluationInput}.</p>
 *
 * <p>❗ Extending classes should only override {@link #requestValues(EvaluationInput)}
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
            assembled.add(InspectionRule.regularExpression(e.getKey(), e.getValue()));
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
        final Map<String, Set<String>> values = requestValues(input);

        if (values == null || values.isEmpty()) {
            return Decision.allow();
        }

        for (Map.Entry<String, Set<String>> source : values.entrySet()) {
            for (String raw : source.getValue()) {
                final String value = normalize(raw);

                if (value == null) {
                    continue;
                }

                for (InspectionRule rule : rules) {
                    if (rule.test(value)) {
                        return Decision.block(blockStatus, "BLOCKED BY %s[%s]; SOURCE=%s"
                                .formatted(policyName, rule.id(), source.getKey()));
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
    protected Map<String, Set<String>> requestValues(EvaluationInput input) {
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

        String trimmed = string.trim();

        if (trimmed.isEmpty()) {
            return null;
        }

        if (trimmed.contains("%")) {
            trimmed = URLDecoder.decode(trimmed, UTF_8);
        }

        return trimmed.length() > MAX_VALUE_LENGTH ? trimmed.substring(0, MAX_VALUE_LENGTH) : trimmed;
    }

}
