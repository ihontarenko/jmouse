package org.jmouse.security;

import java.util.List;

public interface AccessDecisionStrategy {
    /**
     * @param votes ordered list of voter results (parallel to voters)
     * @return final vote to apply (GRANT/DENY/ABSTAIN)
     */
    AuthorizationVoter.Vote decide(List<AuthorizationVoter.Vote> votes);

    /**
     * Optional: short-circuit hint while iterating voters (perf).
     * Return non-null to stop early and use that vote.
     */
    default AuthorizationVoter.Vote tryShortCircuit(List<AuthorizationVoter.Vote> current) { return null; }
}
