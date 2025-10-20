package org.jmouse.security.web.configuration.matching;

import jakarta.servlet.DispatcherType;
import org.jmouse.security.web.configuration.Customizer;
import org.jmouse.web.http.RequestRoute;
import org.jmouse.web.match.routing.MappingMatcher;
import org.jmouse.web.match.routing.MatcherCriteria;
import org.jmouse.web.match.routing.condition.DispatcherTypeMatcher;

import java.util.ArrayList;
import java.util.List;

import static java.util.List.of;

abstract public class AbstractAuthorizationRequestMatchCriterion<T> {

    abstract protected T applyMatchers(List<MappingMatcher<RequestRoute>> requestMatchers);

    public T requestPath(String pattern) {
        return matcherCriteria(c -> c.pathPattern(pattern));
    }

    public T anyRequest() {
        return mappingMatcher(MatcherCriteria.any());
    }

    public T noneRequest() {
        return mappingMatcher(MatcherCriteria.none());
    }

    public T dispatcherType(DispatcherType... dispatcherTypes) {
        return matcherCriteria(c -> c.add(new DispatcherTypeMatcher(dispatcherTypes)));
    }

    @SafeVarargs
    public final T matcherCriteria(Customizer<MatcherCriteria>... customizers) {
        List<MappingMatcher<RequestRoute>> criteria = new ArrayList<>();

        for (Customizer<MatcherCriteria> customizer : customizers) {
            MatcherCriteria mappingCriteria = new MatcherCriteria();
            criteria.add(mappingCriteria);
            customizer.customize(mappingCriteria);
        }

        return applyMatchers(criteria);
    }

    @SafeVarargs
    public final T mappingMatcher(MappingMatcher<RequestRoute>... criteria) {
        return applyMatchers(of(criteria));
    }

}
