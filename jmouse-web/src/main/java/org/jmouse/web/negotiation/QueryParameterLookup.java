package org.jmouse.web.negotiation;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.MediaType;

import java.util.List;

public class QueryParameterLookup extends MapppedMediaTypeLookup {

    private String parameterName = "format";

    public QueryParameterLookup() {}

    public QueryParameterLookup(String parameterName) {
        this.parameterName = parameterName;
    }

    @Override
    public List<MediaType> lookup(HttpServletRequest request) {
        return List.of(getExtension(request.getParameter(parameterName)));
    }

}
