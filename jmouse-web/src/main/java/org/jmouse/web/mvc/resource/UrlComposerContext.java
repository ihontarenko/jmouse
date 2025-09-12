package org.jmouse.web.mvc.resource;

import org.jmouse.core.io.Resource;

import java.util.List;

public record UrlComposerContext(Resource resource, List<? extends Resource> locations) {
}
