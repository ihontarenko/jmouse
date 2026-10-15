package org.jmouse.scheduler;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
import java.util.BitSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A parsed cron expression, evaluated against wall-clock time in an explicit zone.
 *
 * <h2>Grammar</h2>
 *
 * <p>Five fields — {@code minute hour day-of-month month day-of-week} — or six with a leading
 * {@code second}. Each field is a comma-separated list of {@code *}, {@code n}, {@code a-b},
 * {@code *}{@code /step}, {@code a/step} or {@code a-b/step}. Months accept {@code JAN}–{@code DEC},
 * days of the week {@code SUN}–{@code SAT} as well as {@code 0}–{@code 7} (both {@code 0} and
 * {@code 7} are Sunday). {@code ?} is accepted in either day field and means {@code *}. The
 * day-of-month field also accepts {@code L}, the last day of the month. The macros {@code @yearly},
 * {@code @annually}, {@code @monthly}, {@code @weekly}, {@code @daily}, {@code @midnight} and
 * {@code @hourly} are accepted. Anything else — {@code W}, {@code #}, {@code L} in the day-of-week
 * field, years — is refused at parse time rather than silently ignored.
 *
 * <h2>Day matching</h2>
 *
 * <p>When <em>both</em> day fields are restricted, a day matches if <em>either</em> does — the classic
 * Vixie cron rule, so {@code 0 0 13 * FRI} fires on every 13th and on every Friday.
 *
 * <h2>Daylight-saving time</h2>
 *
 * <ul>
 *     <li><strong>Gap</strong> (clocks jump forward): a wall-clock time that does not exist fires
 *     once, at the instant the clocks jump. Every slot inside one gap collapses into that single
 *     firing, so an every-fifteen-minutes expression does not fire four times at once.</li>
 *     <li><strong>Overlap</strong> (clocks fall back): a wall-clock time that occurs twice fires once,
 *     at its earlier occurrence.</li>
 * </ul>
 *
 * <p>Immutable and thread-safe. Equality is by the normalised field sets, so two spellings of the
 * same schedule are equal.
 */
public final class CronExpression {

    /** How far ahead {@link #nextAfter} searches before concluding the expression never fires. */
    static final int SEARCH_HORIZON_YEARS = 10;

    private static final Map<String, String> MACROS = Map.of(
            "@yearly", "0 0 1 1 *",
            "@annually", "0 0 1 1 *",
            "@monthly", "0 0 1 * *",
            "@weekly", "0 0 * * 0",
            "@daily", "0 0 * * *",
            "@midnight", "0 0 * * *",
            "@hourly", "0 * * * *");

    private static final List<String> MONTH_NAMES = List.of(
            "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC");

    private static final List<String> DAY_NAMES = List.of("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT");

    private final String source;
    private final BitSet seconds;
    private final BitSet minutes;
    private final BitSet hours;
    private final BitSet daysOfMonth;
    private final BitSet months;
    private final BitSet daysOfWeek;
    private final boolean lastDayOfMonth;
    private final boolean dayOfMonthRestricted;
    private final boolean dayOfWeekRestricted;

    private CronExpression(String source, BitSet seconds, BitSet minutes, BitSet hours, DayOfMonthField dayOfMonth,
                           BitSet months, DayOfWeekField dayOfWeek) {
        this.source = source;
        this.seconds = seconds;
        this.minutes = minutes;
        this.hours = hours;
        this.daysOfMonth = dayOfMonth.days();
        this.lastDayOfMonth = dayOfMonth.lastDay();
        this.dayOfMonthRestricted = dayOfMonth.restricted();
        this.months = months;
        this.daysOfWeek = dayOfWeek.days();
        this.dayOfWeekRestricted = dayOfWeek.restricted();
    }

    /**
     * Parses an expression.
     *
     * @param expression five or six fields, or a macro
     * @return the parsed expression
     * @throws IllegalArgumentException naming the field and the reason when the expression is invalid
     */
    public static CronExpression parse(String expression) {
        Objects.requireNonNull(expression, "expression");
        String trimmed = expression.trim();

        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("A cron expression must not be empty.");
        }

        String expanded = trimmed.startsWith("@") ? expandMacro(trimmed) : trimmed;
        String[] fields = expanded.split("\\s+");

        if (fields.length != 5 && fields.length != 6) {
            throw new IllegalArgumentException(
                    "A cron expression has five or six fields, not " + fields.length + ": '" + trimmed + "'.");
        }

        int offset = fields.length == 6 ? 1 : 0;
        BitSet seconds = offset == 1 ? parseField(fields[0], "second", 0, 59, List.of()) : single(0);
        BitSet minutes = parseField(fields[offset], "minute", 0, 59, List.of());
        BitSet hours = parseField(fields[offset + 1], "hour", 0, 23, List.of());
        DayOfMonthField dayOfMonth = parseDayOfMonth(fields[offset + 2]);
        BitSet months = parseField(fields[offset + 3], "month", 1, 12, MONTH_NAMES);
        DayOfWeekField dayOfWeek = parseDayOfWeek(fields[offset + 4]);

        return new CronExpression(trimmed, seconds, minutes, hours, dayOfMonth, months, dayOfWeek);
    }

    /**
     * The first instant strictly after {@code after} at which this expression fires in {@code zone}.
     *
     * @param after the instant to search from, exclusive
     * @param zone  the zone whose wall clock the fields describe
     * @return the next firing, or empty when there is none within {@value #SEARCH_HORIZON_YEARS} years
     *         (for example {@code 0 0 30 2 *}, which never fires)
     */
    public Optional<Instant> nextAfter(Instant after, ZoneId zone) {
        Objects.requireNonNull(after, "after");
        Objects.requireNonNull(zone, "zone");

        ZoneRules rules = zone.getRules();
        LocalDateTime candidate = LocalDateTime.ofInstant(after, zone).truncatedTo(ChronoUnit.SECONDS).plusSeconds(1);
        int horizonYear = candidate.getYear() + SEARCH_HORIZON_YEARS;

        while (candidate.getYear() <= horizonYear) {
            if (!months.get(candidate.getMonthValue())) {
                candidate = startOfNextMatchingMonth(candidate);
                continue;
            }

            if (!dayMatches(candidate.toLocalDate())) {
                candidate = candidate.toLocalDate().plusDays(1).atStartOfDay();
                continue;
            }

            int hour = hours.nextSetBit(candidate.getHour());

            if (hour < 0) {
                candidate = candidate.toLocalDate().plusDays(1).atStartOfDay();
                continue;
            }

            if (hour != candidate.getHour()) {
                candidate = candidate.toLocalDate().atTime(hour, 0, 0);
            }

            int minute = minutes.nextSetBit(candidate.getMinute());

            if (minute < 0) {
                candidate = candidate.truncatedTo(ChronoUnit.HOURS).plusHours(1);
                continue;
            }

            if (minute != candidate.getMinute()) {
                candidate = candidate.withMinute(minute).withSecond(0);
            }

            int second = seconds.nextSetBit(candidate.getSecond());

            if (second < 0) {
                candidate = candidate.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1);
                continue;
            }

            candidate = candidate.withSecond(second);

            Instant resolved = resolveWallClock(candidate, rules);

            if (resolved.isAfter(after)) {
                return Optional.of(resolved);
            }

            candidate = candidate.plusSeconds(1);
        }

        return Optional.empty();
    }

    /** The expression as it was written, macros unexpanded. */
    public String source() {
        return source;
    }

    private boolean dayMatches(LocalDate date) {
        boolean dayOfMonthMatches = daysOfMonth.get(date.getDayOfMonth())
                || (lastDayOfMonth && date.getDayOfMonth() == date.lengthOfMonth());
        boolean dayOfWeekMatches = daysOfWeek.get(date.getDayOfWeek().getValue() % 7);

        if (dayOfMonthRestricted && dayOfWeekRestricted) {
            return dayOfMonthMatches || dayOfWeekMatches;
        }

        return dayOfMonthMatches && dayOfWeekMatches;
    }

    private LocalDateTime startOfNextMatchingMonth(LocalDateTime candidate) {
        int nextMonth = months.nextSetBit(candidate.getMonthValue() + 1);

        if (nextMonth < 0) {
            return LocalDateTime.of(candidate.getYear() + 1, months.nextSetBit(1), 1, 0, 0);
        }

        return LocalDateTime.of(candidate.getYear(), nextMonth, 1, 0, 0);
    }

    /**
     * Maps a wall-clock time to an instant: the only one, the earlier of two in an overlap, or the
     * moment the clocks jump when the time falls in a gap.
     */
    private static Instant resolveWallClock(LocalDateTime wallClock, ZoneRules rules) {
        List<ZoneOffset> offsets = rules.getValidOffsets(wallClock);

        if (offsets.size() == 1) {
            return wallClock.toInstant(offsets.get(0));
        }

        if (offsets.size() == 2) {
            Instant first = wallClock.toInstant(offsets.get(0));
            Instant second = wallClock.toInstant(offsets.get(1));
            return first.isBefore(second) ? first : second;
        }

        ZoneOffsetTransition transition = rules.getTransition(wallClock);
        return transition.getInstant();
    }

    private static String expandMacro(String macro) {
        String expansion = MACROS.get(macro.toLowerCase(Locale.ROOT));

        if (expansion == null) {
            throw new IllegalArgumentException("Unknown cron macro '" + macro + "'.");
        }

        return expansion;
    }

    private static DayOfMonthField parseDayOfMonth(String field) {
        if (isUnrestricted(field)) {
            return new DayOfMonthField(range(1, 31), false, false);
        }

        BitSet days = new BitSet(32);
        boolean lastDay = false;

        for (String element : field.split(",", -1)) {
            if (element.equalsIgnoreCase("L")) {
                lastDay = true;
            } else {
                days.or(parseElement(element, "day-of-month", 1, 31, List.of()));
            }
        }

        return new DayOfMonthField(days, lastDay, true);
    }

    private static DayOfWeekField parseDayOfWeek(String field) {
        if (isUnrestricted(field)) {
            return new DayOfWeekField(range(0, 6), false);
        }

        BitSet days = parseField(field, "day-of-week", 0, 7, DAY_NAMES);

        if (days.get(7)) {
            days.clear(7);
            days.set(0);
        }

        return new DayOfWeekField(days, true);
    }

    private static boolean isUnrestricted(String field) {
        return field.equals("*") || field.equals("?");
    }

    private static BitSet parseField(String field, String name, int minimum, int maximum, List<String> names) {
        BitSet values = new BitSet(maximum + 1);

        for (String element : field.split(",", -1)) {
            values.or(parseElement(element, name, minimum, maximum, names));
        }

        return values;
    }

    private static BitSet parseElement(String element, String name, int minimum, int maximum, List<String> names) {
        if (element.isEmpty()) {
            throw new IllegalArgumentException("The " + name + " field has an empty list element.");
        }

        String rangePart = element;
        int step = 1;
        int slash = element.indexOf('/');

        if (slash >= 0) {
            rangePart = element.substring(0, slash);
            step = parseNumber(element.substring(slash + 1), name, "step");

            if (step < 1) {
                throw new IllegalArgumentException("The " + name + " field has a step below one: '" + element + "'.");
            }
        }

        int start;
        int end;

        if (rangePart.equals("*") || rangePart.equals("?")) {
            start = minimum;
            end = maximum;
        } else {
            int dash = rangePart.indexOf('-');

            if (dash > 0) {
                start = parseValue(rangePart.substring(0, dash), name, names, minimum);
                end = parseValue(rangePart.substring(dash + 1), name, names, minimum);
            } else {
                start = parseValue(rangePart, name, names, minimum);
                end = slash >= 0 ? maximum : start;
            }
        }

        if (start < minimum || end > maximum || start > end) {
            throw new IllegalArgumentException("The " + name + " field accepts " + minimum + "-" + maximum
                    + "; '" + element + "' is outside it or reversed.");
        }

        BitSet values = new BitSet(maximum + 1);

        for (int value = start; value <= end; value += step) {
            values.set(value);
        }

        return values;
    }

    private static int parseValue(String text, String name, List<String> names, int minimum) {
        int index = names.indexOf(text.toUpperCase(Locale.ROOT));

        if (index >= 0) {
            return index + minimum;
        }

        return parseNumber(text, name, "value");
    }

    private static int parseNumber(String text, String name, String role) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("The " + name + " field has an unsupported " + role + " '" + text + "'.");
        }
    }

    private static BitSet single(int value) {
        BitSet values = new BitSet(value + 1);
        values.set(value);
        return values;
    }

    private static BitSet range(int start, int end) {
        BitSet values = new BitSet(end + 1);
        values.set(start, end + 1);
        return values;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof CronExpression that)) {
            return false;
        }

        return lastDayOfMonth == that.lastDayOfMonth
                && dayOfMonthRestricted == that.dayOfMonthRestricted
                && dayOfWeekRestricted == that.dayOfWeekRestricted
                && seconds.equals(that.seconds)
                && minutes.equals(that.minutes)
                && hours.equals(that.hours)
                && daysOfMonth.equals(that.daysOfMonth)
                && months.equals(that.months)
                && daysOfWeek.equals(that.daysOfWeek);
    }

    @Override
    public int hashCode() {
        return Objects.hash(seconds, minutes, hours, daysOfMonth, months, daysOfWeek, lastDayOfMonth,
                dayOfMonthRestricted, dayOfWeekRestricted);
    }

    @Override
    public String toString() {
        return source;
    }

    private record DayOfMonthField(BitSet days, boolean lastDay, boolean restricted) {
    }

    private record DayOfWeekField(BitSet days, boolean restricted) {
    }
}
