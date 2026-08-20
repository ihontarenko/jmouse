package org.jmouse.avatar;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.storage.delivery.DeliveryIntent;
import org.jmouse.storage.spring.DeliveryIntents;
import org.jmouse.storage.spring.DeliveryRenderer;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 🖼️ The bytes of an uploaded avatar, served to anybody who has the address.
 *
 * <p>⚠️ <strong>THE ONE ROUTE IN THIS WHOLE EXTRACTION THAT IS UNAUTHENTICATED, AND IT IS
 * UNAUTHENTICATED BECAUSE AN {@code <img>} TAG CANNOT SIGN IN.</strong> A browser fetching an image
 * sends no Authorization header and there is no way to give it one — every alternative means fetching
 * each avatar as a blob in JavaScript and managing an object URL's lifetime at each of the fifteen
 * places a person is rendered. That is a large amount of machinery in exchange for guarding a
 * thumbnail.</p>
 *
 * <p>What actually protects it is that the address is a <strong>capability rather than a name</strong>.
 * It carries a random registry identifier — not a member id, not a subject, not an email: it cannot be
 * constructed from knowing who somebody is, cannot be walked to find the next person, and is only ever
 * learned from an authenticated response that already showed you that member. Nothing here discloses
 * whose face it is.</p>
 *
 * <p>⚠️ <strong>And the second half of that guarantee is the type rule in {@link AvatarService}.</strong>
 * This route serves whatever was stored under the type it was stored as, so if a script host could be
 * uploaded, this is where it would be executed from. Widening that allowlist and leaving this route
 * public is the mistake to never make.</p>
 *
 * <p>⚠️ This is also the reason this controller is the one thing here that genuinely belongs to the
 * library rather than to a product: it is addressed by a registry identifier and carries no product
 * semantics whatsoever. Two products had it as the same sixty-one lines, character for character.</p>
 */
@RestController
public class PublicAvatarController {

    /** Where an avatar's bytes are served from. Stated once, read by every interface. */
    public static final String ROUTE = "/api/public/avatars/{storedFileId}";

    private final AvatarService    avatars;
    private final DeliveryRenderer renderer;

    /**
     * 🏗️ Serve faces.
     *
     * @param avatars  what resolves one
     * @param renderer turns a delivery plan into a response
     */
    public PublicAvatarController(AvatarService avatars, DeliveryRenderer renderer) {
        this.avatars  = avatars;
        this.renderer = renderer;
    }

    /**
     * 🖼️ The bytes.
     *
     * <p>⚠️ {@code PUBLIC} rather than {@code OWNER}: it decides which of the two cache lifetimes the
     * planner applies, and a content-addressed avatar can never change under a cached copy — replacing
     * your picture produces a different address.</p>
     *
     * @param storedFileId the registry identifier the payload handed out
     * @param request      the request, for conditional and range headers
     * @return the bytes, or a redirect to them
     */
    @GetMapping(ROUTE)
    public ResponseEntity<Resource> serve(@PathVariable String storedFileId,
                                          HttpServletRequest request) {
        DeliveryIntent intent = DeliveryIntents.of(request, false, DeliveryIntent.Audience.PUBLIC);

        return renderer.render(avatars.planDelivery(storedFileId, intent));
    }
}
