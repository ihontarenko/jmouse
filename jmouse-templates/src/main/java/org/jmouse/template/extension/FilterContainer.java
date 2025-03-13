package org.jmouse.template.extension;

import org.jmouse.template.AbstractObjectContainer;

public class FilterContainer extends AbstractObjectContainer<String, Filter> {

    /**
     * 🏷️ Retrieves the key associated with a given object.
     *
     * @param extension 🧩 the object to retrieve the key for
     * @return 🔑 the key associated with the object
     */
    @Override
    public String keyFor(Filter extension) {
        return extension.getName();
    }

}
