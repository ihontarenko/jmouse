package org.jmouse.template;

import java.util.function.UnaryOperator;

public interface Rendering<T> {

    default T render(String blueprintKey, Object data) {
        return render(blueprintKey, data, request -> request);
    }

    T render(String blueprintKey, Object data, UnaryOperator<RenderingRequest> requestCustomizer);

}
