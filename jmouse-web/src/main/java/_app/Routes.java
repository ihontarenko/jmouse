package _app;

import org.jmouse.mvc.BeanInstanceInitializer;
import org.jmouse.mvc.mapping.DirectRequestPathMapping;

public class Routes implements BeanInstanceInitializer<DirectRequestPathMapping> {

    @Override
    public void initialize(DirectRequestPathMapping mapping) {
        mapping.addController("/welcome",  (request, response)
                -> response.getWriter().write("Welcome " + request.getRequestURI()));
    }

    @Override
    public Class<DirectRequestPathMapping> objectClass() {
        return DirectRequestPathMapping.class;
    }

}
