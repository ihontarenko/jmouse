package org.jmouse.web.security.firewall;

import org.jmouse.core.net.CIDR;

import java.util.List;
import java.util.Set;

public class FirewallConfiguration {

    private Set<String>         blockedUserAgentsFragments; // sqlmap,nikto,acunetix
    private Set<String>         blockedCountries;
    private List<CIDR>          trustedProxyCIDRs; // "10.0.0.0/8","172.16.0.0/12"
    private RateLimitProperties rateLimit;

    public RateLimitProperties getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimitProperties rateLimit) {
        this.rateLimit = rateLimit;
    }

    public Set<String> getBlockedUserAgentsFragments() {
        return blockedUserAgentsFragments;
    }

    public void setBlockedUserAgentsFragments(Set<String> blockedUserAgentsFragments) {
        this.blockedUserAgentsFragments = blockedUserAgentsFragments;
    }

    public Set<String> getBlockedCountries() {
        return blockedCountries;
    }

    public void setBlockedCountries(Set<String> blockedCountries) {
        this.blockedCountries = blockedCountries;
    }

    public List<CIDR> getTrustedProxyCIDRs() {
        return trustedProxyCIDRs;
    }

    public void setTrustedProxyCIDRs(List<CIDR> trustedProxyCIDRs) {
        this.trustedProxyCIDRs = trustedProxyCIDRs;
    }

}
