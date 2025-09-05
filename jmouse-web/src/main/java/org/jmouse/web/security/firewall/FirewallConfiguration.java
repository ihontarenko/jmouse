package org.jmouse.web.security.firewall;

import org.jmouse.core.net.CIDR;
import org.jmouse.geo.Country;

import java.util.List;
import java.util.Set;

public class FirewallConfiguration {

    private Set<String>         untrustedBrowser;
    private List<Country>       blockedCountries;
    private List<CIDR>          trustedProxy;       // 10.0.0.0/8, 172.16.0.0/12
    private RateLimitProperties rateLimit;
    private InspectionPolicies  inspectionPolicy;

    public RateLimitProperties getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimitProperties rateLimit) {
        this.rateLimit = rateLimit;
    }

    public Set<String> getUntrustedBrowser() {
        return untrustedBrowser;
    }

    public void setUntrustedBrowser(Set<String> untrustedBrowser) {
        this.untrustedBrowser = untrustedBrowser;
    }

    public List<Country> getBlockedCountries() {
        return blockedCountries;
    }

    public void setBlockedCountries(List<Country> blockedCountries) {
        this.blockedCountries = blockedCountries;
    }

    public List<CIDR> getTrustedProxy() {
        return trustedProxy;
    }

    public void setTrustedProxy(List<CIDR> trustedProxy) {
        this.trustedProxy = trustedProxy;
    }

    public InspectionPolicies getInspectionPolicy() {
        return inspectionPolicy;
    }

    public void setInspectionPolicy(InspectionPolicies inspectionPolicies) {
        this.inspectionPolicy = inspectionPolicies;
    }

}
