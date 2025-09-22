package org.jmouse.core.proxy;

import org.jmouse.core.matcher.Matcher;

public interface InvocationMatcher extends Matcher<MethodInvocation> {

    static InvocationMatcher any() {
        return (InvocationMatcher) Matcher.<MethodInvocation>constant(true);
    }

}
