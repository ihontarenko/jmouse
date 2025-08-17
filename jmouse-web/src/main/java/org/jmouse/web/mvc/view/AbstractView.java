package org.jmouse.web.mvc.view;

import org.jmouse.core.MediaType;
import org.jmouse.web.mvc.View;

/**
 * 🧩 Base class for {@link View} implementations.
 *
 * Provides default support for storing a {@link MediaType} (like <code>text/html</code>).
 * Extend this to implement specific rendering strategies (e.g. HTML, JSON, etc).
 *
 * @author Ivan Hontarenko
 * @author ihontarenko@gmail.com
 */
public abstract class AbstractView implements View {

    /** 🎯 The response content type (e.g. text/html, application/json). */
    protected MediaType contentType;

    /**
     * 📄 Constructs an HTML view by default.
     */
    public AbstractView() {
        this(MediaType.TEXT_HTML);
    }

    /**
     * 📄 Constructs a view with the specified content type.
     *
     * @param contentType the media type of the response (e.g. {@code MediaType.APPLICATION_JSON})
     */
    public AbstractView(MediaType contentType) {
        this.contentType = contentType;
    }

    /**
     * 🏷️ Returns the view's content type.
     *
     * @return media type to be set as response Content-Type
     */
    @Override
    public MediaType getContentType() {
        return contentType;
    }

}
