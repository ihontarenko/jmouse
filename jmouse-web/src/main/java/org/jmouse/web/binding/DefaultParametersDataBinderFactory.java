package org.jmouse.web.binding;

import org.jmouse.core.mapping.Mapper;
import org.jmouse.web.parameters.RequestParametersJavaStructureConverter;
import org.jmouse.web.parameters.RequestParametersTreeParser;

import java.util.Map;

public final class DefaultParametersDataBinderFactory implements ParametersDataBinderFactory {

    private final RequestParametersJavaStructureConverter structureConverter;
    private final Mapper                                  mapper;
    private final RequestParametersTreeParser             treeParser;

    public DefaultParametersDataBinderFactory(
            Mapper mapper, RequestParametersTreeParser treeParser, RequestParametersJavaStructureConverter structureConverter
    ) {
        this.mapper = mapper;
        this.structureConverter = structureConverter;
        this.treeParser = treeParser;
    }

    @Override
    public ParametersDataBinder createBinder(String objectName, Map<String, String[]> parameters) {
        return new DefaultParametersDataBinder(
                objectName,
                parameters,
                mapper,
                treeParser,
                structureConverter
        );
    }
}
