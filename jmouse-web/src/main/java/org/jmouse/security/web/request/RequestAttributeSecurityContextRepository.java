package org.jmouse.security.web.request;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.security.SecurityContextHolder;
import org.jmouse.security.SecurityContext;
import org.jmouse.security.SupplierDeferredSecurityContext;
import org.jmouse.security.web.context.SecurityContextRepository;
import org.jmouse.web.http.RequestContextKeeper;

public class RequestAttributeSecurityContextRepository implements SecurityContextRepository {

    public static final String DEFAULT_REQUEST_ATTRIBUTE_KEY = RequestAttributeSecurityContextRepository.class.getName()
            .concat(".JMOUSE_SECURITY_CONTEXT");

    private final String attributeName;

    public RequestAttributeSecurityContextRepository(String attributeName) {
        this.attributeName = attributeName;
    }

    public RequestAttributeSecurityContextRepository() {
        this(DEFAULT_REQUEST_ATTRIBUTE_KEY);
    }

    @Override
    public SecurityContext load(RequestContextKeeper keeper) {
        return new SupplierDeferredSecurityContext(
                () -> load(keeper.request()), SecurityContextHolder.getContextHolderStrategy()).get();
    }

    @Override
    public void save(SecurityContext context, RequestContextKeeper keeper) {
        keeper.request().setAttribute(attributeName, context);
    }

    @Override
    public void clear(SecurityContext context, RequestContextKeeper keeper) {
        keeper.request().removeAttribute(attributeName);
    }

    @Override
    public boolean contains(SecurityContext context, RequestContextKeeper keeper) {
        return load(keeper.request()) != null;
    }

    public SecurityContext load(HttpServletRequest request) {
        return (SecurityContext) request.getAttribute(attributeName);
    }

}
