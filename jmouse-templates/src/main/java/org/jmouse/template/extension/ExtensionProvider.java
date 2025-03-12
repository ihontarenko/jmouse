package org.jmouse.template.extension;

import org.jmouse.template.parser.TagParser;
import org.jmouse.template.parser.Parser;

import java.util.List;

public interface ExtensionProvider {

    List<TagParser> getTagParsers();

    List<Parser> getParsers();

    List<Operator> getOperators();

    List<Function> getFunctions();

    List<Test> getTests();

    List<Filter> getFilters();

}
