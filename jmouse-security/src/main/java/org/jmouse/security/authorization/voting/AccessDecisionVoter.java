package org.jmouse.security.authorization.voting;

import org.jmouse.security.Authentication;
import org.jmouse.security.access.Vote;

/**
 * 🗳️ Individual access vote.
 */
public interface AccessDecisionVoter<T> {

    Vote vote(Authentication authentication, T target);

}