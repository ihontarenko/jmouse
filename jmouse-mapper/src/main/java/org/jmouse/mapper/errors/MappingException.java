package org.jmouse.mapper.errors;

import org.jmouse.core.access.PropertyPath;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A mapping failure, carrying a stable {@link #code()}, free-form {@link #meta()} and the
 * {@link #path()} it happened at. 💥
 *
 * <p>The path is stamped where the failure is raised, not where it is caught. That matters because
 * the outermost handler only knows the root scope, so an exception without a recorded path is
 * reported as having happened at the root - which is never true.</p>
 *
 * <p>The type is immutable: {@link #withMeta(String, Object)} and {@link #withPath(PropertyPath)}
 * return copies.</p>
 */
public final class MappingException extends RuntimeException {

    private final String              code;
    private final Map<String, Object> meta;
    private final PropertyPath        path;

    public MappingException(String code, String message) {
        this(code, message, null, Map.of(), null);
    }

    public MappingException(String code, String message, Throwable cause) {
        this(code, message, cause, Map.of(), null);
    }

    public MappingException(String code, String message, Throwable cause, Map<String, Object> meta) {
        this(code, message, cause, meta, null);
    }

    public MappingException(
            String code,
            String message,
            Throwable cause,
            Map<String, Object> meta,
            PropertyPath path
    ) {
        super(message, cause);
        this.code = code;
        this.meta = Collections.unmodifiableMap(new LinkedHashMap<>(meta));
        this.path = path;
    }

    /**
     * Stable identifier of what went wrong, matched by {@link ErrorsPolicy}.
     *
     * @return error code
     */
    public String code() {
        return code;
    }

    /**
     * Extra detail attached to the failure.
     *
     * @return immutable metadata
     */
    public Map<String, Object> meta() {
        return meta;
    }

    /**
     * Where the failure happened within the mapping.
     *
     * @return property path, or {@code null} when the raising site recorded none
     */
    public PropertyPath path() {
        return path;
    }

    /**
     * Copy carrying one more metadata entry.
     *
     * @param key metadata key
     * @param value metadata value
     * @return new exception instance
     */
    public MappingException withMeta(String key, Object value) {
        LinkedHashMap<String, Object> copy = new LinkedHashMap<>(this.meta);
        copy.put(key, value);
        return copyWith(copy, this.path);
    }

    /**
     * Copy located at the given path.
     *
     * @param path where the failure happened
     * @return new exception instance
     */
    public MappingException withPath(PropertyPath path) {
        return copyWith(this.meta, path);
    }

    /**
     * Build a copy that still points at the original throw site.
     *
     * <p>⚠️ The stack trace is carried over deliberately. A copy made the ordinary way records where
     * the <em>copy</em> was constructed, so annotating an exception on its way out would quietly
     * replace the one piece of information that says where the mapping actually broke.</p>
     *
     * @param meta metadata for the copy
     * @param path location for the copy
     * @return new exception instance carrying this one's stack trace
     */
    private MappingException copyWith(Map<String, Object> meta, PropertyPath path) {
        MappingException copy = new MappingException(this.code, getMessage(), getCause(), meta, path);
        copy.setStackTrace(getStackTrace());
        return copy;
    }

    @Override
    public String toString() {
        String location = path == null ? "" : " at '" + path + "'";
        return "[" + code.toUpperCase() + "]" + location + ": " + super.toString();
    }

}
