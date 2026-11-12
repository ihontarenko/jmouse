package org.jmouse.licence;

import org.jmouse.licence.reading.LicenceExpressionReader;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An SPDX licence expression: one licence, perhaps with an exception — {@code CC-BY-SA-4.0 WITH KiCad-libraries-exception} —, or
 * several, all of whose terms hold ({@code AND}) or any one of which may be chosen ({@code OR}).
 */
public sealed interface LicenceExpression {

    /** What SPDX writes after a licence the work may also be taken under any later version of: {@code GPL-2.0+}. */
    String OR_LATER = "+";

    /**
     * The expression a text writes.
     *
     * @throws IllegalArgumentException for a text that is not an SPDX expression, saying where
     */
    static LicenceExpression read(String text) {
        return LicenceExpressionReader.read(text);
    }

    /** The expression as SPDX writes it. */
    String written();

    /** Every licence and exception it names, each once, in the order written. */
    default List<String> identifiers() {
        return switch (this) {
            case Single single -> Stream.concat(Stream.of(single.licence()), single.exception().stream()).toList();
            case AllOf all -> all.parts().stream().flatMap(part -> part.identifiers().stream()).distinct().toList();
            case AnyOf any -> any.parts().stream().flatMap(part -> part.identifiers().stream()).distinct().toList();
        };
    }

    /**
     * One licence.
     *
     * @param licence   its SPDX identifier, without {@code +}
     * @param orLater   whether any later version of it may be chosen too
     * @param exception the exception it is taken with; empty for none
     */
    record Single(String licence, boolean orLater, Optional<String> exception) implements LicenceExpression {

        @Override
        public String written() {
            return licence + (orLater ? OR_LATER : "") + exception.map(named -> " WITH " + named).orElse("");
        }
    }

    /** Every one of them at once: the work owes each one's conditions. */
    record AllOf(List<LicenceExpression> parts) implements LicenceExpression {

        public AllOf {
            parts = List.copyOf(parts);
        }

        @Override
        public String written() {
            return parts.stream().map(LicenceExpression::grouped).collect(Collectors.joining(" AND "));
        }
    }

    /** Any one of them, as whoever takes the work chooses. */
    record AnyOf(List<LicenceExpression> parts) implements LicenceExpression {

        public AnyOf {
            parts = List.copyOf(parts);
        }

        @Override
        public String written() {
            return parts.stream().map(LicenceExpression::grouped).collect(Collectors.joining(" OR "));
        }
    }

    /** A part as it is written inside another: in brackets where it is a group itself. */
    private static String grouped(LicenceExpression part) {
        return part instanceof Single ? part.written() : "(" + part.written() + ")";
    }
}
