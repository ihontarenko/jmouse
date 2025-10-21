package org.jmouse.security.authorization.method.attribute.jsr250;

import jakarta.annotation.security.DenyAll;

public class DenyAllAnnotationResolver extends Jsr250AnnotationResolver<DenyAll> {

    public static final String DENY_ALL_EL = "denyAll()";

    @Override
    public Class<DenyAll> annotationType() {
        return DenyAll.class;
    }

    @Override
    public int order() {
        return 5;
    }

    @Override
    protected String getExpressionString(DenyAll annotation) {
        return DENY_ALL_EL;
    }

}