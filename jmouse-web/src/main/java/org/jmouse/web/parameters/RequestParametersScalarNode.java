package org.jmouse.web.parameters;

import java.util.List;

public record RequestParametersScalarNode(List<String> values) implements RequestParametersNode {
    @Override
    public QueryParametersNodeType nodeType() {
        return QueryParametersNodeType.SCALAR;
    }
}
