package org.jmouse.jdbc.parameters.compile;

import org.jmouse.el.StringSource;
import org.jmouse.el.lexer.TokenizableSource.Entry;
import org.jmouse.jdbc.parameters.SQLPlan;

import java.util.ArrayList;
import java.util.List;

import static org.jmouse.jdbc.parameters.SQLParameterToken.T_NAMED_PARAMETER;
import static org.jmouse.jdbc.parameters.SQLParameterToken.T_POSITIONAL_PARAMETER;
import static org.jmouse.jdbc.parameters.lexer.SQLParameterSplitter.Q_MARK;

/**
 * Compiles tokenized SQL parameter sources into an executable {@link SQLPlan}.
 *
 * <p>Named and positional parameters are replaced with JDBC {@code ?} placeholders,
 * while binding metadata is collected in declaration order.</p>
 */
public final class SQLPlanCompiler {

    /**
     * Compiles the given source into a SQL execution plan.
     *
     * <p>The resulting plan preserves the original SQL text, produces compiled SQL
     * with JDBC placeholders, and records parameter bindings for later value resolution.</p>
     *
     * @param source the tokenized SQL source
     * @return the compiled SQL plan
     */
    public SQLPlan compile(StringSource source) {
        String                original = source.toString();
        StringBuilder         buffer   = new StringBuilder(original.length());
        List<SQLPlan.Binding> bindings = new ArrayList<>();

        int cursor            = 0;
        int positionalCounter = 0;

        for (int i = 0; i < source.size(); i++) {
            Entry entry = source.get(i);

            if (entry.token() == T_POSITIONAL_PARAMETER || entry.token() == T_NAMED_PARAMETER) {
                buffer.append(source, cursor, entry.offset());
                buffer.append(Q_MARK);
                cursor = entry.offset() + entry.length();

                if (entry.token() == T_NAMED_PARAMETER) {
                    bindings.add(new SQLPlan.Binding.Named(entry.segment().substring(1), entry.segment()));
                }

                if (entry.token() == T_POSITIONAL_PARAMETER) {
                    bindings.add(new SQLPlan.Binding.Positional(++positionalCounter));
                }
            }
        }

        buffer.append(source, cursor, source.length());

        return new SQLPlan(original, buffer.toString(), List.copyOf(bindings));
    }
}