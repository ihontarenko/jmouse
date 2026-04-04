package org.jmouse.security.web.authentication;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.security.Authentication;

public interface IdentityRestoreService {

    Authentication autoLogin(HttpServletRequest request);

    void success(HttpServletRequest request, Authentication authentication);

    void failure(HttpServletRequest request);

}
