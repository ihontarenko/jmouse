package org.jmouse.el.writer;

import org.jmouse.el.node.expression.literal.IntegerLiteralNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InlineSourceWritingTest {
    @Test
    void expressionCompositionDoesNotInsertASemanticLineBreak() {
        SourceWriter writer = new TextSourceWriter(SourceLayout.WORDS);
        writer.word("return").write(new IntegerLiteralNode(1)).join(";");
        assertEquals("return 1;", writer.text());
    }
}
