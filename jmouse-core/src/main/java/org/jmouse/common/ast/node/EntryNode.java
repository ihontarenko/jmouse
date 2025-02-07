package org.jmouse.common.ast.node;

import org.jmouse.common.ast.token.Token;
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

    public Map<String, Object> properties() {
        return this.properties;
    }

    public Object getAttribute(String key) {
        return this.properties.get(key);
    }

    public Object setAttribute(String key, Object value) {
        return this.properties.put(key, value);
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
