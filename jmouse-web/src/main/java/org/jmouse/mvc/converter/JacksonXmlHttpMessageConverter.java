package org.jmouse.mvc.converter;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.jmouse.core.MediaType;

import java.io.IOException;
import java.util.List;

public class JacksonXmlHttpMessageConverter extends AbstractHttpMessageConverter<Object> {

    private static final MediaType MEDIA_TYPE = MediaType.APPLICATION_XML;

    private final XmlMapper xmlMapper;

    public JacksonXmlHttpMessageConverter() {
        super(MEDIA_TYPE);
        this.xmlMapper = new XmlMapper();
    }

    public JacksonXmlHttpMessageConverter(XmlMapper xmlMapper) {
        super(MEDIA_TYPE);
        this.xmlMapper = xmlMapper;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return true;
    }

    @Override
    public void write(Object body, Class<?> type, HttpOutputMessage outputMessage) throws IOException {
        outputMessage.getHeaders().setContentType(MEDIA_TYPE);
        xmlMapper.writeValue(outputMessage.getOutputStream(), body);
    }

    @Override
    public Object read(Class<? extends Object> clazz, HttpInputMessage inputMessage) throws IOException {
        return xmlMapper.readValue(inputMessage.getInputStream(), clazz);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return List.of(MEDIA_TYPE);
    }

}