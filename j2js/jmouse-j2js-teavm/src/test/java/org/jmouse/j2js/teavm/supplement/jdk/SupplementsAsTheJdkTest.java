package org.jmouse.j2js.teavm.supplement.jdk;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Each supplement answers as the JDK method it stands in, side by side — the generic and the method-handle ones as documented. */
class SupplementsAsTheJdkTest {

    record Point(int x) {
    }

    @ParameterizedTest
    @ValueSource(classes = {String.class, Map.Entry.class, int.class, String[][].class, int[].class, SupplementsAsTheJdkTest.class, Point.class})
    void aClassIsAnsweredAsTheJdkAnswers(Class<?> type) {
        assertEquals(type.getPackageName(), ClassSupplements.getPackageName(type));
        assertEquals(type.componentType(), ClassSupplements.componentType(type));
        assertEquals(type.isRecord(), ClassSupplements.isRecord(type));
    }

    @Test
    void aClassDeclaredWithoutTypeArgumentsHasItsRawSupertypesForGenericOnes() {
        assertEquals(Number.class.getGenericSuperclass(), ClassSupplements.getGenericSuperclass(Number.class));
        assertEquals(Arrays.asList(Number.class.getGenericInterfaces()), Arrays.asList(ClassSupplements.getGenericInterfaces(Number.class)));
        assertEquals(List.of(Serializable.class), Arrays.asList(ClassSupplements.getGenericInterfaces(Number.class)));
    }

    @ParameterizedTest
    @ValueSource(ints = {'a', 0x263A, 0x1F600, 0})
    void aCodePointIsItsText(int codePoint) {
        assertEquals(Character.toString(codePoint), CharacterSupplements.toString(codePoint));
    }

    @Test
    void aDateAndACalendarCrossToJavaTimeAsTheJdks() {
        Instant  instant  = Instant.ofEpochMilli(1_700_000_000_123L);
        Date     date     = Date.from(instant);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        assertEquals(Date.from(instant), DateSupplements.from(instant));
        assertEquals(date.toInstant(), DateSupplements.toInstant(date));
        assertEquals(calendar.toInstant(), CalendarSupplements.toInstant(calendar));
    }

    @Test
    void aZonedDateTimeIsACalendarWithIsoWeeks() {
        ZonedDateTime     dateTime   = ZonedDateTime.of(2026, 11, 17, 10, 30, 0, 0, ZoneId.of("Europe/Kyiv"));
        GregorianCalendar jdk        = GregorianCalendar.from(dateTime);
        GregorianCalendar supplement = GregorianCalendarSupplements.from(dateTime);

        assertEquals(jdk.getTimeInMillis(), supplement.getTimeInMillis());
        assertEquals(jdk.getTimeZone().getID(), supplement.getTimeZone().getID());
        assertEquals(jdk.getFirstDayOfWeek(), supplement.getFirstDayOfWeek());
        assertEquals(jdk.getMinimalDaysInFirstWeek(), supplement.getMinimalDaysInFirstWeek());
        assertEquals(jdk.get(Calendar.WEEK_OF_YEAR), supplement.get(Calendar.WEEK_OF_YEAR));
    }

    @Test
    void aCharsetFallsBackWhereNoneIsByItsNameAndAnIllegalNameIsStillRefused() {
        assertEquals(Charset.forName("UTF-8", StandardCharsets.US_ASCII), CharsetSupplements.forName("UTF-8", StandardCharsets.US_ASCII));
        assertEquals(Charset.forName("x-no-such", StandardCharsets.US_ASCII), CharsetSupplements.forName("x-no-such", StandardCharsets.US_ASCII));
        assertThrows(IllegalCharsetNameException.class, () -> CharsetSupplements.forName("bad name", StandardCharsets.US_ASCII));
    }

    @Test
    void aMethodHandleIsRefusedForAPageSpinsNone() {
        UnsupportedOperationException refused = assertThrows(UnsupportedOperationException.class, MethodHandlesSupplements::lookup);

        assertEquals("A page compiles no method handle: TeaVM spins no call at run time", refused.getMessage());
        assertThrows(UnsupportedOperationException.class, () -> MethodHandlesSupplements.privateLookupIn(String.class, MethodHandles.lookup()));
        assertThrows(UnsupportedOperationException.class, () -> MethodTypeSupplements.methodType(Object.class, new Class<?>[0]));
    }
}
