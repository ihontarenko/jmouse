package org.jmouse.mvc._test;

import org.jmouse.beans.annotation.Factories;
import org.jmouse.beans.annotation.Provide;

@Factories
public class ServiceConfig {

    @Provide
    public Service getService() {
        return new Service("core");
    }

}
