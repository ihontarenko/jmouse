package svit.web.context.support;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import svit.beans.ObjectFactory;

public class HttpSessionObjectFactory implements ObjectFactory<HttpSession> {

    private final ObjectFactory<HttpServletRequest> factory;

    public HttpSessionObjectFactory(ObjectFactory<HttpServletRequest> factory) {
        this.factory = factory;
    }

    @Override
    public HttpSession createObject() {
        return factory.createObject().getSession();
    }

}
