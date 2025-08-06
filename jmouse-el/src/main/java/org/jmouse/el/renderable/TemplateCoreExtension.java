package org.jmouse.el.renderable;

import org.jmouse.el.extension.CoreExtension;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.TagParser;
import org.jmouse.el.renderable.parser.RootParser;
import org.jmouse.el.renderable.parser.TemplateParser;
import org.jmouse.el.renderable.parser.tag.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Core extension for the view engine that adds view-specific parsing capabilities.
 * <p>
 * Extends {@link CoreExtension} by registering:
 * <ul>
 *   <li>Structural parsers for overall view syntax (via {@link TemplateParser} and {@link RootParser}).</li>
 *   <li>A suite of {@link TagParser} implementations for common view tags
 *       (e.g., loops, conditionals, inheritance, blocks, macros, imports, includes, etc.).</li>
 * </ul>
 * </p>
 */
public class TemplateCoreExtension extends CoreExtension implements TemplateExtension {

    /**
     * Returns the full set of expression and structural parsers, including
     * the core extensions plus the view-specific parsers.
     *
     * @return a list of {@link Parser} instances for parsing view syntax
     */
    @Override
    public List<Parser> getParsers() {
        List<Parser> parsers = new ArrayList<>(super.getParsers());

        parsers.add(new TemplateParser());
        parsers.add(new RootParser());

        return parsers;
    }

    /**
     * Returns the collection of tag parsers used to recognize and handle
     * view directives such as loops, imports, blocks, macros, and more.
     *
     * @return an immutable list of {@link TagParser} instances
     */
    @Override
    public List<TagParser> getTagParsers() {
        return List.of(
                new SetParser(),
                new DoParser(),
                new ForParser(),
                new IfParser(),
                new IncludeParser(),
                new UseParser(),
                new ExtendsParser(),
                new BlockParser(),
                new MacroParser(),
                new ScopeParser(),
                new ApplyParser(),
                new EmbedParser(),
                new RenderParser(),
                new ParentParser(),
                new CacheParser(),
                new LoremParser(),
                new URLParser()
        );
    }
}
