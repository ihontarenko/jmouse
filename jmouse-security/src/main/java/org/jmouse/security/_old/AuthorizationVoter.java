package org.jmouse.security._old;

public interface AuthorizationVoter {
    enum Vote { GRANT, DENY, ABSTAIN }

    Vote vote(Subject subject, Envelope envelope);
}
