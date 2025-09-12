package org.jmouse.web.mvc.method.converter;

import org.jmouse.core.MediaType;
import org.jmouse.web.http.request.Headers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 📝 {@link AbstractHttpMessageConverter} for {@link String} payloads.
 *
 * <p>Supports plain text and all media types, with charset detection
 * from {@link MediaType} or defaulting to UTF-8.</p>
 */
public class StringHttpMessageConverter extends AbstractHttpMessageConverter<String> {

    /**
     * 🛠️ Register supported media types: {@code text/plain} and {@literal *&#47;*}.
     */
    protected StringHttpMessageConverter() {
        super(MediaType.TEXT_PLAIN, MediaType.ALL);
    }

    /**
     * ✍️ Write string content to the HTTP response body.
     *
     * <ul>
     *   <li>Encodes string to bytes using resolved charset.</li>
     *   <li>Writes directly to output stream.</li>
     * </ul>
     *
     * @param data          string to write
     * @param type          declared type
     * @param outputMessage target HTTP output
     * @throws IOException if writing fails
     */
    @Override
    protected void doWrite(String data, Class<?> type, HttpOutputMessage outputMessage) throws IOException {
        outputMessage.getOutputStream().write(data.getBytes(getCharset(outputMessage.getHeaders().getContentType())));
    }

    /**
     * 📥 Read string content from the HTTP request body.
     *
     * <ul>
     *   <li>Reads fixed length if {@code Content-Length} is provided.</li>
     *   <li>Otherwise consumes full input stream.</li>
     *   <li>Decodes bytes into string using resolved charset.</li>
     * </ul>
     *
     * @param clazz   target type (always {@code String.class})
     * @param message source HTTP input
     * @return decoded string
     * @throws IOException if reading fails
     */
    @Override
    protected String doRead(Class<? extends String> clazz, HttpInputMessage message) throws IOException {
        Headers     headers       = message.getHeaders();
        long        contentLength = headers.getContentLength();
        InputStream stream        = message.getInputStream();
        byte[]      bytes         = contentLength > 0 ? stream.readNBytes((int) contentLength) : stream.readAllBytes();
        return new String(bytes, getCharset(headers.getContentType()));
    }

    /**
     * 🔍 Check if the converter supports the given type.
     *
     * @param clazz target class
     * @return {@code true} if {@code String.class}, else {@code false}
     */
    @Override
    protected boolean supportsType(Class<?> clazz) {
        return String.class == clazz;
    }

    /**
     * 🎯 Resolve charset from media type or fallback to UTF-8.
     *
     * @param mediaType content type (may be {@code null})
     * @return resolved {@link Charset}, defaults to UTF-8
     */
    private Charset getCharset(MediaType mediaType) {
        Charset charset = StandardCharsets.UTF_8;

        if (mediaType != null && mediaType.getCharset() != null) {
            charset = mediaType.getCharset();
        }

        return charset;
    }

}
