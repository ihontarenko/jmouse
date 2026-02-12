package org.jmouse.web.parameters;

import java.util.List;

public record RequestParametersArrayNode(List<RequestParametersNode> items) implements RequestParametersNode {
    @Override
    public QueryParametersNodeType nodeType() {
        return QueryParametersNodeType.ARRAY;
    }
}
