package org.jmouse.web.binding;

import java.util.Map;

public interface ParametersDataBinderFactory {

    /**
     * 🏭 Create a binder for the given parameters and logical name.
     *
     * @param objectName logical name (e.g. "filter", "user", "form")
     * @param parameters raw request parameters
     */
    ParametersDataBinder createBinder(String objectName, Map<String, String[]> parameters);
}
