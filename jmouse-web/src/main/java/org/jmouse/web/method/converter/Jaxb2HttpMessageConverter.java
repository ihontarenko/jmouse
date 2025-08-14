package org.jmouse.web.method.converter;

import jakarta.xml.bind.*;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import org.jmouse.core.MediaType;
import org.jmouse.core.Priority;

import java.io.IOException;
import java.util.List;

@Priority(Integer.MIN_VALUE + 300)
public class Jaxb2HttpMessageConverter extends AbstractHttpMessageConverter<Object> {

    private final JAXBContext jaxbContext;

    public Jaxb2HttpMessageConverter() {
        super(List.of(MediaType.APPLICATION_XML));
        this.jaxbContext = null;
    }

    @Override
    protected boolean supportsType(Class<?> clazz) {
        return clazz.isAnnotationPresent(XmlRootElement.class) || clazz.isAnnotationPresent(XmlType.class);
    }

    @Override
    public void doWrite(Object data, Class<?> type, HttpOutputMessage outputMessage) throws IOException {
        try {
            JAXBContext context    = getJaxbContext(type);
            Marshaller  marshaller = context.createMarshaller();

            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            outputMessage.getHeaders().setContentType(MediaType.APPLICATION_XML);

            marshaller.marshal(data, outputMessage.getOutputStream());
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object doRead(Class<?> clazz, HttpInputMessage inputMessage) throws IOException {
        try {
            JAXBContext  context      = getJaxbContext(clazz);
            Unmarshaller unmarshaller = context.createUnmarshaller();

            return unmarshaller.unmarshal(inputMessage.getInputStream());
        } catch (JAXBException e) {
            throw new IOException("Could not unmarshal JAXB object", e);
        }
    }

    private JAXBContext getJaxbContext(Class<?> clazz) throws JAXBException {
        if (jaxbContext != null) {
            return jaxbContext;
        } else {
            return JAXBContext.newInstance(clazz);
        }
    }
}