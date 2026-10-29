package org.jmouse.gerber.excellon.node;

import org.jmouse.el.writer.SourceWriter;
import org.jmouse.gerber.GerberPosition;

import java.util.List;

/** A whole Excellon file: its statements, a line each. */
public final class ExcellonProgram extends ExcellonNode {

    public ExcellonProgram(GerberPosition position) {
        super(position);
    }

    public static ExcellonProgram of(List<? extends ExcellonStatement> statements) {
        ExcellonProgram program = new ExcellonProgram(GerberPosition.NONE);
        statements.forEach(program::add);
        return program;
    }

    public List<ExcellonStatement> statements() {
        return getChildren(ExcellonStatement.class);
    }

    @Override
    public void writeSource(SourceWriter writer) {
        writer.each(statements());
    }
}
