package org.jmouse.web.security.firewall.policy;

import org.jmouse.util.PathHelper;
import org.jmouse.web.security.firewall.Decision;
import org.jmouse.web.security.firewall.EvaluationInput;
import org.jmouse.web.security.firewall.FirewallPolicy;

/**
 * 🔒 Firewall policy that protects against <b>path traversal attacks</b>.
 *
 * <p>Path traversal (also known as directory traversal) is an attack technique
 * where an attacker attempts to access files and directories outside the web sourceRoot
 * by injecting sequences like {@code ../} into the request path.
 *
 * <p>This policy normalizes the request URI and blocks requests that attempt
 * to move outside the intended directory structure.
 *
 * <h3>Inspection rules</h3>
 * <ul>
 *   <li>If URI normalization fails → block with {@code 400 INVALID PATH}.</li>
 *   <li>If the normalized URI contains {@code /../} → block with {@code 403 PATH TRAVERSAL}.</li>
 *   <li>Otherwise → allow the request.</li>
 * </ul>
 *
 * @see PathHelper
 * @see FirewallPolicy
 */
public class PathTraversalPolicy implements FirewallPolicy {

    /**
     * Applies path traversal detection on the incoming request.
     *
     * @param input encapsulated request and evaluation context
     * @return a {@link Decision} that either blocks or allows the request:
     *         <ul>
     *           <li>{@link Decision#block(int, String)} if traversal attempt is detected</li>
     *           <li>{@link Decision#allow()} if request path is safe</li>
     *         </ul>
     */
    @Override
    public Decision apply(EvaluationInput input) {
        String requestURI = input.requestContext().request().getRequestURI();
        String normalized = PathHelper.normalize(requestURI, true);

        if (normalized == null) {
            return Decision.block(400, "INVALID PATH");
        }

        if (normalized.contains("/../")) {
            return Decision.block(403, "PATH TRAVERSAL");
        }

        return Decision.allow();
    }
}
