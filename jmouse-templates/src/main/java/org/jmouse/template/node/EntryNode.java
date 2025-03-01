package org.jmouse.template.node;

import org.jmouse.template.lexer.Token;

import java.util.HashMap;
import java.util.Map;

import static org.jmouse.util.helper.Strings.underscored;

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
        return "%s ENTRY: [%s] PROPERTIES: %s"
                .formatted(underscored(getClass().getSimpleName(), true), token, properties);
    }

}
