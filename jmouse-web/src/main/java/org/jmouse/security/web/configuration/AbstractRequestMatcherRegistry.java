package org.jmouse.security.web.configuration;

import jakarta.servlet.DispatcherType;
import org.jmouse.security.web.RequestMatcher;
import org.jmouse.security.web.match.DispatcherTypeRequestMatcher;

import java.util.ArrayList;
import java.util.List;

abstract public class AbstractRequestMatcherRegistry<C> {

    abstract protected C applyMatchers(List<RequestMatcher> requestMatchers);

    public C requestMatchers(RequestMatcher... requestMatchers) {
        return applyMatchers(List.of(requestMatchers));
    }

    public C dispatcherTypeMatchers(DispatcherType... dispatcherTypes) {
        List<RequestMatcher> requestMatchers = new ArrayList<>();

        for (DispatcherType dispatcherType : dispatcherTypes) {
            requestMatchers.add(new DispatcherTypeRequestMatcher(dispatcherType));
        }

        return applyMatchers(requestMatchers);
    }

}
