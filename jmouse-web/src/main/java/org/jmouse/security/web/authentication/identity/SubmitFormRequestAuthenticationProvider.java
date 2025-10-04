package org.jmouse.security.web.authentication.identity;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.security.authentication.UsernamePasswordAuthentication;
import org.jmouse.security.core.Authentication;
import org.jmouse.security.web.authentication.AbstractAuthenticationProvider;

public class SubmitFormRequestAuthenticationProvider extends AbstractAuthenticationProvider {

    private final String usernameParameter;
    private final String passwordParameter;

    public SubmitFormRequestAuthenticationProvider(String usernameParameter, String passwordParameter) {
        this.usernameParameter = usernameParameter;
        this.passwordParameter = passwordParameter;
    }

    public String getUsernameParameter() {
        return usernameParameter;
    }

    public String getPasswordParameter() {
        return passwordParameter;
    }

    @Override
    protected Authentication doProvide(HttpServletRequest request) {
        String username = request.getParameter(getUsernameParameter());
        String password = request.getParameter(getPasswordParameter());
        return new UsernamePasswordAuthentication(username, password);
    }

}
