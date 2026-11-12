package org.jmouse.libre.pcb;

import org.jmouse.sexpression.node.SExpressionList;

/** Where a wire of a sheet or a trace of a board ends, whatever that is. */
public sealed interface LibrePcbAnchor {

    /** A pin of a symbol on a sheet. */
    record OnPin(String symbol, String pin) implements LibrePcbAnchor {
    }

    /** A pad of a device on a board. */
    record OnPad(String device, String pad) implements LibrePcbAnchor {
    }

    /** A point where a segment's lines meet. */
    record OnJunction(String junction) implements LibrePcbAnchor {
    }

    /** A via of a board. */
    record OnVia(String via) implements LibrePcbAnchor {
    }

    /** {@code (from (symbol …) (pin …))}, {@code (to (junction …))}. */
    static LibrePcbAnchor of(SExpressionList end) {
        if (end.list(LibrePcbKeyword.SYMBOL).isPresent()) {
            return new OnPin(LibrePcbValues.text(end, LibrePcbKeyword.SYMBOL), LibrePcbValues.text(end, LibrePcbKeyword.PIN));
        }
        if (end.list(LibrePcbKeyword.DEVICE).isPresent()) {
            return new OnPad(LibrePcbValues.text(end, LibrePcbKeyword.DEVICE), LibrePcbValues.text(end, LibrePcbKeyword.PAD));
        }
        if (end.list(LibrePcbKeyword.JUNCTION).isPresent()) {
            return new OnJunction(LibrePcbValues.text(end, LibrePcbKeyword.JUNCTION));
        }
        if (end.list(LibrePcbKeyword.VIA).isPresent()) {
            return new OnVia(LibrePcbValues.text(end, LibrePcbKeyword.VIA));
        }
        throw new LibrePcbReadException("(" + end.name() + ") at " + end.position() + " ends on nothing this reader knows.");
    }
}
