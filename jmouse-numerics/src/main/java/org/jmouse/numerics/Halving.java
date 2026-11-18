package org.jmouse.numerics;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;

/**
 * Where a test that holds on one side of a value and fails on the other changes, found by halving: how far a track reaches before
 * it meets copper, the widest rounding its corners keep their gaps at, where copper's edge stands between a point on it and one
 * off it. Every search halves the same way, so one does it; how many halvings is the caller's to say — each halves the
 * uncertainty, so it is a precision, never a guess.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class Halving {

    /** Where the search ended: the last value the test held at, and the last it failed at — the change lies between them. */
    public record Bracket(double holds, double fails) {

        /** The middle of what is left: the change itself, as near as the halvings found it. */
        public double middle() {
            return (holds + fails) / 2;
        }
    }

    private Halving() {
    }

    /**
     * Halved {@code halvings} times between a value the test holds at and one it fails at: the change, bracketed. The test is
     * not asked at either end, which the caller vouches for.
     */
    public static Bracket bracket(double holds, double fails, int halvings, DoublePredicate test) {
        double held = holds;
        double failed = fails;
        for (int halving = 0; halving < halvings; halving++) {
            double middle = (held + failed) / 2;
            if (test.test(middle)) {
                held = middle;
            } else {
                failed = middle;
            }
        }
        return new Bracket(held, failed);
    }

    /**
     * What is made at the farthest value toward {@code fails} that anything can be made at, halving from {@code holds}: the thing
     * made there; empty where nothing could be made at any value tried.
     */
    public static <Made> Optional<Made> farthestMade(double holds, double fails, int halvings, DoubleFunction<Optional<Made>> make) {
        double held = holds;
        double failed = fails;
        Optional<Made> best = Optional.empty();
        for (int halving = 0; halving < halvings; halving++) {
            double middle = (held + failed) / 2;
            Optional<Made> made = make.apply(middle);
            if (made.isPresent()) {
                best = made;
                held = middle;
            } else {
                failed = middle;
            }
        }
        return best;
    }

    /**
     * The first share of the whole way — 1, then half, a quarter … — the test holds at, {@code halvings} halvings at most: as far
     * toward a target as can be gone, coming back toward the start; empty where none of them holds.
     */
    public static OptionalDouble firstHeldShare(int halvings, DoublePredicate test) {
        double share = 1;
        for (int halving = 0; halving <= halvings; halving++) {
            if (test.test(share)) {
                return OptionalDouble.of(share);
            }
            share /= 2;
        }
        return OptionalDouble.empty();
    }
}
