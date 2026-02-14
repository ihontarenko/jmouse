package org.jmouse.core.parameters;

public sealed interface RequestParametersNode
        permits RequestParametersObjectNode, RequestParametersArrayNode, RequestParametersScalarNode {

    QueryParametersNodeType nodeType();

    enum QueryParametersNodeType { OBJECT, ARRAY, SCALAR }
}
