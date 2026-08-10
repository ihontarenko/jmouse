package org.jmouse.access.spring;

import org.jmouse.access.enforcement.ParameterNaming;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Parameter;
import java.util.Optional;

/**
 * What Spring MVC calls a handler's parameters.
 *
 * <p>The name a declaration matches on is the one the <em>request</em> uses — the value in
 * {@code @PathVariable("entryId")} where there is one, {@code @RequestParam("q")} otherwise, and the
 * declared name where neither says. That is the name a reader of the URL template already knows, and
 * matching on anything else would make {@code resourceId} refer to something invisible.
 *
 * <p>Only {@code @PathVariable} counts as an identifier. A query parameter is a filter; a path
 * segment is the thing being addressed, and "the row this route is about" is the second one.
 */
public class SpringParameterNaming implements ParameterNaming {

    @Override
    public Optional<String> requestName(Parameter parameter) {
        PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);

        if (pathVariable != null && !pathVariable.value().isBlank()) {
            return Optional.of(pathVariable.value());
        }

        RequestParam requestParam = parameter.getAnnotation(RequestParam.class);

        if (requestParam != null && !requestParam.value().isBlank()) {
            return Optional.of(requestParam.value());
        }

        return Optional.empty();
    }

    @Override
    public boolean isIdentifier(Parameter parameter) {
        return parameter.isAnnotationPresent(PathVariable.class);
    }
}
