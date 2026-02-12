package org.jmouse.web.binding;

import org.jmouse.core.access.TypedValue;
import org.jmouse.core.mapping.Mapper;
import org.jmouse.web.parameters.RequestParametersJavaStructureConverter;
import org.jmouse.web.parameters.RequestParametersTree;
import org.jmouse.web.parameters.RequestParametersTreeParser;

import java.util.Map;

public final class DefaultParametersDataBinder implements ParametersDataBinder {

    private final String                                  objectName;
    private final Map<String, String[]>                   parameters;
    private final Mapper                                  mapper;
    private final RequestParametersTreeParser             treeParser;
    private final RequestParametersJavaStructureConverter structureConverter;

    public DefaultParametersDataBinder(
            String objectName,
            Map<String, String[]> parameters,
            Mapper mapper,
            RequestParametersTreeParser treeParser,
            RequestParametersJavaStructureConverter structureConverter
    ) {
        this.objectName = objectName;
        this.parameters = parameters;
        this.mapper = mapper;
        this.treeParser = treeParser;
        this.structureConverter = structureConverter;
    }

    @Override
    public String getObjectName() {
        return objectName;
    }

    @Override
    public Map<String, String[]> getParameters() {
        return parameters;
    }

    @Override
    public <T> T bind(TypedValue<T> target) {
        RequestParametersTree tree   = treeParser.parse(parameters);
        Object                source = structureConverter.toJavaObject(tree);
        return mapper.map(source, target);
    }
}
