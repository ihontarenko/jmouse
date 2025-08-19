package org.jmouse.web.security.firewall.policy;

import org.jmouse.web.security.firewall.Decision;
import org.jmouse.web.security.firewall.EvaluationInput;
import org.jmouse.web.security.firewall.FirewallPolicy;

public class XssPolicy implements FirewallPolicy {

    @Override
    public Decision apply(EvaluationInput evaluationInput) {
        return null;
    }

}
