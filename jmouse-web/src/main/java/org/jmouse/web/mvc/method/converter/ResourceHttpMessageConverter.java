package org.jmouse.web.mvc.method.converter;

import org.jmouse.core.MediaType;
import org.jmouse.core.io.ByteArrayResource;
import org.jmouse.core.io.InputStreamResource;
import org.jmouse.core.io.Resource;
import org.jmouse.core.io.ResourceException;
import org.jmouse.web.http.request.Headers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 📦 {@link HttpMessageConverter} for {@link Resource} bodies.
 *
 * <p>Writes a {@link Resource} by streaming its {@link InputStream} to the HTTP response.
 * Reads either an {@link InputStreamResource} (single-use stream) or a {@link ByteArrayResource}
 * from the HTTP request body, inferring metadata (filename, length) from headers when available.</p>
 *
 * <p><b>Notes:</b>
 * <ul>
 *   <li>Default supported media type: {@code application/octet-stream}</li>
 *   <li>On write: I/O errors from the underlying stream are swallowed by design</li>
 *   <li>On read: only {@code InputStreamResource} and {@code ByteArrayResource} are supported</li>
 * </ul>
 * </p>
 */
public class ResourceHttpMessageConverter extends AbstractHttpMessageConverter<Resource> {

    /**
     * 🏗️ Create a converter for binary resources.
     *
     * <p>Currently initializes with default {@code application/octet-stream} support.</p>
     *
     * @param supportedMediaType optional media types (currently ignored)
     */
    public ResourceHttpMessageConverter(MediaType... supportedMediaType) {
        super(MediaType.APPLICATION_OCTET_STREAM);
    }

    /**
     * 📤 Write the given {@link Resource} to the HTTP response.
     *
     * <p>Streams the resource's input directly to the output stream and flushes it.</p>
     *
     * @param resource resource to write
     * @param type     declared type (ignored)
     * @param message  HTTP output message
     * @throws IOException if an I/O error occurs on response stream
     */
    @Override
    protected void doWrite(Resource resource, Class<?> type, HttpOutputMessage message) throws IOException {
        try {
            try (InputStream input = resource.getInputStream()) {
                OutputStream output = message.getOutputStream();
                input.transferTo(output);
                output.flush();
            } catch (Exception ignore) {

            }
        } catch (ResourceException ignore) {

        }
    }

    /**
     * 📥 Read a {@link Resource} from the HTTP request.
     *
     * <p>Supported targets:</p>
     * <ul>
     *   <li>{@link InputStreamResource} — returns a single-use resource backed by the request stream.
     *       Filename and content length are derived from {@link Headers#getContentDisposition()} and
     *       {@link Headers#getContentLength()} when present.</li>
     *   <li>{@link ByteArrayResource} — reads all bytes into memory; filename (if any) is taken from
     *       {@code Content-Disposition}.</li>
     * </ul>
     *
     * @param clazz   desired resource subtype
     * @param message HTTP input message
     * @return a {@link Resource} instance matching {@code clazz}
     * @throws IOException if an I/O error occurs while reading
     * @throws UnreadableException if the requested resource type is unsupported
     */
    @Override
    protected Resource doRead(Class<? extends Resource> clazz, HttpInputMessage message) throws IOException {
        Headers headers = message.getHeaders();

        if (InputStreamResource.class == clazz) {
            return new InputStreamResource(message.getInputStream()) {

                @Override
                public String getFilename() {
                    return headers.getContentDisposition().filename();
                }

                @Override
                public long getSize() {
                    return headers.getContentLength();
                }

            };
        } else if (ByteArrayResource.class == clazz) {
            return new ByteArrayResource(message.getInputStream().readAllBytes()) {
                @Override
                public String getFilename() {
                    return headers.getContentDisposition().filename();
                }
            };
        } else {
            throw new UnreadableException("Unsupported resource: %s".formatted(clazz));
        }
    }

    /**
     * ✅ Supports any subtype of {@link Resource}.
     *
     * @param clazz candidate type
     * @return {@code true} if {@code clazz} is a {@link Resource}
     */
    @Override
    protected boolean supportsType(Class<?> clazz) {
        return Resource.class.isAssignableFrom(clazz);
    }
}
