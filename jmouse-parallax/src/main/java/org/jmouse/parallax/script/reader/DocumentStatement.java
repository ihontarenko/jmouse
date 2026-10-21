package org.jmouse.parallax.script.reader;

import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.Meta;

import java.util.List;

/** One statement of a document's braces, as its parser read it; the document parser gathers them into the document. */
sealed interface DocumentStatement {

    /** {@code extends base-slug} */
    record Base(String slug) implements DocumentStatement {
    }

    /** {@code units A, B} */
    record Units(List<String> names) implements DocumentStatement {
    }

    /** {@code use library, other} */
    record Uses(List<String> slugs) implements DocumentStatement {
    }

    /** {@code meta { … }} */
    record MetaBlock(Meta meta) implements DocumentStatement {
    }

    /** Any other block: {@code parameters { … }}, {@code parts { … }}, {@code behaviour { … }}. */
    record BlockStatement(Block block) implements DocumentStatement {
    }
}
