package org.jmouse.security.web.configuration.matching;

import jakarta.servlet.DispatcherType;
import org.jmouse.security.web.configuration.Customizer;
import org.jmouse.web.http.RequestRoute;
import org.jmouse.web.match.routing.MappingMatcher;
import org.jmouse.web.match.routing.MatcherCriteria;
import org.jmouse.web.match.routing.condition.DispatcherTypeMatcher;

import java.util.List;

import static java.util.List.of;

abstract public class AbstractAuthorizationConfigurer<T> {

    abstract protected T applyMatchers(List<MappingMatcher<RequestRoute>> requestMatchers);

    public T requestPath(String pattern) {
        return matcherCriteria(c -> c.pathPattern(pattern));
    }

    public T anyRequest() {
        return matcherCriteria(MatcherCriteria.any());
    }

    public T noneRequest() {
        return matcherCriteria(MatcherCriteria.none());
    }

    public T dispatcherType(DispatcherType... dispatcherTypes) {
        return matcherCriteria(c -> c.add(new DispatcherTypeMatcher(dispatcherTypes)));
    }

    public T matcherCriteria(Customizer<MatcherCriteria> customizer) {
        MatcherCriteria mappingCriteria = new MatcherCriteria();
        customizer.customize(mappingCriteria);
        return matcherCriteria(mappingCriteria);
    }

    @SafeVarargs
    public final T matcherCriteria(MappingMatcher<RequestRoute>... criteria) {
        return applyMatchers(of(criteria));
    }

}
