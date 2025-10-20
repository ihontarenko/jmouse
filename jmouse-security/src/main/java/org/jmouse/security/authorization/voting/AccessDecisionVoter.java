package org.jmouse.security.authorization.voting;

import org.jmouse.security.core.Authentication;
import org.jmouse.security.core.access.Vote;

/**
 * 🗳️ Individual access vote.
 */
public interface AccessDecisionVoter<T> {

    Vote vote(Authentication authentication, T target);

}