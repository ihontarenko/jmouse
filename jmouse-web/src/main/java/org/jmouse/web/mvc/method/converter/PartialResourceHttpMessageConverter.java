package org.jmouse.web.mvc.method.converter;

import org.jmouse.core.MediaType;
import org.jmouse.core.MediaTypeHelper;
import org.jmouse.core.StreamHelper;
import org.jmouse.core.io.Resource;
import org.jmouse.core.io.ResourceSegment;
import org.jmouse.web.http.HttpHeader;
import org.jmouse.web.http.HttpStatus;
import org.jmouse.web.http.request.Headers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.jmouse.core.MediaTypeHelper.getMediaType;

/**
 * 🚚 Streams {@link Resource} content to a servlet response with HTTP Range support.
 *
 * <p>Features:</p>
 * <ul>
 *   <li><b>200</b> full-body with {@code Content-Length}</li>
 *   <li><b>206</b> single range with {@code Content-Range}</li>
 *   <li><b>206</b> multiple ranges as {@code multipart/byteranges}</li>
 *   <li>HEAD handling (no body)</li>
 *   <li>Portable {@link InputStream}-based copy (no zero-copy)</li>
 * </ul>
 *
 * <p>Input object can be a single {@link ResourceSegment} or a {@code List<ResourceSegment>}.</p>
 */
public class PartialResourceHttpMessageConverter extends AbstractHttpMessageConverter<Object> {

    /**
     * CRLF bytes for multipart boundaries.
     */
    public static final byte[] NEW_LINE_BYTES = "\r\n".getBytes(StandardCharsets.US_ASCII);

    /**
     * 🏗️ Create a converter with default content type {@code application/octet-stream}.
     */
    public PartialResourceHttpMessageConverter() {
        super(MediaType.APPLICATION_OCTET_STREAM);
    }

    /**
     * 🖊️ Write a buffer to the stream, adding CRLF if requested.
     */
    private static void write(OutputStream output, String buffer, boolean newLine) throws IOException {
        if (buffer != null) {
            output.write(buffer.getBytes(StandardCharsets.US_ASCII));
        }
        if (newLine) {
            output.write(NEW_LINE_BYTES);
        }
    }

    /**
     * ✍️ Write either a single {@link ResourceSegment} or a list of segments.
     *
     * <p>Sets HTTP status to {@code 206 Partial Content} when writing ranges.</p>
     *
     * @param object  payload: {@link ResourceSegment} or {@code List<ResourceSegment>}
     * @param type    declared type
     * @param message output message wrapper
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void doWrite(Object object, Class<?> type, HttpOutputMessage message) throws IOException {
        List<ResourceSegment> segments = new LinkedList<>();

        if (object instanceof ResourceSegment segment) {
            segments.add(segment);
        } else if (object instanceof List<?> collection && !collection.isEmpty()
                && collection.getFirst() instanceof ResourceSegment) {
            segments.addAll((List<ResourceSegment>) collection);
        }

        // If we are writing ranges, force 206 status
        if (message instanceof ServletResponseHttpOutputMessage response) {
            response.getResponse().setStatus(HttpStatus.PARTIAL_CONTENT.getCode());
        }

        if (!segments.isEmpty()) {
            if (segments.size() == 1) {
                writeSingleSegment(message, segments.getFirst());
            } else {
                writeMultiSegment(message, segments);
            }
        }
    }

    /**
     * 📦 Write a single range response.
     *
     * <ul>
     *   <li>Sets {@code Content-Type}</li>
     *   <li>Sets {@code Content-Length}</li>
     *   <li>Sets {@code Content-Range}</li>
     * </ul>
     */
    private void writeSingleSegment(HttpOutputMessage message, ResourceSegment segment) throws IOException {
        Resource resource    = segment.getResource();
        long     length      = resource.getSize();
        long     start       = segment.getPosition();
        long     rangeLength = segment.getTotal();
        long     end         = start + rangeLength - 1;

        MediaType contentType = getMediaType(resource.getFilename());
        Headers   headers     = message.getHeaders();

        writeHeaders(headers, contentType);

        headers.setContentLength(rangeLength);
        headers.setHeader(HttpHeader.CONTENT_RANGE, "bytes %d-%d/%d".formatted(start, end, length));

        try (InputStream input = resource.getInputStream()) {
            OutputStream output = message.getOutputStream();
            StreamHelper.copy(input, output, start, end);
        }
    }

    /**
     * 🧱 Write a multipart/byteranges response for multiple segments.
     *
     * <p>Each part includes its own {@code Content-Type} and {@code Content-Range} headers.</p>
     */
    private void writeMultiSegment(HttpOutputMessage message, List<ResourceSegment> segments) throws IOException {
        String    boundary  = MediaTypeHelper.generateMultipartBoundary(true);
        Headers   headers   = message.getHeaders();
        MediaType mediaType = getMultipartContentType(boundary);

        writeHeaders(headers, mediaType);

        try (OutputStream output = message.getOutputStream()) {
            for (ResourceSegment segment : segments) {
                long start       = segment.getPosition();
                long rangeLength = segment.getTotal();
                long end         = start + rangeLength - 1;
                long length      = segment.getResource().getSize();

                String    filename    = segment.getResource().getFilename();
                MediaType contentType = getMediaType(filename);

                // Validate indices
                if (start < 0 || start >= length || end < start) {
                    headers.setHeader(HttpHeader.CONTENT_RANGE, "bytes */%d".formatted(length));
                    if (message instanceof ServletResponseHttpOutputMessage response) {
                        response.getResponse().setStatus(HttpStatus.RANGE_NOT_SATISFIABLE.getCode());
                    }
                    return;
                }

                end = Math.min(end, length - 1);

                write(output, null, true);
                write(output, "--" + boundary, false);
                write(output, null, true);

                write(output, "Content-Type: %s".formatted(contentType.toString()), true);
                write(output, "Content-Range: bytes %d-%d/%d".formatted(start, end, length), true);
                write(output, null, true);

                try (InputStream input = segment.getResource().getInputStream()) {
                    StreamHelper.copy(input, output, start, end);
                }

                write(output, null, true);
            }

            write(output, "--" + boundary + "--", false);
        }
    }

    /**
     * 🧩 Build {@code multipart/byteranges} media type with boundary parameter.
     */
    private MediaType getMultipartContentType(String boundary) {
        return new MediaType("multipart", "byteranges", Map.of("boundary", boundary));
    }

    /**
     * 📨 Common headers for range responses.
     *
     * <ul>
     *   <li>{@code Accept-Ranges: bytes}</li>
     *   <li>{@code Content-Type: ...}</li>
     * </ul>
     */
    private void writeHeaders(Headers headers, MediaType mediaType) {
        headers.setHeader(HttpHeader.ACCEPT_RANGES, "bytes");
        headers.setContentType(mediaType);
    }

    /**
     * ❌ Reading resources is not supported by this converter.
     */
    @Override
    protected Resource doRead(Class<?> clazz, HttpInputMessage message) throws IOException {
        throw new UnsupportedOperationException("Big files not supported reading resources");
    }

    /**
     * ✅ Declare supported payload type.
     *
     * <p>Note: the converter accepts a single {@link ResourceSegment}. Lists are
     * handled in {@link #doWrite(Object, Class, HttpOutputMessage)} when the runtime
     * object is a {@code List<ResourceSegment>}.</p>
     */
    @Override
    protected boolean supportsType(Class<?> clazz) {
        return ResourceSegment.class.isAssignableFrom(clazz);
    }
}
