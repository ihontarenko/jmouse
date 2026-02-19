package org.jmouse.pipeline.definition.loading.readers;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.jmouse.pipeline.definition.loading.DefinitionSource;
import org.jmouse.pipeline.definition.loading.PipelineDefinitionReader;
import org.jmouse.pipeline.definition.loading.dto.XmlDTO2DefinitionMapper;
import org.jmouse.pipeline.definition.loading.dto.xml.XmlRootDTO;
import org.jmouse.pipeline.definition.model.PipelineDefinition;

import java.io.InputStream;

import static org.jmouse.util.StringHelper.extension;

public final class XmlDefinitionReader implements PipelineDefinitionReader {

    private final XmlMapper               xml;
    private final XmlDTO2DefinitionMapper mapper;

    public XmlDefinitionReader(XmlMapper xml, XmlDTO2DefinitionMapper mapper) {
        this.xml = xml;
        this.mapper = mapper;
    }

    @Override
    public boolean supports(DefinitionSource source) {
        String ext = extension(source.location());
        return ext.isEmpty() || "xml".equals(ext);
    }

    @Override
    public PipelineDefinition read(DefinitionSource source) {
        try (InputStream inputStream = source.openStream()) {
            XmlRootDTO dto = xml.readValue(inputStream, XmlRootDTO.class);
            return mapper.map(dto);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to read XML definition: " + source.location(), e);
        }
    }
}
