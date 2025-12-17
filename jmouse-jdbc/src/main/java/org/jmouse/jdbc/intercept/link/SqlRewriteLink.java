package org.jmouse.jdbc.intercept.link;

import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Link;
import org.jmouse.core.chain.Outcome;
import org.jmouse.jdbc.intercept.JdbcCall;
import org.jmouse.jdbc.intercept.JdbcExecutionContext;

import java.util.Objects;
import java.util.function.UnaryOperator;

public final class SqlRewriteLink implements Link<JdbcExecutionContext, JdbcCall<?>, Object> {

    private final UnaryOperator<String> rewriter;

    public SqlRewriteLink(UnaryOperator<String> rewriter) {
        this.rewriter = Objects.requireNonNull(rewriter, "rewriter");
    }

    @Override
    public Outcome<Object> handle(JdbcExecutionContext context, JdbcCall<?> input, Chain<JdbcExecutionContext, JdbcCall<?>, Object> next) {
        String rewritten = rewriter.apply(input.getSql());
        if (rewritten != null && !rewritten.equals(input.getSql())) {
            input.setSql(rewritten);
        }
        return Outcome.next();
    }
}
