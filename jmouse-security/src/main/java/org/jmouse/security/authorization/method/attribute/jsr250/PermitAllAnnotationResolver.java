package org.jmouse.security.authorization.method.attribute.jsr250;

import jakarta.annotation.security.PermitAll;

public class PermitAllAnnotationResolver extends Jsr250AnnotationResolver<PermitAll> {

    public static final String PERMIT_ALL_EL = "permitAll()";

    @Override
    public Class<PermitAll> annotationType() {
        return PermitAll.class;
    }

    @Override
    public int order() {
        return 5;
    }

    @Override
    protected String getExpressionString(PermitAll annotation) {
        return PERMIT_ALL_EL;
    }

}