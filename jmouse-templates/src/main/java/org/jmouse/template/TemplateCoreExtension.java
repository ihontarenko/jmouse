package org.jmouse.template;

import org.jmouse.el.extension.StandardExtension;
import org.jmouse.el.parser.TagParser;
import org.jmouse.template.parsing.tag.ForParser;
import org.jmouse.template.parsing.tag.IfParser;
import org.jmouse.template.parsing.tag.LoremParser;

import java.util.List;

public class TemplateCoreExtension extends StandardExtension {

    @Override
    public List<TagParser> getTagParsers() {
        return List.of(
                new ForParser(),
                new IfParser(),
                new LoremParser()
        );
    }

}
