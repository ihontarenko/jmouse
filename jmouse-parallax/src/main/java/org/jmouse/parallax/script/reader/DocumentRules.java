package org.jmouse.parallax.script.reader;

import org.jmouse.el.lexer.Token;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.Block.ParameterFlag;
import org.jmouse.parallax.script.Capabilities;
import org.jmouse.parallax.script.ExperienceDocument;
import org.jmouse.parallax.script.ExperienceParseException;
import org.jmouse.parallax.script.Kind;
import org.jmouse.parallax.script.SourceText;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** The rules a document is held to once all of it is read, because they need more than one of its blocks. */
final class DocumentRules {

    private final Reading reading;

    DocumentRules(Reading reading) {
        this.reading = reading;
    }

    /** Time is either an input or what evolves the state, never both; a component's expectation has a bench. */
    void validate(ExperienceDocument document, Token head) {
        boolean animated = document.parameters().stream().anyMatch(parameter -> parameter.has(ParameterFlag.ANIMATE));
        if (animated && Capabilities.evolves(document)) {
            throw reading.refusal(head, "'" + document.title() + "' evolves its state in time, so no parameter can be "
                    + "'animate': time is either an input or what runs the state, not both");
        }
        if (document.kind() == Kind.COMPONENT) {
            for (Block.Expectation expectation : document.blocks(Block.Expectation.class)) {
                if (expectation.bench().isEmpty() && expectation.sharedBench() == null) {
                    throw new ExperienceParseException(reading.document(), expectation.line(), 0, "expectation '"
                            + expectation.title() + "' of a component needs a bench: 'bench for 3 ms { parts }'");
                }
            }
        }
        for (Block.Output output : document.outputs()) {
            if (output.components().stream().anyMatch(SourceText::isEmpty)) {
                throw reading.refusal(head, "output '" + output.name() + "' has an empty expression");
            }
        }
        if (document.kind() == Kind.GRAPH) {
            validateGraph(document, head);
        }
    }

    /** A clock moves a parameter or names its own range; a line is drawn in a pane the graph has. */
    private void validateGraph(ExperienceDocument document, Token head) {
        document.block(Block.Clock.class).ifPresent(clock -> {
            boolean declared = document.parameters().stream().anyMatch(parameter -> parameter.name().equals(clock.parameter()));
            if (!declared && clock.range().isEmpty()) {
                throw reading.refusal(head, "the clock moves '" + clock.parameter() + "', which is not a parameter: "
                        + "declare it, or give the clock its range — 'clock " + clock.parameter() + " from 0 to 1'");
            }
        });
        List<Block.Pane> written = document.block(Block.Panes.class).map(Block.Panes::panes).orElse(List.of());
        Set<String> panes = written.stream().map(Block.Pane::name).collect(Collectors.toSet());
        Set<String> circuitPanes = written.stream().filter(pane -> pane.kind() == Block.PaneKind.CIRCUIT)
                .map(Block.Pane::name).collect(Collectors.toSet());
        for (Block.GraphLine line : document.block(Block.Lines.class).map(Block.Lines::lines).orElse(List.of())) {
            if (line.from().isPresent() && !circuitPanes.contains(line.from().get())) {
                throw new ExperienceParseException(reading.document(), line.line(), 0, "line '" + line.name()
                        + "' reads its signal from '" + line.from().get() + "', which is not a circuit pane of this graph");
            }
            if (line.pane().isPresent() && !panes.contains(line.pane().get())) {
                throw new ExperienceParseException(reading.document(), line.line(), 0, "line '" + line.name()
                        + "' is drawn in '" + line.pane().get() + "', which is not a pane of this graph");
            }
        }
    }
}
