package org.jmouse.web.http.values;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Set;

public interface RequestValueProvider {

    String getTargetName();

    Set<String> getRequestValues(HttpServletRequest request);

}
