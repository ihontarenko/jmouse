package org.jmouse.web.security.firewall;

import org.jmouse.web.security.firewall.policy.XssInjectionPolicy;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 🔍 Central container for firewall inspection policies.
 *
 * <p>This class defines groups of security inspection rules that can be applied
 * by different {@code FirewallPolicy} implementations (e.g. SQL Injection, XSS).
 * Each {@link InspectionGroup} contains:
 * <ul>
 *   <li><b>expression</b>: regex-based detection patterns</li>
 *   <li><b>contains</b>: simple substring checks</li>
 * </ul>
 *
 * <p>Configuration for these rules is usually provided via application properties
 * or external security configuration files.</p>
 *
 * <h3>Example configuration</h3>
 * <pre>
 * jmouse.web.servlet.firewall.inspectionPolicy[injection][expression][union_select] = (?i)union\s+select
 * jmouse.web.servlet.firewall.inspectionPolicy[injection][contains][comment.inline] = --
 * jmouse.web.servlet.firewall.inspectionPolicy[xss][expression][script_tag] = (?i)&lt;script
 * jmouse.web.servlet.firewall.inspectionPolicy[xss][contains][proto_js] = proto_js
 * </pre>
 *
 * @see org.jmouse.web.security.firewall.policy.AbstractInspectionPolicy
 * @see org.jmouse.web.security.firewall.policy.SqlInjectionPolicy
 * @see XssInjectionPolicy
 */
public class InspectionPolicies {

    /** Inspection rules for SQL Injection detection. */
    private InspectionGroup injection = new InspectionGroup();

    /** Inspection rules for XSS detection. */
    private InspectionGroup xss = new InspectionGroup();

    public InspectionGroup getInjection() {
        return injection;
    }

    public void setInjection(InspectionGroup injection) {
        this.injection = injection;
    }

    public InspectionGroup getXss() {
        return xss;
    }

    public void setXss(InspectionGroup xss) {
        this.xss = xss;
    }

    /**
     * Represents a group of inspection rules.
     *
     * <p>Each group supports two detection strategies:
     * <ul>
     *   <li>{@link #expression} — regex-based patterns for advanced detection.</li>
     *   <li>{@link #contains} — substring-based checks for lightweight detection.</li>
     * </ul>
     */
    public static class InspectionGroup {

        /** Regex-based inspection rules (key = name, value = compiled matched). */
        private Map<String, Pattern> expression = new LinkedHashMap<>();

        /** Substring-based inspection rules (key = name, value = substring to detect). */
        private Map<String, String> contains = new LinkedHashMap<>();

        public Map<String, Pattern> getExpression() {
            return expression;
        }

        public void setExpression(Map<String, Pattern> expression) {
            this.expression = expression;
        }

        public Map<String, String> getContains() {
            return contains;
        }

        public void setContains(Map<String, String> contains) {
            this.contains = contains;
        }
    }
}
