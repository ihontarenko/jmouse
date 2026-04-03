package org.jmouse.web.http;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.parameters.RequestParametersJavaStructureConverter;
import org.jmouse.core.parameters.RequestParametersJavaStructureOptions;
import org.jmouse.core.parameters.RequestParametersTree;
import org.jmouse.core.parameters.RequestParametersTreeParser;

import java.util.Map;

public record RequestParameters(Map<String, Object> parameters) {

    public static final String REQUEST_PARAMETERS_ATTRIBUTE = RequestParameters.class.getName() + ".REQUEST_PARAMETERS";

    public static RequestParameters ofRequest(HttpServletRequest request) {
        RequestParametersTreeParser parser = new RequestParametersTreeParser();
        RequestParametersTree       tree   = parser.parse(request.getParameterMap());

        RequestParametersJavaStructureConverter converter = new RequestParametersJavaStructureConverter(
                RequestParametersJavaStructureOptions.defaults());

        Map<String, Object> parameters = converter.toMap(tree);

        return new RequestParameters(parameters);
    }

    public <T> T getParameter(String name, Class<T> type) {
        return type.cast(parameters.get(name));
    }

}
