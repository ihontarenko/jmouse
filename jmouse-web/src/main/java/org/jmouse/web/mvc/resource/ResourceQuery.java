package org.jmouse.web.mvc.resource;

import org.jmouse.core.io.Resource;

import java.util.List;

/**
 * 🔍 Encapsulates a resource lookup request.
 *
 * <p>Contains:</p>
 * <ul>
 *   <li>📍 Requested path (URL or relative lookup key)</li>
 *   <li>📂 Candidate resource locations to search</li>
 * </ul>
 *
 * @param path      requested path
 * @param locations candidate resource locations
 */
public record ResourceQuery(String path, List<? extends Resource> locations) {}