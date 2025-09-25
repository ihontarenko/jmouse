package org.jmouse.security.pipeline;

import org.jmouse.core.chain.Chain;
import org.jmouse.security.Envelope;
import org.jmouse.security.Subject;

import java.util.Map;
import java.util.Set;

public class RBACAuthorizer implements Authorizer {

    private final Set<String> roles;

    public RBACAuthorizer(Set<String> roles) {
        this.roles = Set.copyOf(roles);
    }

    @Override
    public Result authorize(Subject subject, Envelope envelope, Chain<Subject, Envelope, Result> next) {
        if (subject == null || subject.authorities() == null) {
            return new Result.Deny("RBAC_NO_SUBJECT", "No subject or roles found", Map.of());
        }

        for (String role : subject.authorities()) {
            if (roles.contains(role)) {
                return new Result.Permit(Map.of());
            }
        }

        return new Result.Deny("RBAC_FORBIDDEN", "Required role not present", Map.of());
    }

}
