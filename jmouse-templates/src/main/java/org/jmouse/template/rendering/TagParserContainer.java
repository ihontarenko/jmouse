package org.jmouse.template.rendering;

import org.jmouse.el.AbstractObjectContainer;

/**
 * 🏗️ A container for managing {@link TagParser} instances.
 * This implementation extends {@link AbstractObjectContainer} and uses
 * the parser's name as its unique key.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class TagParserContainer extends AbstractObjectContainer<String, TagParser> {

    /**
     * 🔑 Retrieves the unique key for a given {@link TagParser}.
     *
     * @param extension 🛠️ the tag parser instance
     * @return 🏷️ the name of the parser, used as its key
     */
    @Override
    public String keyFor(TagParser extension) {
        return extension.getName();
    }
}
