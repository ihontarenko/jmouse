package org.jmouse.el.renderable;

import org.jmouse.el.core.extension.StandardExtension;
import org.jmouse.el.core.parser.Parser;
import org.jmouse.el.core.parser.TagParser;
import org.jmouse.el.renderable.parsing.RootParser;
import org.jmouse.el.renderable.parsing.TemplateParser;
import org.jmouse.el.renderable.parsing.tag.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 📜 TemplateCoreExtension provides core extensions for the template engine.
 * <p>
 * It extends {@link StandardExtension} and implements {@link TemplateExtension},
 * adding additional parsers and tag parsers specific to template processing, such as
 * for loops, conditionals, inheritance, blocks, and placeholder text.
 * </p>
 *
 * @author ...
 */
public class TemplateCoreExtension extends StandardExtension implements TemplateExtension {

    /**
     * Returns a list of parsers for template expressions and overall structure.
     * <p>
     * This method extends the base parsers from {@link StandardExtension} by adding the
     * {@link TemplateParser} and {@link RootParser}, which handle the overall template layout.
     * </p>
     *
     * @return a list of {@link Parser} instances used for template parsing
     */
    @Override
    public List<Parser> getParsers() {
        List<Parser> parsers = new ArrayList<>(super.getParsers());

        parsers.add(new TemplateParser());
        parsers.add(new RootParser());

        return parsers;
    }

    /**
     * Returns a list of tag parsers for processing template tags.
     * <p>
     * The provided tag parsers support the following tags:
     * <ul>
     *   <li>{@code for} – for iterating over collections</li>
     *   <li>{@code if} – for conditional processing</li>
     *   <li>{@code extends} – for template inheritance</li>
     *   <li>{@code block} – for defining overridable content sections</li>
     *   <li>{@code lorem} – for placeholder text</li>
     * </ul>
     * </p>
     *
     * @return a list of {@link TagParser} instances for handling template tags
     */
    @Override
    public List<TagParser> getTagParsers() {
        return List.of(
                new ForParser(),
                new IfParser(),
                new IncludeParser(),
                new ExtendsParser(),
                new BlockParser(),
                new MacroParser(),
                new LoremParser()
        );
    }
}
