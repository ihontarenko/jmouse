package org.jmouse.template.node;
import org.jmouse.template.lexer.Token;
import org.jmouse.util.helper.Strings;

import java.util.HashMap;
import java.util.Map;

public class EntryNode extends AbstractNode {

    protected final Token.Entry         entry;
    protected final Map<String, Object> properties = new HashMap<>();

    public EntryNode() {
        this(null);
    }

    public EntryNode(Token.Entry entry) {
        this.entry = entry;
    }

    public Token.Entry entry() {
        return this.entry;
    }

    @Override
    public String toString() {
        return String.format("%s ENTRY: [%s] PROPERTIES: %s", Strings.underscored(getClass().getSimpleName(), true),
                             entry, properties);
    }

}
