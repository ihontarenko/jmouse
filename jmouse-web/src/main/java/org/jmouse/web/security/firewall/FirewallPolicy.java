package org.jmouse.web.security.firewall;

public interface FirewallPolicy {

    Decision apply(EvaluationInput evaluationInput);

}
