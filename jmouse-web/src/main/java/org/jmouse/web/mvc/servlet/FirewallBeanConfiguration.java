package org.jmouse.web.mvc.servlet;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.context.BeanProperties;
import org.jmouse.core.bind.BindName;
import org.jmouse.core.net.CIDR;
import org.jmouse.web.security.firewall.Firewall;
import org.jmouse.web.security.firewall.FirewallConfiguration;
import org.jmouse.web.security.firewall.FirewallPolicy;
import org.jmouse.web.security.firewall.RateLimitProperties;
import org.jmouse.web.security.firewall.policy.PathTraversalPolicy;
import org.jmouse.web.security.firewall.policy.RequestLimitPolicy;
import org.jmouse.web.security.firewall.policy.SqlInjectionPolicy;
import org.jmouse.web.security.firewall.policy.XssPolicy;

import java.util.List;

@BeanFactories
public class FirewallBeanConfiguration {

    @Bean
    public Firewall firewallBean(Properties properties) {
        List<FirewallPolicy> policies = List.of(
                new PathTraversalPolicy(),
                new RequestLimitPolicy(properties.getRateLimit(), properties.getTrustedProxyCIDRs()),
                new SqlInjectionPolicy(),
                new XssPolicy()
        );
        return new Firewall(policies);
    }

    @BeanProperties("jmouse.web.servlet.firewall")
    public static class Properties extends FirewallConfiguration {

        @BindName("token-bucket")
        public void setTokenBucket(RateLimitProperties rateLimit) {
           setRateLimit(rateLimit);
        }

        @BindName("trustedProxy")
        public void setTrustedProxy(List<CIDR> proxies) {
            super.setTrustedProxyCIDRs(proxies);
        }
    }

}
