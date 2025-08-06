package org.jmouse.el.renderable.lexer;

import org.jmouse.el.lexer.*;
import org.jmouse.el.lexer.recognizer.Recognizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateTokenizer extends DefaultTokenizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateTokenizer.class);

    /**
     * Constructs a {@code TemplateTokenizer} with predefined type recognizers.
     * Registers standard and view tokens with specific priority values.
     */
    public TemplateTokenizer(Recognizer<Token.Type, RawToken> recognizer) {
        super(new TemplateSplitter(), recognizer);
    }

}
