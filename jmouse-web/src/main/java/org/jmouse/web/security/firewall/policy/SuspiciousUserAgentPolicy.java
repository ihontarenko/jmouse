package org.jmouse.web.security.firewall.policy;

import org.jmouse.web.http.HttpStatus;
import org.jmouse.web.http.request.WebRequest;
import org.jmouse.web.security.firewall.Decision;
import org.jmouse.web.security.firewall.EvaluationInput;
import org.jmouse.web.security.firewall.FirewallPolicy;

import java.util.Set;

/**
 * 🕵️ Firewall policy that detects suspicious <b>User-Agent</b> headers.
 *
 * <p>This policy checks the incoming request's {@code User-Agent} header against
 * a configured list of forbidden fragments. If a match is found, the request is blocked.</p>
 *
 * <h3>Use cases</h3>
 * <ul>
 *   <li>Blocking known malicious bots, crawlers, or scanners.</li>
 *   <li>Detecting outdated or untrusted browsers.</li>
 *   <li>Preventing automated attacks that rely on fake User-Agent strings.</li>
 * </ul>
 *
 * <h3>Inspection rules</h3>
 * <ul>
 *   <li>Iterates through configured {@link #fragments} (substrings).</li>
 *   <li>If the request {@code User-Agent} contains any fragment → block with
 *       {@link HttpStatus#NOT_ACCEPTABLE} and reason
 *       {@code "SUSPICIOUS USER-AGENT FRAGMENT: '<fragment>'"}.</li>
 *   <li>Otherwise → request is allowed.</li>
 * </ul>
 *
 * <h3>Example configuration</h3>
 * <pre>
 * jmouse.web.servlet.firewall.untrustedBrowser[0] = curl
 * jmouse.web.servlet.firewall.untrustedBrowser[1] = sqlmap
 * jmouse.web.servlet.firewall.untrustedBrowser[2] = nmap
 * </pre>
 *
 * @see FirewallPolicy
 */
public class SuspiciousUserAgentPolicy implements FirewallPolicy {

    /** Set of suspicious User-Agent substrings to block. */
    private final Set<String> fragments;

    /**
     * Creates a policy that blocks requests containing forbidden User-Agent fragments.
     *
     * @param fragments set of suspicious User-Agent substrings
     */
    public SuspiciousUserAgentPolicy(Set<String> fragments) {
        this.fragments = fragments;
    }

    /**
     * Applies User-Agent inspection.
     *
     * @param input encapsulated request and evaluation context
     * @return a {@link Decision} to block or allow the request
     */
    @Override
    public Decision apply(EvaluationInput input) {
        if (input.requestContext().request() instanceof WebRequest webRequest) {
            String userAgent = webRequest.getHeaders().getUserAgent();
            for (String fragment : fragments) {
                if (userAgent.contains(fragment)) {
                    return Decision.block(
                            HttpStatus.NOT_ACCEPTABLE,
                            "SUSPICIOUS USER-AGENT FRAGMENT: '%s'".formatted(fragment)
                    );
                }
            }
        }
        return Decision.allow();
    }
}
