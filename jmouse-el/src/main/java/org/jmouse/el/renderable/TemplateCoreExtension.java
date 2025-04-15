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
 * TemplateCoreExtension provides the core extensions required by the template engine.
 * <p>
 * This extension builds upon the base functionality provided by {@link CoreExtension}
 * and implements {@link TemplateExtension}. It supplies additional parsers for both
 * expressions and template-specific tag syntax. These parsers handle common template
 * constructs such as loops, conditionals, inheritance, blocks, macros, and placeholder text.
 * </p>
 * <p>
 * The extension ensures that the template engine can correctly interpret and build the
 * abstract syntax tree (AST) for templates by extending the base parsers with:
 * <ul>
 *   <li>{@link TemplateParser} – for parser template-specific syntax and structure.</li>
 *   <li>{@link RootParser} – for parser the root of the template layout.</li>
 * </ul>
 * Additionally, it provides a collection of {@link TagParser} implementations to process
 * individual template tags.
 * </p>
 *
 * @author ...
 */
public class TemplateCoreExtension extends CoreExtension implements TemplateExtension {

    /**
     * Returns a list of parsers used for parser template expressions and structure.
     * <p>
     * This method extends the default list of parsers inherited from {@link CoreExtension}
     * by adding:
     * <ul>
     *   <li>{@link TemplateParser} – handles the overall layout and structure of templates.</li>
     *   <li>{@link RootParser} – processes the root element of a template.</li>
     * </ul>
     * </p>
     *
     * @return a list of {@link Parser} instances for template parser
     */
    @Override
    public List<Parser> getParsers() {
        List<Parser> parsers = new ArrayList<>(super.getParsers());

        parsers.add(new TemplateParser());
        parsers.add(new RootParser());

        return parsers;
    }

    /**
     * Returns a list of tag parsers for processing the custom tags defined in templates.
     * <p>
     * The tag parsers provided by this extension support a variety of template directives:
     * <ul>
     *   <li>{@code for} – iterates over collections.</li>
     *   <li>{@code if} – conditionally renders sections.</li>
     *   <li>{@code extends} – defines template inheritance.</li>
     *   <li>{@code block} – marks sections that can be overridden.</li>
     *   <li>{@code macro} – defines reusable code blocks.</li>
     *   <li>{@code include} – includes another template within the current one.</li>
     *   <li>{@code import} – imports definitions (e.g., macros) from another template.</li>
     *   <li>{@code from} – a helper tag typically used with import.</li>
     *   <li>{@code with} – specifies arguments for macros or includes.</li>
     *   <li>{@code apply} – apply function to code block.</li>
     *   <li>{@code lorem} – generates placeholder text.</li>
     * </ul>
     * </p>
     *
     * @return a list of {@link TagParser} instances for handling template tags
     */
    @Override
    public List<TagParser> getTagParsers() {
        return List.of(
                new SetParser(),
                new ForParser(),
                new IfParser(),
                new IncludeParser(),
                new ImportParser(),
                new ExtendsParser(),
                new BlockParser(),
                new MacroParser(),
                new FromParser(),
                new ScopeParser(),
                new ApplyParser(),
                new EmbedParser(),
                new LoremParser()
        );
    }
}
