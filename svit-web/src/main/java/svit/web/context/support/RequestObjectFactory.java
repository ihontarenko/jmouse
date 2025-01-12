package svit.web.context.support;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import svit.beans.ObjectFactory;
import svit.web.context.WebBeanContext;

/**
 * An {@link ObjectFactory} implementation that retrieves the current {@link HttpServletRequest}
 * from the {@link ServletContext}. This allows classes to lazily obtain the current request
 * without directly depending on request-scoped objects.
 *
 * <p>Example usage:
 * <pre>{@code
 * ObjectFactory<ServletContext> servletContextFactory = ...;
 * RequestObjectFactory requestObjectFactory = new RequestObjectFactory(servletContextFactory);
 * HttpServletRequest request = requestObjectFactory.createObject();
 * }</pre>
 */
public class RequestObjectFactory implements ObjectFactory<HttpServletRequest> {

    private final ObjectFactory<ServletContext> factory;

    /**
     * Constructs a new {@code RequestObjectFactory} with the given factory for obtaining
     * the {@link ServletContext}.
     *
     * @param factory the factory responsible for providing the {@link ServletContext}.
     */
    public RequestObjectFactory(ObjectFactory<ServletContext> factory) {
        this.factory = factory;
    }

    /**
     * Creates and returns the current {@link HttpServletRequest} by fetching it from
     * the {@link ServletContext}'s attributes, using the {@link WebBeanContext#CURRENT_REQUEST} key.
     *
     * @return the current {@link HttpServletRequest}, or {@code null} if not available.
     */
    @Override
    public HttpServletRequest createObject() {
        return (HttpServletRequest) factory.createObject().getAttribute(WebBeanContext.CURRENT_REQUEST);
    }
}
