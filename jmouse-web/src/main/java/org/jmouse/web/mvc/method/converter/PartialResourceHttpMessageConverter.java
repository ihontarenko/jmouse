package org.jmouse.web.mvc.method.converter;

import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.core.MediaType;
import org.jmouse.core.MediaTypeHelper;
import org.jmouse.core.StreamHelper;
import org.jmouse.core.io.Resource;
import org.jmouse.core.io.ResourceSegment;
import org.jmouse.web.http.HttpHeader;
import org.jmouse.web.http.HttpStatus;
import org.jmouse.web.http.request.Headers;
import org.jmouse.web.http.response.HeadersBuffer;

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
        if (message instanceof ServletHttpOutputMessage response) {
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

        if (!validatePositions(start, end, length, message)) {
            return;
        }

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

        if (!validateSegments(segments, message)) {
            return;
        }

        try (OutputStream output = message.getOutputStream()) {
            for (ResourceSegment segment : segments) {
                long start       = segment.getPosition();
                long rangeLength = segment.getTotal();
                long end         = start + rangeLength - 1;
                long length      = segment.getResource().getSize();

                String    filename    = segment.getResource().getFilename();
                MediaType contentType = getMediaType(filename);

                end = Math.min(end, length - 1);

                write(output, null, true); // \r\n
                write(output, "--" + boundary, false);
                write(output, null, true); // \r\n

                write(output, "Content-Type: %s".formatted(contentType.toString()), false);
                write(output, null, true);
                write(output, "Content-Range: bytes %d-%d/%d".formatted(start, end, length), false);
                write(output, null, true); // \r\n
                write(output, null, true); // \r\n

                try (InputStream input = segment.getResource().getInputStream()) {
                    StreamHelper.copy(input, output, start, end);
                }

                write(output, null, true); // \r\n
            }

            write(output, "--" + boundary + "--", false);
        }
    }

    /**
     * ✅ Validate a list of {@link ResourceSegment} instances against their underlying resource lengths.
     *
     * <p>This method iterates over all segments and ensures that each segment's
     * <code>start</code> and <code>end</code> byte positions fall within the valid
     * range of the resource.</p>
     *
     * <p>If any segment is invalid:</p>
     * <ul>
     *   <li>Delegates to {@link #validatePositions(long, long, long, HttpOutputMessage)} to
     *       prepare a <code>416 Range Not Satisfiable</code> response.</li>
     *   <li>Stops further validation (early exit).</li>
     * </ul>
     *
     * @param segments list of resource segments to validate
     * @param message  HTTP output message adapter (used to set headers/status on error)
     * @return {@code true} if all segments are valid; {@code false} if at least one segment is invalid
     * @throws IOException if committing the error response fails
     */
    private boolean validateSegments(List<ResourceSegment> segments, HttpOutputMessage message) throws IOException {
        boolean valid = true;

        for (ResourceSegment segment : segments) {
            long start       = segment.getPosition();
            long rangeLength = segment.getTotal();
            long end         = start + rangeLength - 1;
            long length      = segment.getResource().getSize();

            if (!validatePositions(start, end, length, message)) {
                valid = false;
                break;
            }
        }

        return valid;
    }

    /**
     * ✅ Validate computed byte positions against the resource length and, if invalid,
     * prepares a proper 416 (Range Not Satisfiable) response.
     *
     * <p>On invalid range this method:</p>
     * <ul>
     *   <li>Sets <code>Content-Range: bytes &#42;/{length}</code></li>
     *   <li>Sets <code>Content-Length: 0</code></li>
     *   <li>Writes buffered headers into the servlet response</li>
     *   <li>Commits the response via <code>flushBuffer()</code> without opening the body stream</li>
     * </ul>
     *
     * <p>Note: The last valid byte index is <code>length - 1</code>. This method treats
     * <code>end >= length</code> as invalid.</p>
     *
     * @param start   inclusive start byte index (0-based) or -1 for suffix
     * @param end     inclusive end byte index, or -1 for open-ended
     * @param length  total resource length in bytes
     * @param message HTTP output message adapter
     * @return {@code true} if positions are valid and writing may proceed; {@code false} otherwise
     * @throws IOException if committing the response fails
     */
    private boolean validatePositions(long start, long end, long length, HttpOutputMessage message) throws IOException {
        boolean valid = true;

        // Invalid when: negative start, start beyond EOF, inverted range, or end beyond last valid index
        if (start < 0 || end < start || end >= length) {
            Headers headers = message.getHeaders();

            headers.setHeader(HttpHeader.CONTENT_RANGE, "bytes */%d".formatted(length));
            headers.setContentLength(0);
            headers.setHeader(HttpHeader.X_JMOUSE_DEBUG, "POSITIONS CORRUPTED; START: %d; END: %d; LENGTH: %d;"
                    .formatted(start, end, length));

            if (message instanceof ServletHttpOutputMessage responseMessage) {
                HttpServletResponse response = responseMessage.getResponse();
                HeadersBuffer       buffer   = responseMessage.getHeadersBuffer();

                response.setStatus(HttpStatus.RANGE_NOT_SATISFIABLE.getCode());

                if ((!responseMessage.isWritten() && !response.isCommitted()) || buffer.cleanup(response)) {
                    responseMessage.writeHeaders();
                    response.flushBuffer();
                }
            }

            valid = false;
        }

        return valid;
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
