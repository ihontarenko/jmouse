package org.jmouse.security;

public interface AuthorizationVoter {
    Decision vote(Subject subject, Envelope envelope);
}
