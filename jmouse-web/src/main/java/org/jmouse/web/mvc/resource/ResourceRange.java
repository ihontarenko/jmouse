package org.jmouse.web.mvc.resource;

import org.jmouse.core.io.Resource;
import org.jmouse.core.io.ResourceSegment;
import org.jmouse.web.http.request.Range;

import java.util.ArrayList;
import java.util.List;

/**
 * 📦 Utility for mapping HTTP {@link Range} requests
 * to {@link ResourceSegment}s of a {@link Resource}.
 *
 * <p>Supports single and multiple ranges as defined by
 * RFC 7233 (HTTP Range Requests).</p>
 */
public class ResourceRange {

    /**
     * 🎯 Convert a single {@link Range} into a {@link ResourceSegment}.
     *
     * @param range    requested byte range
     * @param resource target resource
     * @return computed {@link ResourceSegment}
     */
    public static ResourceSegment toSegment(Range range, Resource resource) {
        long length   = getContentLength(resource);
        long position = range.getStart(length);
        long end      = range.getEnd(length);
        long total    = end - position + 1;
        return new ResourceSegment(position, resource, total);
    }

    /**
     * 🎯 Convert multiple {@link Range}s into {@link ResourceSegment}s.
     *
     * @param ranges   list of byte ranges
     * @param resource target resource
     * @return list of computed segments
     */
    public static List<ResourceSegment> toSegments(List<Range> ranges, Resource resource) {
        List<ResourceSegment> segments = new ArrayList<>(ranges.size());

        for (Range range : ranges) {
            segments.add(toSegment(range, resource));
        }

        return segments;
    }

    /**
     * 📏 Get the total size of a resource.
     *
     * @param resource the resource
     * @return content length in bytes
     */
    public static long getContentLength(Resource resource) {
        return resource.getSize();
    }
}
