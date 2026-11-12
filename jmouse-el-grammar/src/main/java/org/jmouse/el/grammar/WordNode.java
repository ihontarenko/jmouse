package org.jmouse.el.grammar;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.writer.SourceWriter;

/**
 * One word of a line, hyphens and all — {@code through-hole}, {@code R1} —, or a text in quotes when {@code quoted}. A
 * word holding what a bare word cannot — a space, a quote, a comment's mark, or a mark that ends a word
 * ({@link Lines#WORD_SEPARATORS}: the colon of {@code FP.CHIP:R0805}) — is written in quotes whatever it was made as, so
 * what is written reads back as the same word ({@code Values.WORD} reads a quoted word as the word inside).
 */
public class WordNode extends AbstractExpression {

    private final String  word;
    private final boolean quoted;

    public WordNode(String word, boolean quoted) {
        this.word = word;
        this.quoted = quoted;
    }

    public String word() {
        return word;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return word;
    }

    @Override
    public void writeSource(SourceWriter writer) {
        if (quoted || !readsBare(word)) {
            writer.text(word);
        } else {
            writer.word(word);
        }
    }

    /** Whether a word reads back whole when written without quotes. */
    static boolean readsBare(String word) {
        if (word == null || word.isEmpty()) {
            return true;
        }
        for (int index = 0; index < word.length(); index++) {
            char character = word.charAt(index);
            if (Character.isWhitespace(character) || character == '"' || character == '\''
                    || Lines.COMMENT_MARK.indexOf(character) >= 0 || Lines.WORD_SEPARATORS.contains(String.valueOf(character))) {
                return false;
            }
        }
        return true;
    }
}
