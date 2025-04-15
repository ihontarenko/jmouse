package org.jmouse.el.parser;

/**
 * Represents configurable options for the parser.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface ParserOptions {

    default Class<? extends Parser> nextParser() {
        return null;
    }

    static ParserOptions withNextParser(Class<? extends Parser> nextParser) {
        return new Default(nextParser);
    }

    /**
     * DirectAccess implementation of {@link ParserOptions}.
     */
    record Default(Class<? extends Parser> nextParser) implements ParserOptions {

    }
}
