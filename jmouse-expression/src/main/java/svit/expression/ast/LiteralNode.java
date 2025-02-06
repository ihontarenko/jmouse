package svit.expression.ast;

import org.jmouse.common.ast.node.EntryNode;
import org.jmouse.common.ast.compiler.EvaluationContext;
import org.jmouse.common.ast.token.Token;

import static java.lang.Integer.parseInt;

public class LiteralNode extends EntryNode {

    private Object value;

    public LiteralNode(Token.Entry entry) {
        super(entry);
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public Object evaluate(EvaluationContext ctx) {
        return ctx.getCompiler(this).compile(this, ctx);
    }

}
