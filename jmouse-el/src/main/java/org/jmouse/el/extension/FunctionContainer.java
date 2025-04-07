package org.jmouse.el.extension;

import org.jmouse.el.AbstractObjectContainer;

public class FunctionContainer extends AbstractObjectContainer<String, Function> {

    /**
     * 🏷️ Retrieves the key associated with a given object.
     *
     * @param extension 🧩 the object to retrieve the key for
     * @return 🔑 the key associated with the object
     */
    @Override
    public String keyFor(Function extension) {
        return extension.getName();
    }

}
