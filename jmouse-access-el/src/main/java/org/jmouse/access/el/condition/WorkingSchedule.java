package org.jmouse.access.el.condition;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * When an installation is open — the days of the week, and the hours within them.
 *
 * <h2>How it is written</h2>
 *
 * <pre>{@code
 * mon-fri 09:00-18:00      a range of days
 * mon,wed,fri 10:00-16:00  a list of days
 * sat-sun 00:00-24:00      a weekend, all day
 * }</pre>
 *
 * <p>Days are the three-letter English abbreviations — {@code mon tue wed thu fri sat sun} — which is
 * the one place here where the short form is the name of the thing rather than a truncation of it.
 * Times are 24-hour {@code HH:mm}.
 *
 * <h2>⚠️ The end of the range is exclusive, and {@code 24:00} is how midnight is said</h2>
 *
 * <p>{@code 09:00-18:00} means <em>up to but not including</em> 18:00, so a rule guarding it refuses at
 * 18:00 exactly. That is what somebody means by "we close at six".
 *
 * <p>{@link LocalTime} cannot hold 24:00, so a schedule is kept as two minute-of-day counts rather than
 * as two {@code LocalTime}s. Writing {@code 23:59} instead would leave one minute of every day outside
 * every schedule — exactly the kind of gap nobody finds by reading.
 *
 * <h2>⚠️ It does not know about holidays</h2>
 *
 * <p>A working-hours rule written against this will let somebody write on Christmas Day. That is a
 * deliberate first cut — the alternative is a holiday calendar per country, which is a product feature
 * and not an authorization one — but it is a fact to know rather than to discover.
 *
 * <p>If it is ever wanted, the shape is a calendar port the product implements, consulted beside this.
 * It is not a longer parse format.
 */
public record WorkingSchedule(Set<DayOfWeek> days, int fromMinuteOfDay, int toMinuteOfDay) {

    private static final int MINUTES_IN_A_DAY = 24 * 60;

    private static final String SPELLING =
            "write it as 'mon-fri 09:00-18:00', 'mon,wed,fri 10:00-16:00' or 'sat-sun 00:00-24:00' — "
            + "days as mon tue wed thu fri sat sun, times as 24-hour HH:mm";

    public WorkingSchedule {
        if (days == null || days.isEmpty()) {
            throw new IllegalArgumentException(
                    "a working schedule with no days is a schedule nothing is ever inside, so it would "
                    + "refuse everybody forever. " + SPELLING);
        }

        if (fromMinuteOfDay >= toMinuteOfDay) {
            throw new IllegalArgumentException(
                    ("a working day cannot end before it starts, and this one runs from %s to %s. A "
                     + "schedule crossing midnight is two schedules — say so as two rules. " + SPELLING)
                            .formatted(asTime(fromMinuteOfDay), asTime(toMinuteOfDay)));
        }

        days = EnumSet.copyOf(days);
    }

    /** Monday to Friday, nine to six — what an installation that has said nothing else means. */
    public static WorkingSchedule standard() {
        return parse("mon-fri 09:00-18:00");
    }

    /** Whether the given moment falls inside this schedule. */
    public boolean covers(ZonedDateTime moment) {
        if (!days.contains(moment.getDayOfWeek())) {
            return false;
        }

        int minuteOfDay = moment.getHour() * 60 + moment.getMinute();

        return minuteOfDay >= fromMinuteOfDay && minuteOfDay < toMinuteOfDay;
    }

    /** The schedule as somebody would say it out loud — for a refusal, and for an administration screen. */
    public String describe() {
        StringBuilder written = new StringBuilder();

        for (DayOfWeek day : days) {
            written.append(written.isEmpty() ? "" : ",").append(abbreviate(day));
        }

        return "%s %s-%s".formatted(written, asTime(fromMinuteOfDay), asTime(toMinuteOfDay));
    }

    /**
     * The schedule a policy file or a configuration property wrote, or a refusal naming the spelling
     * that would have worked.
     */
    public static WorkingSchedule parse(String written) {
        if (written == null || written.isBlank()) {
            throw new IllegalArgumentException("a working schedule cannot be empty — " + SPELLING);
        }

        String[] halves = written.trim().split("\\s+");

        if (halves.length != 2) {
            throw new IllegalArgumentException(
                    ("'%s' is not a working schedule: it needs days and hours, separated by a space — "
                     + SPELLING).formatted(written));
        }

        String[] hours = halves[1].split("-", -1);

        if (hours.length != 2) {
            throw new IllegalArgumentException(
                    ("'%s' in '%s' is not a range of hours — " + SPELLING).formatted(halves[1], written));
        }

        return new WorkingSchedule(
                daysIn(halves[0], written),
                minuteOfDay(hours[0], written),
                minuteOfDay(hours[1], written));
    }

    private static Set<DayOfWeek> daysIn(String written, String whole) {
        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);

        for (String part : written.split(",")) {
            if (part.contains("-")) {
                addRange(days, part, whole);
                continue;
            }

            days.add(day(part, whole));
        }

        return days;
    }

    private static void addRange(Set<DayOfWeek> days, String written, String whole) {
        String[] ends = written.split("-", -1);

        if (ends.length != 2) {
            throw new IllegalArgumentException(
                    ("'%s' in '%s' is not a range of days — " + SPELLING).formatted(written, whole));
        }

        DayOfWeek last    = day(ends[1], whole);
        DayOfWeek current = day(ends[0], whole);

        // Inclusive at both ends, and it wraps — `fri-mon` is Friday through Monday rather than an
        // error. The do-while is what makes `mon-mon` mean Monday instead of every day of the week.
        do {
            days.add(current);
            current = current.plus(1);
        } while (current != last.plus(1));
    }

    private static DayOfWeek day(String written, String whole) {
        String trimmed = written.trim().toLowerCase(Locale.ROOT);

        for (DayOfWeek day : DayOfWeek.values()) {
            if (abbreviate(day).equals(trimmed)) {
                return day;
            }
        }

        throw new IllegalArgumentException(
                ("'%s' in '%s' is not a day — " + SPELLING).formatted(written, whole));
    }

    private static int minuteOfDay(String written, String whole) {
        String[] parts = written.trim().split(":");

        if (parts.length != 2) {
            throw new IllegalArgumentException(
                    ("'%s' in '%s' is not a time — " + SPELLING).formatted(written, whole));
        }

        int total;

        try {
            total = Integer.parseInt(parts[0].trim()) * 60 + Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException unreadable) {
            throw new IllegalArgumentException(
                    ("'%s' in '%s' is not a time — " + SPELLING).formatted(written, whole), unreadable);
        }

        if (total < 0 || total > MINUTES_IN_A_DAY || Integer.parseInt(parts[1].trim()) > 59) {
            throw new IllegalArgumentException(
                    ("'%s' in '%s' is not a time of day — " + SPELLING).formatted(written, whole));
        }

        return total;
    }

    private static String abbreviate(DayOfWeek day) {
        return day.name().substring(0, 3).toLowerCase(Locale.ROOT);
    }

    private static String asTime(int minuteOfDay) {
        return "%02d:%02d".formatted(minuteOfDay / 60, minuteOfDay % 60);
    }
}
