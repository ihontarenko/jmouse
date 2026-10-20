package org.jmouse.parallax.script.reader;

import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.parallax.script.Block;

/** {@code plus red [stripes black]}: a pin, its lead's colour, and optionally its stripes' colour. */
final class LeadLineParser extends JmxLineParser<LeadLineParser.Lead> {

    /**
     * What a lead line declares.
     *
     * @param first where the line begins, for a refusal of a lead described twice
     */
    record Lead(Token first, String pin, Block.LeadLook look) {
    }

    private final Reading reading;

    LeadLineParser(Reading reading) {
        this.reading = reading;
    }

    @Override
    Lead read() {
        TokenCursor line = reading.lineView();
        Token first = reading.cursor.current();
        int length = line.remaining();
        boolean striped = length == 4 && line.checkValueAt(2, "stripes");
        if (length != 2 && !striped) {
            throw reading.refusal(first, "a lead is 'pin colour [stripes colour]': 'plus red stripes black'");
        }
        for (int at : striped ? new int[] {1, 3} : new int[] {1}) {
            if (!Block.LEAD_COLOURS.contains(line.valueAt(at))) {
                throw reading.refusalAhead(at, "a lead is " + String.join(", ", Block.LEAD_COLOURS) + ", not '"
                        + line.valueAt(at) + "'");
            }
        }
        Lead lead = new Lead(first, line.valueAt(0), new Block.LeadLook(line.valueAt(1), striped ? line.valueAt(3) : null));
        reading.skipLine();
        return lead;
    }
}
