package org.jmouse.web.negotiation;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.MediaType;
import org.jmouse.core.Priority;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 🛡️ Provides a fallback {@link MediaType} when no other
 * negotiation strategies are applicable.
 *
 * <p>Registered with the lowest priority so it acts
 * only as a last resort.</p>
 */
@Priority(Integer.MAX_VALUE)
public class FallbackMediaTypeLookup implements MediaTypeLookup {

    /** 🎯 The fallback media type to return if none is resolved. */
    private MediaType fallback;

    /**
     * 📥 Get the configured fallback media type.
     *
     * @return fallback type or {@code null} if not set
     */
    public MediaType getFallback() {
        return fallback;
    }

    /**
     * 📤 Set the fallback media type.
     *
     * @param fallback the type to use as last resort
     */
    public void setFallback(MediaType fallback) {
        this.fallback = fallback;
    }

    /**
     * 🔎 Lookup always returns the configured fallback
     * media type if present, otherwise an empty list.
     *
     * @param request the current HTTP request
     * @return unmodifiable list containing the fallback type, or empty
     */
    @Override
    public List<MediaType> lookup(HttpServletRequest request) {
        List<MediaType> mediaTypes = new ArrayList<>();

        if (fallback != null) {
            mediaTypes.add(fallback);
        }

        return Collections.unmodifiableList(mediaTypes);
    }

}
