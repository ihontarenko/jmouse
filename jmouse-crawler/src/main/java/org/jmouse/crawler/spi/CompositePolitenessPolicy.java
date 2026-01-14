package org.jmouse.crawler.spi;

import org.jmouse.core.Verify;

import java.net.URI;
import java.time.Instant;
import java.util.List;

public final class CompositePolitenessPolicy implements PolitenessPolicy {

    private final List<PolitenessPolicy> policies;

    public CompositePolitenessPolicy(List<PolitenessPolicy> policies) {
        this.policies = List.copyOf(Verify.nonNull(policies, "policies"));
    }

    @Override
    public Instant notBefore(URI url, Instant now) {
        Instant temporary = now;

        for (PolitenessPolicy politenessPolicy : policies) {
            Instant notBefore = politenessPolicy.notBefore(url, now);
            if (notBefore != null && notBefore.isAfter(temporary)) {
                temporary = notBefore;
            }
        }

        return temporary;
    }
}
