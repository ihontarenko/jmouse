/**
 * The second entry point: the same tools, reached by a model instead of by hand.
 *
 * <p>What it is here to show is that <strong>nothing changes</strong>. The dispatcher is the same
 * object, the guards are the same guards, and a call refused by hand is refused identically here, in
 * the identical words — because the words are written once in {@code jmouse-ai} and every transport
 * reads them rather than writing their own.
 *
 * <p>The model is scripted, not real. A real one is not reproducible, needs a key, and cannot be made
 * to ask for the awkward thing on purpose — two calls in one turn, a call that will be refused, a turn
 * that never ends. The script is also what makes the loop testable in ticket 13 without a network.
 */
package org.jmouse.ai.sandbox.conversation;
