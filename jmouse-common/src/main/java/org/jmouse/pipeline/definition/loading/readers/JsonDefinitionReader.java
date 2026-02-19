package org.jmouse.pipeline.definition.loading.readers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jmouse.common.pipeline.definition.loading.*;
import org.jmouse.common.pipeline.definition.loading.dto.*;
import org.jmouse.pipeline.definition.loading.DefinitionSource;
import org.jmouse.pipeline.definition.loading.PipelineDefinitionReader;
import org.jmouse.pipeline.definition.loading.dto.DTO2DefinitionMapper;
import org.jmouse.pipeline.definition.loading.dto.PipelineDTO;
import org.jmouse.pipeline.definition.model.PipelineDefinition;
import org.jmouse.util.StringHelper;

import java.io.InputStream;

public final class JsonDefinitionReader implements PipelineDefinitionReader {

    private final ObjectMapper         mapper;
    private final DTO2DefinitionMapper toCanonical = new DTO2DefinitionMapper();

    public JsonDefinitionReader(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean supports(DefinitionSource source) {
        return "json".equals(StringHelper.extension(source.location()));
    }

    @Override
    public PipelineDefinition read(DefinitionSource source) {
        try (InputStream inputStream = source.openStream()) {
            PipelineDTO pipelineDTO = mapper.readValue(inputStream, PipelineDTO.class);
            return toCanonical.map(pipelineDTO);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to read JSON definition: " + source.location(), e);
        }
    }
}
