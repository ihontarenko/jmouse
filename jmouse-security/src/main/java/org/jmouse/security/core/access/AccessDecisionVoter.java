package org.jmouse.security.core.access;

import org.jmouse.security.core.Authentication;

/**
 * 🗳️ Individual access vote.
 */
public interface AccessDecisionVoter<T> {

    Vote vote(Authentication authentication, T target);

    enum Vote {GRANT, DENY}

}