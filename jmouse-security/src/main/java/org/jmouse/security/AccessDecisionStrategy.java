package org.jmouse.security;

import java.util.List;

public interface AccessDecisionStrategy {
    Decision decide(List<Decision> votes);
}
