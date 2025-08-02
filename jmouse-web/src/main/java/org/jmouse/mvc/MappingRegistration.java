package org.jmouse.mvc;

public interface MappingRegistration {
    Route getRoute();

    Object getHandler();

    void unregister();
}
