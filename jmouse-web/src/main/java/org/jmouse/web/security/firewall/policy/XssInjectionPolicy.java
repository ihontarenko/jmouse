package org.jmouse.web.security.firewall.policy;

import org.jmouse.web.http.HttpStatus;
import org.jmouse.web.security.firewall.InspectionPolicies;

/**
 * 🛡️ Firewall policy that protects against <b>Cross-Site Scripting (XSS)</b> attacks.
 *
 * <p>XSS vulnerabilities allow attackers to inject malicious JavaScript or HTML
 * content into web pages viewed by other users. This can lead to session hijacking,
 * credential theft, or arbitrary script execution in the victim's browser.</p>
 *
 * <h3>Inspection rules</h3>
 * <ul>
 *   <li>Uses configured {@link InspectionPolicies.InspectionGroup} to match suspicious patterns.</li>
 *   <li>Supports both <b>contains checks</b> (dangerous substrings) and <b>regex expressions</b>
 *       for flexible detection.</li>
 *   <li>If a suspicious match is found → request is blocked with given {@link HttpStatus}
 *       and reason {@code "XSS ATTACK"}.</li>
 * </ul>
 *
 * <h3>Examples of detected payloads</h3>
 * <pre>
 *   &lt;script&gt;alert('XSS')&lt;/script&gt;
 *   &lt;img src=x onerror=alert(1)&gt;
 *   javascript:alert('XSS')
 * </pre>
 *
 * @see AbstractInspectionPolicy
 * @see InspectionPolicies
 */
public class XssInjectionPolicy extends AbstractInspectionPolicy {

    /**
     * Creates an XSS detection policy with the given inspection group and status.
     *
     * @param group  inspection rules for XSS detection (contains & regex patterns)
     * @param status HTTP status code to return when blocking the request
     */
    public XssInjectionPolicy(InspectionPolicies.InspectionGroup group, HttpStatus status) {
        super(group, status, "XSS ATTACK");
    }

}
