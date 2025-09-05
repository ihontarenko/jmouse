package org.jmouse.web.mvc.method.converter;

import jakarta.xml.bind.*;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import org.jmouse.core.MediaType;
import org.jmouse.core.Priority;

import java.io.IOException;
import java.util.List;

/**
 * 📦 {@link AbstractHttpMessageConverter} for JAXB-annotated objects.
 *
 * <p>Supports reading/writing XML payloads via {@link Marshaller}
 * and {@link Unmarshaller}.</p>
 */
@Priority(Integer.MIN_VALUE + 300)
public class Jaxb2HttpMessageConverter extends AbstractHttpMessageConverter<Object> {

    /** 🔧 Optional pre-configured JAXB context (may be null). */
    private final JAXBContext jaxbContext;

    /**
     * 🛠️ Register supported media type: {@code application/xml}.
     */
    public Jaxb2HttpMessageConverter() {
        super(List.of(MediaType.APPLICATION_XML));
        this.jaxbContext = null;
    }

    /**
     * 🔍 Check if target type is JAXB-annotated.
     *
     * <ul>
     *   <li>Supported if annotated with {@link XmlRootElement} or {@link XmlType}.</li>
     * </ul>
     *
     * @param clazz target class
     * @return {@code true} if JAXB-compatible
     */
    @Override
    protected boolean supportsType(Class<?> clazz) {
        return clazz.isAnnotationPresent(XmlRootElement.class) || clazz.isAnnotationPresent(XmlType.class);
    }

    /**
     * ✍️ Marshal object into XML and write to output.
     *
     * <ul>
     *   <li>Creates {@link Marshaller} from context.</li>
     *   <li>Enables formatted output.</li>
     *   <li>Sets content type to {@code application/xml}.</li>
     * </ul>
     *
     * @param data          object to marshal
     * @param type          declared type
     * @param outputMessage target HTTP output
     * @throws IOException if marshalling fails
     */
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

    /**
     * 📥 Unmarshal XML into a Java object.
     *
     * <ul>
     *   <li>Creates {@link Unmarshaller} from context.</li>
     *   <li>Reads object from input stream.</li>
     * </ul>
     *
     * @param clazz        target type
     * @param inputMessage source HTTP input
     * @return unmarshalled object
     * @throws IOException if unmarshalling fails
     */
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

    /**
     * ⚙️ Resolve {@link JAXBContext}.
     *
     * <ul>
     *   <li>Use pre-configured context if available.</li>
     *   <li>Otherwise create a new context for the target class.</li>
     * </ul>
     *
     * @param clazz target type
     * @return JAXB context
     * @throws JAXBException if context creation fails
     */
    private JAXBContext getJaxbContext(Class<?> clazz) throws JAXBException {
        if (jaxbContext != null) {
            return jaxbContext;
        } else {
            return JAXBContext.newInstance(clazz);
        }
    }
}
