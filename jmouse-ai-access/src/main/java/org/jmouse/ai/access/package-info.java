/**
 * The bridge from the tool dispatcher's authorization seam to the jMouse access engine.
 *
 * <p>Two translations and no third responsibility: {@code ToolAuthorizer} answered by
 * {@code AccessEngine}, and a tool invocation's scope translated to and from the engine's own scope
 * reference.
 *
 * <p>Its own artifact so that {@code jmouse-ai} can stay free of an authorization engine. A product
 * taking both gets something better than either alone — its tools and its HTTP endpoints are
 * authorized by the same engine against the same policy, so the two cannot drift apart the way a
 * parallel check always eventually does.
 *
 * <p>The permission a tool declares stays an opaque string everywhere in {@code jmouse-ai}: the
 * catalogue insists only that one is present and, where a vocabulary is supplied, that it exists. What
 * it <em>means</em> is decided here, which is the whole reason the two modules can be versioned apart.
 */
package org.jmouse.ai.access;
