package org.jmouse.web.mvc.servlet;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.context.BeanProperties;
import org.jmouse.core.bind.BindName;
import org.jmouse.web.http.HttpStatus;
import org.jmouse.web.security.firewall.*;
import org.jmouse.web.security.firewall.policy.*;

import java.util.List;

/**
 * 🛡️ Servlet-level firewall configuration.
 *
 * <p>Registers a {@link Firewall} with multiple policies:
 * <ul>
 *   <li>📂 {@link PathTraversalPolicy}</li>
 *   <li>⏱️ {@link RequestLimitPolicy}</li>
 *   <li>💉 {@link SqlInjectionPolicy}</li>
 *   <li>🖊️ {@link XssPolicy}</li>
 *   <li>🕵️ {@link SuspiciousUserAgentPolicy}</li>
 * </ul>
 *
 * <pre>{@code
 * Firewall firewall = context.getBean(Firewall.class);
 * Decision d = firewall.evaluate(new EvaluationInput(request));
 * if (d.isBlocked()) {
 *     response.sendError(d.statusCode());
 * }
 * }</pre>
 */
@BeanFactories
public class FirewallBeanConfiguration {

    /**
     * 🔧 Builds the main {@link Firewall} bean with policies.
     *
     * @param properties firewall properties
     * @return configured firewall
     */
    @Bean
    public Firewall firewallBean(Properties properties) {
        InspectionPolicies inspection = properties.getInspectionPolicy();
        return new Firewall(List.of(
                new PathTraversalPolicy(),
                new RequestLimitPolicy(properties.getRateLimit(), properties.getTrustedProxy()),
                new SqlInjectionPolicy(inspection.getInjection(), HttpStatus.NOT_ACCEPTABLE),
                new XssPolicy(inspection.getXss(), HttpStatus.NOT_ACCEPTABLE),
                new SuspiciousUserAgentPolicy(properties.getUntrustedBrowser())
        ));
    }

    /**
     * ⚙️ Firewall configuration properties.
     *
     * <p>Binds values from {@code jmouse.web.servlet.firewall.*}</p>
     *
     * <pre>{@code
     * jmouse.web.servlet.firewall.token-bucket.capacity=50
     * jmouse.web.servlet.firewall.token-bucket.refill-per-sec=10
     * jmouse.web.servlet.firewall.untrustedBrowser[0]=sqlmap
     * jmouse.web.servlet.firewall.inspectionPolicy[xss][expression][svg_tag]=(?i)<\\s*svg\\b
     * }</pre>
     */
    @BeanProperties("jmouse.web.servlet.firewall")
    public static class Properties extends FirewallConfiguration {

        /**
         * 🔑 Injects token bucket settings for {@link RequestLimitPolicy}.
         *
         * @param rateLimit token bucket configuration
         */
        @BindName("token-bucket")
        public void setTokenBucket(RateLimitProperties rateLimit) {
            setRateLimit(rateLimit);
        }
    }
}
