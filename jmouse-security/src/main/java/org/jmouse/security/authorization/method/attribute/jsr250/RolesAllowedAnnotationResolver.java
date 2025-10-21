package org.jmouse.security.authorization.method.attribute.jsr250;

import jakarta.annotation.security.RolesAllowed;
import org.jmouse.core.Streamable;
import org.jmouse.util.StringHelper;

public class RolesAllowedAnnotationResolver extends Jsr250AnnotationResolver<RolesAllowed> {

    @Override
    public Class<RolesAllowed> annotationType() {
        return RolesAllowed.class;
    }

    @Override
    protected String getExpressionString(RolesAllowed annotation) {
        String contains = Streamable.of(annotation.value())
                .map(StringHelper::unquote).map(role -> "is contains('" + role + "')")
                .joining(" or ");
        return "authentication.authorities | map(a -> a.authority) | sout";
    }
}