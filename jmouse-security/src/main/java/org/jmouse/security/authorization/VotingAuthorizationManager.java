package org.jmouse.security.authorization;

import org.jmouse.security.core.Authentication;
import org.jmouse.security.core.access.AccessDecisionVoter;

import java.util.List;

public class VotingAuthorizationManager<T> implements AuthorizationManager<T> {

    private final List<AccessDecisionVoter<T>> voters;
    private final AccessDecisionStrategy       strategy;

    public VotingAuthorizationManager(List<AccessDecisionVoter<T>> voters, AccessDecisionStrategy strategy) {
        this.voters = voters;
        this.strategy = strategy;
    }

    @Override
    public AuthorizationDecision check(Authentication authentication, T target) {
        int grants = 0, denies = 0;

        for (AccessDecisionVoter<T> voter : getVoters()) {
            AccessDecisionVoter.Vote vote = voter.vote(authentication, target);

            switch (vote) {
                case GRANT -> grants++;
                case DENY  -> denies++;
            }

            if (getStrategy() == AccessDecisionStrategy.AFFIRMATIVE && vote == AccessDecisionVoter.Vote.GRANT) {
                return AuthorizationDecision.permit("AFFIRMATIVE STRATEGY PERMIT ACCESS");
            }
        }

        return switch (getStrategy()) {
            case AFFIRMATIVE -> (grants > 0)
                    ? AuthorizationDecision.permit("AFFIRMATIVE STRATEGY PERMIT ACCESS")
                    : AuthorizationDecision.deny("NO AFFIRMATIVE GRANT");
            case CONSENSUS   -> (grants > denies)
                    ? AuthorizationDecision.permit("CONSENSUS STRATEGY PERMIT ACCESS")
                    : AuthorizationDecision.deny("NO CONSENSUS MAJORITY");
            case UNANIMOUS   -> (denies == 0 && grants > 0)
                    ? AuthorizationDecision.permit("UNANIMOUS STRATEGY PERMIT ACCESS")
                    : AuthorizationDecision.deny("NOT UNANIMOUS");
        };
    }

    public List<AccessDecisionVoter<T>> getVoters() {
        return voters;
    }

    public AccessDecisionStrategy getStrategy() {
        return strategy;
    }

}
