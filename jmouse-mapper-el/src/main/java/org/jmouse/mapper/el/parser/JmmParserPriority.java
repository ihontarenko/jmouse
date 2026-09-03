package org.jmouse.mapper.el.parser;

/**
 * The order the mapping parsers are offered a statement in.
 *
 * <p>Dispatch asks every parser {@code supports(cursor)} in priority order and takes the first yes,
 * where a <em>lower</em> number is asked earlier.</p>
 *
 * <h2>⚠️ The old javadoc's argument still stands, and this is not a retraction of it</h2>
 *
 * <p>{@code JmmParser} argued that a priority registry was machinery answering a question this grammar
 * never asks, because no two constructions open the same way. That is <strong>true</strong>, and these
 * numbers are not what makes the grammar work: every parser's {@code supports} is exact, and each
 * keyword-led one asks the second question a keyword always has to answer — is a colon behind it, in
 * which case the word is a property name and not a keyword at all.</p>
 *
 * <p>What the registry bought was not disambiguation. It was {@code AbstractBodyParser},
 * {@code StatementsParser} and the no-progress guard that came with them — three things the hand-written
 * reader re-implemented and one it did not have at all.</p>
 *
 * <p>So the order below is a safety net: if two shapes ever do overlap, the more specific one wins.
 * The rule parser sorts last because it is the default — a line is a rule when it is nothing else.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JmmParserPriority {

    /**
     * ⚠️ {@link MappingDocumentParser} is deliberately absent. It is the root a reader hands a whole
     * file to, not one more shape a statement might be, and its {@code supports} stays {@code false}.
     */
    public static final int MAPPING  = Integer.MIN_VALUE + 1000;
    public static final int USE      = Integer.MIN_VALUE + 1100;
    public static final int FRAGMENT = Integer.MIN_VALUE + 1200;
    public static final int TARGET   = Integer.MIN_VALUE + 1300;
    public static final int UNMAPPED = Integer.MIN_VALUE + 1400;
    public static final int REFUSE   = Integer.MIN_VALUE + 1500;
    public static final int ALWAYS   = Integer.MIN_VALUE + 1600;
    public static final int FROM     = Integer.MIN_VALUE + 1700;
    public static final int LET      = Integer.MIN_VALUE + 1800;
    public static final int INCLUDE  = Integer.MIN_VALUE + 1900;

    /** The default statement of a rule block, therefore last. */
    public static final int RULE     = Integer.MIN_VALUE + 2500;

    private JmmParserPriority() {
    }
}
