package org.jmouse.pipeline.definition.loading.dto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PipelineDTO {

    public String                name;
    public Map<String, ChainDTO> chains = new LinkedHashMap<>();

    public static class ChainDTO {
        public String               name;
        public String               initial;
        public Map<String, LinkDTO> links = new LinkedHashMap<>();
    }

    public static class LinkDTO {
        public String        name;
        public ProcessorDTO  processor;
        public PropertiesDTO properties;
    }

    public static class ProcessorDTO {
        public String             className;
        public List<ParameterDTO> parameters;
    }

    public static class PropertiesDTO {
        public Map<String, String> transitions   = new LinkedHashMap<>();
        public Map<String, String> configuration = new LinkedHashMap<>();
        public String              fallback;
    }

    public static class ParameterDTO {
        public String name;
        public String value;
        public String resolver;
        public String converter;
    }
}
