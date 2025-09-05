package org.jmouse.web.mvc.method.converter;

import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.core.MediaType;
import org.jmouse.core.Priority;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 📦 {@link AbstractHttpMessageConverter} for raw {@code byte[]} payloads.
 *
 * <p>Supports reading and writing binary streams with content type
 * {@code application/octet-stream} or any type).</p>
 */
@Priority(Integer.MIN_VALUE)
public class ByteArrayHttpMessageConverter extends AbstractHttpMessageConverter<byte[]> {

    /**
     * 🛠️ Register supported media types: {@code application/octet-stream} .
     */
    protected ByteArrayHttpMessageConverter() {
        super(List.of(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));
    }

    /**
     * ✍️ Write byte array to the HTTP response body.
     *
     * <ul>
     *   <li>Sets {@code Content-Length} header.</li>
     *   <li>Writes raw bytes directly to output stream.</li>
     *   <li>Flushes response buffer if not committed.</li>
     * </ul>
     *
     * @param data          the byte array to write
     * @param type          the declared object type
     * @param outputMessage target HTTP output
     * @throws IOException if writing fails
     */
    @Override
    protected void doWrite(byte[] data, Class<?> type, HttpOutputMessage outputMessage) throws IOException {
        outputMessage.getHeaders().setContentLength(data.length);
        outputMessage.getOutputStream().write(data);

        if (outputMessage instanceof HttpServletResponse response && !response.isCommitted()) {
            response.flushBuffer();
        }
    }

    /**
     * 📥 Read byte array from the HTTP request body.
     *
     * <ul>
     *   <li>Reads up to declared {@code Content-Length} if available.</li>
     *   <li>Falls back to reading entire stream if length unknown.</li>
     * </ul>
     *
     * @param clazz        target type ({@code byte[].class})
     * @param inputMessage source HTTP input
     * @return decoded byte array
     * @throws IOException if reading fails
     */
    @Override
    protected byte[] doRead(Class<? extends byte[]> clazz, HttpInputMessage inputMessage) throws IOException {
        long        length      = inputMessage.getHeaders().getContentLength();
        InputStream inputStream = inputMessage.getInputStream();

        if (length >= 0 && length < Integer.MAX_VALUE) {
            return inputStream.readNBytes((int) length);
        }

        return inputStream.readAllBytes();
    }

    /**
     * 🔍 Check if the converter supports the given type.
     *
     * @param clazz target class
     * @return {@code true} if {@code byte[].class}, else {@code false}
     */
    @Override
    protected boolean supportsType(Class<?> clazz) {
        return byte[].class == clazz;
    }

}