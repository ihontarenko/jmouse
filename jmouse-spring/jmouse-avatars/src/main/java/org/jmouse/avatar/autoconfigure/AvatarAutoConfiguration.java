package org.jmouse.avatar.autoconfigure;

import jakarta.persistence.EntityManager;
import org.jmouse.avatar.AvatarService;
import org.jmouse.avatar.PublicAvatarController;
import org.jmouse.storage.jpa.StoredFileDelivery;
import org.jmouse.storage.jpa.StoredFileIngestion;
import org.jmouse.storage.jpa.StoredFileRegistry;
import org.jmouse.storage.spring.DeliveryRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 🙂 Faces: the rule about which of three kinds is worn, and the one route that serves the bytes.
 *
 * <h3>⚠️ The public route is opt-in, and this switch is a security decision</h3>
 *
 * <p>{@code /api/public/avatars/**} is unauthenticated by design — an {@code <img>} tag cannot sign in
 * — and what makes it safe is a property of the <em>installation</em>: an upload allowlist with no
 * script hosts in it. A product whose policy is a denylist, or which has widened its allowlist, must
 * decide for itself whether that route is safe to publish. Making it appear the moment the dependency
 * lands would take that decision away from the only party able to make it.</p>
 *
 * <p>So it needs {@code jmouse.avatars.public-route.enabled: true}. The service is unconditional.</p>
 */
@AutoConfiguration
@ConditionalOnClass({EntityManager.class, StoredFileRegistry.class})
public class AvatarAutoConfiguration {

    /** Property a product sets to publish the unauthenticated byte route. */
    public static final String PUBLIC_ROUTE_ENABLED = "jmouse.avatars.public-route.enabled";

    /**
     * 🙂 The face rules.
     *
     * @param ingestion        the write path into storage
     * @param registry         where a stored avatar is looked up
     * @param delivery         the read path out of storage
     * @param maximumSizeBytes the largest picture a face may be, a megabyte unless a product says
     *                         otherwise
     * @return the service
     */
    @Bean
    @ConditionalOnMissingBean
    public AvatarService avatarService(StoredFileIngestion ingestion, StoredFileRegistry registry,
                                       StoredFileDelivery delivery,
                                       @Value("${jmouse.avatars.max-size-bytes:1048576}")
                                       long maximumSizeBytes) {
        return new AvatarService(ingestion, registry, delivery, maximumSizeBytes);
    }

    /**
     * 🖼️ The unauthenticated byte route — only where a product decided it is safe.
     *
     * @param avatars  what resolves one
     * @param renderer turns a delivery plan into a response
     * @return the controller
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = PUBLIC_ROUTE_ENABLED, havingValue = "true")
    public PublicAvatarController publicAvatarController(AvatarService avatars,
                                                         DeliveryRenderer renderer) {
        return new PublicAvatarController(avatars, renderer);
    }
}
