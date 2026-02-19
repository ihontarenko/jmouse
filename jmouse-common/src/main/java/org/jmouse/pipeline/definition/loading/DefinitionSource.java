package org.jmouse.pipeline.definition.loading;

import java.io.InputStream;
import java.util.Map;

public interface DefinitionSource {
    String location();                 // e.g. "classpath:/pipeline.xml"
    InputStream openStream();
    default Map<String, Object> attributes() { return Map.of(); }
}