package org.jmouse.json.syntax;

import org.jmouse.el.extension.Extension;
import org.jmouse.el.parser.Parser;

import java.util.List;

/**
 * 🧾 JSON's parsers, as the engine takes a language's: one for a value, and one for each kind of value it may be.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JsonExtension implements Extension {

    @Override
    public List<Parser> getParsers() {
        return List.of(new JsonValueParser(), new JsonObjectParser(), new JsonArrayParser(), new JsonTextParser(), new JsonNumberParser());
    }
}
