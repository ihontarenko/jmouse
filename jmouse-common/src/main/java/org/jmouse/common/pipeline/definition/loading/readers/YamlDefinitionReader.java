package org.jmouse.common.pipeline.definition.loading.readers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jmouse.common.pipeline.definition.loading.*;
import org.jmouse.common.pipeline.definition.loading.dto.*;
import org.jmouse.common.pipeline.definition.model.PipelineDefinition;
import org.jmouse.util.StringHelper;

import java.io.InputStream;

public final class YamlDefinitionReader implements PipelineDefinitionReader {

    private final ObjectMapper mapper;
    private final DTO2DefinitionMapper toCanonical = new DTO2DefinitionMapper();

    public YamlDefinitionReader(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean supports(DefinitionSource source) {
        String ext = StringHelper.extension(source.location());
        return "yaml".equals(ext) || "yml".equals(ext);
    }

    @Override
    public PipelineDefinition read(DefinitionSource source) {
        try (InputStream inputStream = source.openStream()) {
            PipelineDTO pipelineDTO = mapper.readValue(inputStream, PipelineDTO.class);
            return toCanonical.map(pipelineDTO);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to read YAML definition: " + source.location(), e);
        }
    }
}
