package org.jmouse.parallax.script.reader;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.parallax.script.Block.InternalNode;

/** {@code [digital] name ["description"]}: an internal node of a component, analog unless digital. */
final class NodeLineParser extends JmxLineParser<InternalNode> {

    private final Reading reading;

    NodeLineParser(Reading reading) {
        this.reading = reading;
    }

    @Override
    InternalNode read() {
        TokenCursor line = reading.lineView();
        int length = line.remaining();
        boolean digital = line.checkValueAt(0, "digital");
        int nameAt = digital ? 1 : 0;
        if (length <= nameAt || length > nameAt + 2 || (length == nameAt + 2 && !reading.isString(line.lookAt(nameAt + 1)))) {
            throw reading.refusal(reading.cursor.current(), "an internal node is '[digital] name [\"description\"]'");
        }
        String description = length == nameAt + 2 ? reading.literal(line.lookAt(nameAt + 1)) : "";
        InternalNode node = new InternalNode(line.valueAt(nameAt), digital, description);
        reading.skipLine();
        return node;
    }
}
