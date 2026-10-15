package org.jmouse.parallax.script.engine;

import java.util.List;
import java.util.Optional;

/**
 * What a definition contributes to its document: the kind word that opens it, the engine family in {@code type},
 * the constants it keeps in {@code parameters}, the fields it keeps in {@code meta}, and its own blocks.
 *
 * @param constants  parameter lines without a range — {@code bailout = 2}, {@code dimension = 4 integer}: values of
 *                   the definition, not sliders
 * @param metaFields meta lines the definition owns, such as a Markov chain's {@code states}
 * @param blocks     the definition's blocks, in the order they are written
 */
record WrittenDefinition(String kind, Optional<String> type, List<String> constants, List<String> metaFields,
                         List<TextBlock> blocks) {

    WrittenDefinition {
        constants = List.copyOf(constants);
        metaFields = List.copyOf(metaFields);
        blocks = List.copyOf(blocks);
    }
}
