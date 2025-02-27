package org.jmouse.template.node;

import org.jmouse.template.lexer.Token;
import org.jmouse.util.helper.Strings;

import java.util.HashMap;
import java.util.Map;

public class EntryNode extends AbstractNode {

    protected final Token               token;
    protected final Map<String, Object> properties = new HashMap<>();

    public EntryNode() {
        this(null);
    }

    public EntryNode(Token token) {
        this.token = token;
    }

    public Token token() {
        return this.token;
    }

    @Override
    public String toString() {
        return String.format("%s ENTRY: [%s] PROPERTIES: %s", Strings.underscored(getClass().getSimpleName(), true),
                             token, properties);
    }

}
