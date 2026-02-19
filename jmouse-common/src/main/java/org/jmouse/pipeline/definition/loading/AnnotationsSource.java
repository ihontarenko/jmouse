package org.jmouse.pipeline.definition.loading;

import java.io.InputStream;
import java.util.Map;

public final class AnnotationsSource implements DefinitionSource {

    private final String     pipelineName;
    private final Class<?>[] baseClasses;

    public AnnotationsSource(String pipelineName, Class<?>... baseClasses) {
        this.pipelineName = pipelineName;
        this.baseClasses = baseClasses;
    }

    @Override
    public String location() {
        return "annotations:" + pipelineName;
    }

    @Override
    public InputStream openStream() {
        throw new UnsupportedOperationException("AnnotationsSource has no stream");
    }

    @Override
    public Map<String, Object> attributes() {
        return Map.of("type", "annotations", "pipelineName", pipelineName, "baseClasses", baseClasses);
    }
}
