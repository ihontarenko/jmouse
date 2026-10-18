package org.jmouse.java.parser.source;

import org.jmouse.java.parser.model.ExecutableBody;
import org.jmouse.java.parser.model.SymbolId;

import java.util.List;

/** Shared conservative validity policy for source models and persisted file indexes. */
public final class ParseValidity {
    private ParseValidity() {}

    public static boolean structureComplete(List<ParseProblem> problems) {
        return problems.stream().allMatch(problem -> problem.phase() == ParseProblem.Phase.BODY);
    }

    public static boolean bodyComplete(
            List<ParseProblem> problems, List<ExecutableBody> bodies, SymbolId owner) {
        return structureComplete(problems)
                && bodies.stream()
                        .filter(body -> body.owner().equals(owner))
                        .anyMatch(
                                body ->
                                        problems.stream()
                                                .noneMatch(
                                                        problem ->
                                                                problem.phase()
                                                                                == ParseProblem
                                                                                        .Phase.BODY
                                                                        && problem.span()
                                                                                        .start()
                                                                                        .offset()
                                                                                >= body.span()
                                                                                        .start()
                                                                                        .offset()
                                                                        && problem.span()
                                                                                        .start()
                                                                                        .offset()
                                                                                <= body.span()
                                                                                        .end()
                                                                                        .offset()));
    }

    /** A recovered neighbouring declaration does not erase a disjoint retained declaration. */
    public static boolean declarationComplete(List<ParseProblem> problems, SourceSpan span) {
        return problems.stream()
                .noneMatch(
                        problem ->
                                problem.phase() == ParseProblem.Phase.LEXICAL
                                        || (problem.phase() == ParseProblem.Phase.STRUCTURE
                                                && problem.span().start().offset()
                                                        <= span.end().offset()
                                                && problem.span().end().offset()
                                                        >= span.start().offset()));
    }
}
