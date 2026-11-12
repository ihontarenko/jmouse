package org.jmouse.web.mvc;

import jakarta.servlet.http.HttpServletRequest;

/** Shares a view model between argument resolution, controller execution and rendering in one servlet request. */
public final class RequestModel {

    private static final String ATTRIBUTE = RequestModel.class.getName() + ".MODEL";

    private RequestModel() {}

    public static Model get(HttpServletRequest request) {
        Object current = request.getAttribute(ATTRIBUTE);
        if (current instanceof Model model) return model;
        Model model = new DefaultModel();
        request.setAttribute(ATTRIBUTE, model);
        return model;
    }
}
