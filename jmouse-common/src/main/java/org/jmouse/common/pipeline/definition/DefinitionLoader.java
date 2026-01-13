package org.jmouse.common.pipeline.definition;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.jmouse.common.ResourceReader;

public abstract class DefinitionLoader {

    abstract public RootDefinition load(String source);

    enum FileType {
        XML, JSON, YAML, YML, PROPERTIES
    }

    public static DefinitionLoader createLoader(String file) {
        final int lastIndex = file.lastIndexOf('.');
        FileType  fileType  = FileType.valueOf(file.substring(lastIndex + 1).toUpperCase());

        return switch (fileType) {
            case XML -> new XML();
            case JSON -> new JSON();
            case PROPERTIES -> new JavaProperties();
            case YAML, YML -> new YAML();
        };
    }

    public RootDefinition readValue(ObjectMapper mapper, String source) {
        try {
            return mapper.readValue(ResourceReader.readFileToString(source), RootDefinition.class);
        } catch (JsonProcessingException e) {
            throw new PipelineDefinitionException(e);
        }
    }

    public static class XML extends DefinitionLoader {

        @Override
        public RootDefinition load(String source) {
            return readValue(new XmlMapper(), source);
        }

    }

    public static class JSON extends DefinitionLoader {

        @Override
        public RootDefinition load(String source) {
            return readValue(new ObjectMapper(), source);
        }

    }

    public static class YAML extends DefinitionLoader {

        @Override
        public RootDefinition load(String source) {
            return readValue(new YAMLMapper(), source);
        }

    }

    public static class JavaProperties extends DefinitionLoader {

        @Override
        public RootDefinition load(String source) {
            return readValue(new JavaPropsMapper(), source);
        }

    }

}
