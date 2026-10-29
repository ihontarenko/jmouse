package org.jmouse.cad.simulation.engine;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** What a measurement found: a number, or a row of them — digital levels, the figures of a display. */
public sealed interface Measured {

    /** One number, in SI. */
    record Value(double value) implements Measured {

        @Override
        public String written() {
            return String.format(Locale.ROOT, "%.5g", value);
        }
    }

    /** A row, each item a number or a level that is no number — {@code X}, {@code Z} —, in the order read. */
    record Row(List<Object> items) implements Measured {

        public Row {
            items = List.copyOf(items);
        }

        @Override
        public String written() {
            return items.stream().map(Row::item).collect(Collectors.joining(", ", "[", "]"));
        }

        private static String item(Object item) {
            if (item instanceof Number number && number.doubleValue() == Math.rint(number.doubleValue())) {
                return Long.toString(number.longValue());
            }
            return String.valueOf(item);
        }
    }

    /** As a verdict shows it. */
    String written();
}
