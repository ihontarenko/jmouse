package org.jmouse.web.mvc.resource;

/**
 * 🏷️ Represents a versioned resource path.
 *
 * @param simple  path without version prefix
 * @param version version string
 */
public record PathVersion(String simple, String version){ }
