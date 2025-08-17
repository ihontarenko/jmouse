package org.jmouse.web.negotiation;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.web.mvc.HandlerMapping;
import org.jmouse.web.mvc.RouteMatch;

import static org.jmouse.web.http.request.RequestAttributesHolder.getAttribute;

/**
 * 🧭 Path-variable based media type resolver.
 *
 * <p>Concrete {@link MappedMediaTypeLookup} that derives the lookup key from a
 * route/path variable obtained via {@link RouteMatch}, stored under
 * {@link HandlerMapping#ROUTE_MATCH_ATTRIBUTE}.</p>
 *
 * <p>The variable name is configurable via {@link #setKeyName(String)} (String)} and
 * defaults to {@code DEFAULT_FORMAT_ATTRIBUTE}. Typical usage is to expose a
 * variable like <code>{format}</code> in the route (e.g. {@code /items.{format}}
 * or {@code /api/{format}/items}) and map values such as {@code json}/{@code xml}
 * to {@link org.jmouse.core.MediaType} through {@link MappedMediaTypeLookup#addExtension(String, org.jmouse.core.MediaType)}.</p>
 *
 * <h4>Notes</h4>
 * <ul>
 *   <li>If the route match or variable is absent, the base class returns an empty list.</li>
 *   <li>Configure the variable name during initialization to keep the component effectively immutable.</li>
 *   <li>Consider normalizing case of variable values when registering mappings.</li>
 * </ul>
 *
 * @author Ivan Hontarenko
 * @see MappedMediaTypeLookup
 * @see HandlerMapping#ROUTE_MATCH_ATTRIBUTE
 * @see RouteMatch
 */
public class PathVariableLookup extends MappedMediaTypeLookup {

    /** Creates a lookup using the default variable name {@code "format"}. */
    public PathVariableLookup() {
        super(DEFAULT_FORMAT_ATTRIBUTE);
    }

    /**
     * Extracts the mapping key from the current request's {@link RouteMatch}.
     *
     * @param request current HTTP request
     * @return the variable value, or {@code null} if not present
     */
    @Override
    protected String getMappingKey(HttpServletRequest request) {
        String mappingKey = null;

        if (getAttribute(HandlerMapping.ROUTE_MATCH_ATTRIBUTE) instanceof RouteMatch match) {
            mappingKey = (String) match.getVariable(getKeyName(), null);
        }

        return mappingKey;
    }

}
