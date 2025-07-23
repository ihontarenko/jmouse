package org.jmouse.mvc.mapping;

import org.jmouse.mvc.handler.Controller;

public record ControllerRegistration(String route, Controller controller) {
}
