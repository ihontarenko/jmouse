package org.jmouse.core.parameters;

import java.util.Map;

public record RequestParametersObjectNode(Map<String, RequestParametersNode> fields)
        implements RequestParametersNode {
    @Override
    public QueryParametersNodeType nodeType() {
        return QueryParametersNodeType.OBJECT;
    }
}
