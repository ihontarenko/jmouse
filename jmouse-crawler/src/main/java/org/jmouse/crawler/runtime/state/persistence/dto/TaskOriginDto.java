package org.jmouse.crawler.runtime.state.persistence.dto;

import org.jmouse.core.bind.BindDefault;

/**
 * Concrete DTO for TaskOrigin.
 *
 * <p>"kind" is a discriminator you can use later to rebuild your domain origin type.</p>
 */
public record TaskOriginDto(
//        String kind,
        @BindDefault("UNDEFINED") String publisher,
        String routeId,
        String parentTaskId,
        String reason,
        String source
) {}