package org.jmouse.web.mvc.resource;

import org.jmouse.core.io.Resource;

import java.util.List;

/**
 * 🌐 Context object passed to a {@link ResourceComposer}.
 *
 * <p>Holds request-specific data used during URL composition:</p>
 * <ul>
 *   <li>📌 {@code requestPath} — the original request path</li>
 *   <li>📦 {@code resource} — the resolved {@link Resource}</li>
 *   <li>📂 {@code locations} — candidate resource locations</li>
 * </ul>
 *
 * <p>💡 This record is immutable and intended for simple transport of
 * values between resource resolution and composition stages.</p>
 *
 * @param requestPath original request path
 * @param resource    the resolved resource
 * @param locations   candidate base locations for resolution
 */
public record UrlComposerContext(String requestPath, Resource resource, List<? extends Resource> locations) { }
