package org.jmouse.web.negotiation;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.MediaType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 🗺️ Base lookup that maps a request-derived key (e.g., path extension or query parameter)
 * to a {@link MediaType}.
 *
 * <p>Subclasses implement {@link #getMappingKey(HttpServletRequest)} to extract a key
 * from the request. If the key exists in the internal mapping, the corresponding
 * media type is returned as a single-element list; otherwise an empty list is returned.</p>
 *
 * <h4>Usage</h4>
 * <pre>{@code
 * MappedMediaTypeLookup lookup = new QueryParameterLookup("format");
 * lookup.addExtension("json", MediaType.APPLICATION_JSON);
 * // ... later in request processing
 * List<MediaType> types = lookup.lookup(request); // -> [application/json]
 * }</pre>
 *
 * <h4>Contract</h4>
 * <ul>
 *   <li>Never returns {@code null} from {@link #lookup(HttpServletRequest)}.</li>
 *   <li>Intended to be populated during initialization; concurrent mutation is not required.</li>
 *   <li>Key format (case, dot prefix, etc.) is defined by the subclass and should be
 *       consistent with calls to {@link #addExtension(String, MediaType)}.</li>
 * </ul>
 *
 * <p><b>Thread-safety:</b> this class is stateless except for its mapping. Populate
 * the mapping during application startup and avoid runtime mutations to keep it effectively
 * immutable and thread-safe.</p>
 *
 * @author Ivan Hontarenko
 * @see MediaTypeLookup
 */
public abstract class MappedMediaTypeLookup implements MediaTypeLookup {

    /** 🏷️ Conventional key name used by some lookups (e.g., {@code {format}} or {@code ?format=}). */
    public static final String DEFAULT_FORMAT_ATTRIBUTE = "format";

    /** 🗂️ Internal key → media type mapping. Populate during init. */
    private final Map<String, MediaType> mapping = new HashMap<>();

    /** 🔑 Optional logical key name used by concrete implementations when extracting the mapping key. */
    private String keyName;

    public MappedMediaTypeLookup(String keyName) {
        this.keyName = keyName;
    }

    /**
     * 🔍 Resolves a single media type by deriving a key from the request and
     * looking it up in the internal mapping.
     *
     * @param request current HTTP request
     * @return a single-element list with the resolved media type, or an empty list if not found
     */
    @Override
    public List<MediaType> lookup(HttpServletRequest request) {
        String key = getMappingKey(request);

        if (key == null) {
            return List.of();
        }

        MediaType mediaType = mapping.get(key);

        return (mediaType != null) ? List.of(mediaType) : List.of();
    }

    /**
     * ➕ Registers a mapping from key/extension to {@link MediaType}.
     *
     * <p>Keys should follow the same normalization rules as applied by
     * {@link #getMappingKey(HttpServletRequest)} (e.g., case, presence of a leading '.').</p>
     *
     * @param extension the mapping key (e.g., {@code "json"})
     * @param mediaType the media type to associate
     */
    public void addExtension(String extension, MediaType mediaType) {
        mapping.put(extension, mediaType);
    }

    /**
     * 📎 Returns the media type mapped to the given key/extension, or {@code null} if absent.
     *
     * @param extension the mapping key
     * @return the mapped media type or {@code null}
     */
    public MediaType getExtension(String extension) {
        return mapping.get(extension);
    }

    /**
     * ✅ Checks whether a mapping exists for the given key/extension.
     *
     * @param extension the mapping key
     * @return {@code true} if a mapping exists; {@code false} otherwise
     */
    public boolean hasExtension(String extension) {
        return mapping.containsKey(extension);
    }

    /**
     * @return the logical key name used by this lookup (may be {@code null})
     */
    public String getKeyName() {
        return keyName;
    }

    /**
     * Sets the logical key name used by this lookup.
     *
     * <p>Call during initialization; avoid modifying at runtime to maintain effective immutability.</p>
     *
     * @param keyName the key name to use (e.g., {@code "format"})
     */
    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    /**
     * 🧩 Extracts the lookup key from the request.
     *
     * <p>Examples: file/path extension, {@code format} query parameter, request attribute, etc.
     * Implementations should normalize the key consistently with how mappings are registered
     * via {@link #addExtension(String, MediaType)}.</p>
     *
     * @param request current HTTP request
     * @return the derived key (may be {@code null} if not applicable)
     */
    protected abstract String getMappingKey(HttpServletRequest request);
}
