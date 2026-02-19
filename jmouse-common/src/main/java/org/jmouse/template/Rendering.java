package org.jmouse.template;

import java.util.function.UnaryOperator;

public interface Rendering<T> {

    default T render(String templateKey, Object data) {
        return render(templateKey, data, request -> request);
    }

    T render(String templateKey, Object data, UnaryOperator<RenderingRequest> requestCustomizer);

}
