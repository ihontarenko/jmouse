package org.jmouse.jdbc.parameters.compile;

import org.jmouse.core.Contract;
import org.jmouse.el.ExpressionLanguage;
import org.jmouse.el.StringSource;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.lexer.TokenizableSource.Entry;
import org.jmouse.el.node.Expression;
import org.jmouse.jdbc.parameters.SQLPlan;
import org.jmouse.jdbc.parameters.SQLParameterToken;
import org.jmouse.jdbc.parameters.lexer.SQLParameterSplitter;

import java.util.ArrayList;
import java.util.List;

public final class SQLPlanCompiler {

    private final ExpressionLanguage expressionLanguage;

    public SQLPlanCompiler(ExpressionLanguage expressionLanguage) {
        this.expressionLanguage = Contract.nonNull(expressionLanguage, "expressionLanguage");
    }

    public SQLPlan compile(StringSource source) {
        String                original = source.toString();
        StringBuilder         buffer   = new StringBuilder(original.length());
        List<SQLPlan.Binding> bindings = new ArrayList<>();

        int cursor            = 0;
        int positionalCounter = 0;

        EvaluationContext evaluationContext = expressionLanguage.newContext();

        for (int i = 0; i < source.size(); i++) {
            Entry entry = source.get(i);

            if (entry.token() == SQLParameterToken.T_NAMED_PARAMETER) {
                buffer.append(source, cursor, entry.offset());
                buffer.append(SQLParameterSplitter.Q_MARK);
                cursor = entry.offset() + entry.length();

                NamedSpecification specification = NamedSpecification.parse(entry.segment());
                Expression         node          = null;

                if (specification.hasPipeline()) {
                    node = expressionLanguage.compile(entry.segment().substring(1));
                }

                bindings.add(new SQLPlan.Binding.Named(specification.name(), entry.segment(), node));
            }

            if (entry.token() == SQLParameterToken.T_POSITIONAL_PARAMETER) {
                buffer.append(source, cursor, entry.offset());
                buffer.append(SQLParameterSplitter.Q_MARK);
                cursor = entry.offset() + entry.length();
                bindings.add(new SQLPlan.Binding.Positional(++positionalCounter));
            }
        }

        buffer.append(source, cursor, source.length());

        return new SQLPlan(original, buffer.toString(), List.copyOf(bindings));
    }

    static final class NamedSpecification {
        private final String name;
        private final String pipeline; // "upper|trim" or null

        private NamedSpecification(String name, String pipeline) {
            this.name = name;
            this.pipeline = pipeline;
        }

        static NamedSpecification parse(String rawToken) {
            // rawToken looks like ":name" or ":name|upper|trim"
            if (rawToken == null || rawToken.length() < 2 || rawToken.charAt(0) != ':') {
                throw new IllegalArgumentException("Invalid named parameter token: " + rawToken);
            }

            String body = rawToken.substring(1);
            int    pipe = body.indexOf('|');

            if (pipe < 0) {
                return new NamedSpecification(body, null);
            }

            String name     = body.substring(0, pipe);
            String pipeline = body.substring(pipe + 1);

            return new NamedSpecification(name, pipeline);
        }

        String name() {
            return name;
        }

        boolean hasPipeline() {
            return pipeline != null && !pipeline.isBlank();
        }

        String pipeline() {
            return pipeline;
        }
    }
}
