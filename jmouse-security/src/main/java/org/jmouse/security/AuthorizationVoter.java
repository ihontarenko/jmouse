package org.jmouse.security;

public interface AuthorizationVoter {
    enum Vote { GRANT, DENY, ABSTAIN }

    Vote vote(Subject subject, Envelope envelope);
}
