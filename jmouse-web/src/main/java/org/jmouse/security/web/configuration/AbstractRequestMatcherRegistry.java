package org.jmouse.security.web.configuration;

import jakarta.servlet.DispatcherType;
import org.jmouse.security.web.RequestMatcher;
import org.jmouse.security.web.match.DispatcherTypeRequestMatcher;

import java.util.ArrayList;
import java.util.List;

import static java.util.List.of;
import static org.jmouse.security.web.RequestMatcher.any;
import static org.jmouse.security.web.RequestMatcher.pathPattern;

abstract public class AbstractRequestMatcherRegistry<C> {

    abstract protected C applyMatchers(List<RequestMatcher> requestMatchers);

    public C requestMatchers(String requestMatcher) {
        return requestMatchers(pathPattern(requestMatcher));
    }

    public C requestMatchers(RequestMatcher... requestMatchers) {
        return applyMatchers(of(requestMatchers));
    }

    public C anyRequest() {
        return requestMatchers(any());
    }

    public C dispatcherTypeMatchers(DispatcherType... dispatcherTypes) {
        List<RequestMatcher> requestMatchers = new ArrayList<>();

        for (DispatcherType dispatcherType : dispatcherTypes) {
            requestMatchers.add(new DispatcherTypeRequestMatcher(dispatcherType));
        }

        return applyMatchers(requestMatchers);
    }

}
