package org.jmouse.common.support.resolver;

public class BasicParameterResolver extends AbstractValueResolver {

    @Override
    public Object resolve(Object value, ResolverContext context) {
        return value;
    }

}
