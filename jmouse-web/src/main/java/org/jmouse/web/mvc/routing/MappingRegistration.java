package org.jmouse.web.mvc.routing;

/**
 * 📌 Represents a single route registration containing its {@link MappingCriteria} and handler.
 *
 * @param criteria the criteria used to match incoming requests
 * @param handler  the handler object associated with the mapping
 *
 * @param <T> type of the handler (e.g. {@code RouteMappedHandler})
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record MappingRegistration<T>(MappingCriteria criteria, T handler) { }
