package org.jmouse.security.web.session;

import static org.jmouse.security.web.session.SessionCreationPolicy.IF_REQUIRED;

public record SessionSettings(SessionCreationPolicy policy, boolean urlRewriting) {

    public SessionSettings(SessionCreationPolicy policy, boolean urlRewriting) {
        this.policy = (policy != null) ? policy : IF_REQUIRED;
        this.urlRewriting = urlRewriting;
    }

}
