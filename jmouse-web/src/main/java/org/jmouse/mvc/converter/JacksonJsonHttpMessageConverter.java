package org.jmouse.mvc.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jmouse.core.MediaType;

import java.io.IOException;
import java.util.List;

public class JacksonJsonHttpMessageConverter extends AbstractHttpMessageConverter<Object> {

    private static final MediaType MEDIA_TYPE = MediaType.APPLICATION_JSON;

    private final ObjectMapper objectMapper;

    public JacksonJsonHttpMessageConverter() {
        super(MEDIA_TYPE);
        this.objectMapper = new ObjectMapper();
    }

    public JacksonJsonHttpMessageConverter(ObjectMapper objectMapper) {
        super(MEDIA_TYPE);
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return super.canWrite(clazz, mediaType);
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return super.canRead(clazz, mediaType);
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return true;
    }

    @Override
    public void write(Object body, Class<?> type, HttpOutputMessage outputMessage) throws IOException, UnwritableException {
        outputMessage.getHeaders().setContentType(MEDIA_TYPE);
        objectMapper.writeValue(outputMessage.getOutputStream(), body);
    }

    @Override
    public Object read(Class<? extends Object> clazz, HttpInputMessage inputMessage) throws IOException {
        return objectMapper.readValue(inputMessage.getInputStream(), clazz);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return List.of(MEDIA_TYPE);
    }
}
