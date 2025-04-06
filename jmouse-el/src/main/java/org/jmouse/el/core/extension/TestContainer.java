package org.jmouse.el.core.extension;

import org.jmouse.el.core.AbstractObjectContainer;

public class TestContainer extends AbstractObjectContainer<String, Test> {

    /**
     * 🏷️ Retrieves the key associated with a given object.
     *
     * @param extension 🧩 the object to retrieve the key for
     * @return 🔑 the key associated with the object
     */
    @Override
    public String keyFor(Test extension) {
        return extension.getName();
    }

}
