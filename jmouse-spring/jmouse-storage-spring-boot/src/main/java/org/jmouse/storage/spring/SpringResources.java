package org.jmouse.storage.spring;

import org.jmouse.core.io.Resource;
import org.jmouse.core.io.ResourceSegment;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.InputStreamResource;

import java.io.IOException;
import java.io.InputStream;

/**
 * 🌉 Carries a jMouse resource across to Spring, so a controller can still return something Spring
 * knows how to write.
 *
 * <p>One direction only, and deliberately: the library reads through its own abstraction, and
 * nothing in it ever needs to consume a Spring resource. A bridge built both ways would be half
 * unused and would invite the library to start accepting Spring types.</p>
 */
public final class SpringResources {

    private SpringResources() {
    }

    /**
     * 🌉 A whole resource, as Spring's.
     *
     * @param resource the jMouse resource
     * @return the Spring resource
     */
    public static org.springframework.core.io.Resource from(Resource resource) {
        return new BridgedResource(resource);
    }

    /**
     * ✂️ A byte range of a resource, as Spring's, positioned at the range's first byte.
     *
     * <p>The segment reports how many bytes precede its start, and reaching that start means
     * <em>skipping</em> them — which is right for a file on disk and wrong for a network stream
     * that already begins at the range. A backend that pre-positions its stream reports zero to
     * skip, so this one method serves both without asking which kind it has.</p>
     *
     * @param segment the segment to serve
     * @return a Spring resource over exactly those bytes
     * @throws IOException when the underlying resource cannot be opened or positioned
     */
    public static org.springframework.core.io.Resource from(ResourceSegment segment) throws IOException {
        InputStream stream = segment.getResource().getInputStream();

        long skipped = stream.skip(segment.getPosition());

        if (skipped < segment.getPosition()) {
            stream.close();
            throw new IOException("Could not position at byte %d of '%s'"
                                          .formatted(segment.getPosition(), segment.getResource().getName()));
        }

        return new InputStreamResource(stream);
    }

    /**
     * 🌉 A Spring resource reading through a jMouse one.
     *
     * <p>Extends {@link AbstractResource} rather than wrapping an {@code InputStream}, so length
     * and existence are answered from the underlying resource instead of by consuming the stream
     * to count it — which would hand the caller an empty one.</p>
     */
    private static final class BridgedResource extends AbstractResource {

        private final Resource resource;

        private BridgedResource(Resource resource) {
            this.resource = resource;
        }

        @Override
        public String getDescription() {
            return resource.getResourceName();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return resource.getInputStream();
        }

        @Override
        public String getFilename() {
            return resource.getFilename();
        }

        @Override
        public boolean exists() {
            return resource.exists();
        }

        @Override
        public long contentLength() {
            return resource.getLength();
        }

        @Override
        public long lastModified() {
            return resource.getLastModified();
        }
    }
}
