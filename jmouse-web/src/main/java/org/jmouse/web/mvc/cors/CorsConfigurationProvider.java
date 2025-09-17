package org.jmouse.web.mvc.cors;

import jakarta.servlet.http.HttpServletRequest;

public interface CorsConfigurationProvider {

    CorsConfiguration getCorsConfiguration(HttpServletRequest request, Object handler);

}
