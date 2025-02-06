package svit.expression.ast;

import org.jmouse.common.ast.node.EntryNode;
import org.jmouse.common.ast.token.Token;

public class IdentifierNode extends EntryNode  {

    private String identifier;

    public IdentifierNode(Token.Entry entry) {
        super(entry);
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

}
