package org.jmouse.security.web.configuration.matching;

import jakarta.servlet.DispatcherType;
import org.jmouse.security.web.RequestMatcher;
import org.jmouse.security.web.configuration.Customizer;
import org.jmouse.security.web.match.DispatcherTypeRequestMatcher;
import org.jmouse.web.match.Route;
import org.jmouse.web.match.routing.MappingCriteria;

import java.util.ArrayList;
import java.util.List;

import static java.util.List.of;
import static org.jmouse.security.web.RequestMatcher.any;
import static org.jmouse.security.web.RequestMatcher.pathPattern;

abstract public class AbstractRequestMatcherRegistry<T> {

    abstract protected T applyMatchers(List<MappingCriteria> requestMatchers);

    public T requestMatchers(String requestMatcher) {
        return requestMatchers(pathPattern(requestMatcher));
    }

    public T requestMatchers(Customizer<Route.Builder> customizer) {
        Route.Builder builder = Route.route();
        customizer.customize(builder);
        MappingCriteria mappingCriteria = new MappingCriteria(builder.toRoute());
        return requestMatchers(pathPattern(requestMatcher));
    }

    public T requestMatchers(RequestMatcher... requestMatchers) {
        return applyMatchers(of(requestMatchers));
    }

    public T anyRequest() {
        return requestMatchers(any());
    }

    public T dispatcherTypeMatchers(DispatcherType... dispatcherTypes) {
        List<RequestMatcher> requestMatchers = new ArrayList<>();

        for (DispatcherType dispatcherType : dispatcherTypes) {
            requestMatchers.add(new DispatcherTypeRequestMatcher(dispatcherType));
        }

        return applyMatchers(requestMatchers);
    }

}
