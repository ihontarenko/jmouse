package org.jmouse.storage.spring;

import org.jmouse.core.io.ResourceSegment;
import org.jmouse.http.Headers;
import org.jmouse.http.HttpHeader;
import org.jmouse.http.Range;
import org.jmouse.storage.FileStore;
import org.jmouse.storage.FileStores;
import org.jmouse.storage.delivery.DeliverableFile;
import org.jmouse.storage.delivery.DeliveryPlan;
import org.jmouse.storage.exception.StorageException;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.Map;

/**
 * 🖨️ Turns a plan into a response.
 *
 * <p>The one place bytes are actually fetched. The planner decided everything — status, headers,
 * which range — without touching storage, so this class has no decisions left to make and no
 * arithmetic to get wrong. That split is what makes the subtle half testable without a server and
 * the mechanical half small enough to read in one sitting.</p>
 *
 * <p><strong>No controller and no route mapping ships here.</strong> Routes, DTOs and
 * authorization stay with each product, because those are the parts that legitimately differ. A
 * product's controller declares its own routes and calls {@link #render} once per route.</p>
 */
public class DeliveryRenderer {

    private final FileStores fileStores;

    /**
     * 🏗️ Render plans by reading through the backend each file names.
     *
     * @param fileStores every backend the application has
     */
    public DeliveryRenderer(FileStores fileStores) {
        this.fileStores = fileStores;
    }

    /**
     * 🖨️ Render a plan.
     *
     * <p>Exhaustive over a sealed set, so a new delivery outcome breaks this method at compile time
     * rather than falling through at runtime.</p>
     *
     * @param plan what the planner decided
     * @return the response
     */
    public ResponseEntity<Resource> render(DeliveryPlan plan) {
        return switch (plan) {
            case DeliveryPlan.Redirected redirected -> respond(redirected).build();
            case DeliveryPlan.NotModified notModified -> respond(notModified).build();
            case DeliveryPlan.RangeNotSatisfiable notSatisfiable -> respond(notSatisfiable).build();
            case DeliveryPlan.Streamed streamed -> respond(streamed).body(readWhole(streamed.file()));
            case DeliveryPlan.PartiallyStreamed partial -> respond(partial).body(readRange(partial));
        };
    }

    /**
     * 📨 Start a response carrying the plan's status and every header it resolved.
     *
     * @param plan the plan being rendered
     * @return the part-built response
     */
    private ResponseEntity.BodyBuilder respond(DeliveryPlan plan) {
        Headers                    headers = plan.headers();
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(headers.getStatus().getCode());

        for (Map.Entry<HttpHeader, Object> header : headers.asMap().entrySet()) {
            if (header.getValue() != null) {
                builder.header(header.getKey().value(), header.getValue().toString());
            }
        }

        return builder;
    }

    /**
     * 📖 The whole object, from the backend that holds it.
     *
     * @param file the file being delivered
     * @return the bytes as a Spring resource
     */
    private Resource readWhole(DeliverableFile file) {
        FileStore fileStore = fileStores.require(file.backendName());
        return SpringResources.from(fileStore.read(file.storageKey()));
    }

    /**
     * ✂️ Exactly the planned bytes, from the backend that holds them.
     *
     * <p>The range is handed back to the backend rather than resolved again here, so a backend that
     * can fetch only the requested bytes — an object store — does, instead of pulling the whole
     * object and discarding most of it.</p>
     *
     * @param partial the planned partial response
     * @return the segment as a Spring resource
     */
    private Resource readRange(DeliveryPlan.PartiallyStreamed partial) {
        DeliverableFile file      = partial.file();
        FileStore       fileStore = fileStores.require(file.backendName());
        ResourceSegment segment   = fileStore.readRange(file.storageKey(),
                                                        Range.ofRange(partial.start(), partial.end()));

        try {
            return SpringResources.from(segment);
        } catch (IOException exception) {
            throw new StorageException("Failed to serve bytes %d-%d of '%s': %s"
                                               .formatted(partial.start(), partial.end(),
                                                          file.storageKey(), exception.getMessage()),
                                       exception);
        }
    }
}
