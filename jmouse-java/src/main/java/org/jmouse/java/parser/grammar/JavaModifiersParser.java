package org.jmouse.java.parser.grammar;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.model.version.JavaSourceFeatures;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.lexer.JavaKeywordCatalog;
import org.jmouse.java.parser.lexer.JavaKeywordDefinition;
import org.jmouse.java.parser.node.name.JavaModifierNode;
import org.jmouse.java.parser.node.name.JavaModifiersNode;

/** Preserves ordered annotations and modifier spellings; declaration rules determine admissibility. */
public final class JavaModifiersParser implements ParseRule<JavaParsingInput, JavaModifiersNode> {
    private final List<JavaKeywordDefinition> keywords;

    public JavaModifiersParser(Collection<? extends JavaKeywordDefinition> keywords) {
        this.keywords = new JavaKeywordCatalog(Objects.requireNonNull(keywords, "keywords")).definitions().stream()
                .sorted(Comparator.comparingInt((JavaKeywordDefinition keyword) -> keyword.spelling().length())
                        .reversed().thenComparing(JavaKeywordDefinition::spelling)).toList();
    }

    @Override
    public JavaModifiersNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        JavaModifiersNode modifiers = new JavaModifiersNode(input.name(), input.point());
        while (true) {
            if (input.matches("@") && !input.cursor().lookAt(1).value().equals(JavaKeyword.INTERFACE.spelling())) {
                modifiers.addEntry(session.parse(JavaParseRoles.ANNOTATION, input));
                continue;
            }
            JavaKeywordDefinition keyword = keywords.stream().filter(input::matches).findFirst().orElse(null);
            if (keyword == null) {
                return input.finish(modifiers, start);
            }
            int modifierStart = input.offset();
            input.expect(keyword, session);
            JavaModifierNode modifier = new JavaModifierNode(input.name(), input.spanFrom(modifierStart));
            modifier.setKeyword(keyword);
            modifiers.addEntry(modifier);
            if (keyword.spelling().equals(JavaKeyword.SEALED.spelling())
                    || keyword.spelling().equals(JavaKeyword.NON_SEALED.spelling())) {
                input.requireFeature(JavaSourceFeatures.SEALED_TYPES);
            }
        }
    }
}
