package org.jmouse.security.authorization.voting;

import org.jmouse.security.authorization.AccessResult;
import org.jmouse.security.authorization.AuthorizationManager;
import org.jmouse.security.core.Authentication;
import org.jmouse.security.core.access.Vote;

import java.util.List;

public class VotingAuthorizationManager<T> implements AuthorizationManager<T> {

    private final List<AccessDecisionVoter<T>> voters;
    private final AccessDecisionStrategy       strategy;

    public VotingAuthorizationManager(List<AccessDecisionVoter<T>> voters, AccessDecisionStrategy strategy) {
        this.voters = voters;
        this.strategy = strategy;
    }

    @Override
    public AccessResult check(Authentication authentication, T target) {
        int grants = 0, denies = 0, abstain = 0;

        for (AccessDecisionVoter<T> voter : getVoters()) {
            Vote vote = voter.vote(authentication, target);

            switch (vote) {
                case GRANT -> grants++;
                case DENY -> denies++;
                case ABSTAIN -> abstain++;
            }

            if (getStrategy() == AccessDecisionStrategy.AFFIRMATIVE && vote == Vote.GRANT) {
                return new AccessResult.Permitted("AFFIRMATIVE STRATEGY PERMIT ACCESS");
            }
        }

        return switch (getStrategy()) {
            case AccessDecisionStrategy.AFFIRMATIVE -> (grants > 0)
                    ? new AccessResult.Permitted("AFFIRMATIVE STRATEGY PERMIT ACCESS")
                    : new AccessResult.Deny("NO AFFIRMATIVE GRANT");
            case AccessDecisionStrategy.CONSENSUS   -> (grants > denies)
                    ? new AccessResult.Permitted("CONSENSUS STRATEGY PERMIT ACCESS")
                    : new AccessResult.Deny("NO CONSENSUS MAJORITY");
            case AccessDecisionStrategy.UNANIMOUS   -> (denies == 0 && grants > 0)
                    ? new AccessResult.Permitted("UNANIMOUS STRATEGY PERMIT ACCESS")
                    : new AccessResult.Deny("NOT UNANIMOUS");
        };
    }

    public List<AccessDecisionVoter<T>> getVoters() {
        return voters;
    }

    public AccessDecisionStrategy getStrategy() {
        return strategy;
    }

}
