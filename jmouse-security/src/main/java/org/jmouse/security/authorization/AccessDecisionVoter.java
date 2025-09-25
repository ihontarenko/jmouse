package org.jmouse.security.authorization;

/**
 * 🗳️ Individual access vote.
 */
public interface AccessDecisionVoter<T> {

    Vote vote(Authentication authentication, T target);

    enum Vote {GRANT, DENY}

}