package org.jmouse.j2js.teavm.classlib;

import org.jmouse.j2js.teavm.classlib.java.lang.reflect.TParameter;
import org.jmouse.j2js.teavm.classlib.java.lang.reflect.TRecordComponent;
import org.jmouse.j2js.teavm.classlib.java.net.TInetAddress;
import org.jmouse.j2js.teavm.classlib.java.net.TUnknownHostException;
import org.jmouse.j2js.teavm.classlib.java.util.TStringJoiner;
import org.jmouse.j2js.teavm.classlib.java.util.concurrent.TConcurrentHashMap$KeySetView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.RecordComponent;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Each substitute answers as the JDK class it stands in, side by side. */
class SubstitutesAsTheJdkTest {

    @Test
    void aStringJoinerJoinsAsTheJdks() {
        StringJoiner  jdk        = new StringJoiner(", ", "[", "]").setEmptyValue("none");
        TStringJoiner substitute = new TStringJoiner(", ", "[", "]").setEmptyValue("none");

        assertEquals(jdk.toString(), substitute.toString());
        assertEquals(jdk.length(), substitute.length());

        jdk.add("a").add("b");
        substitute.add("a").add("b");

        assertEquals(jdk.toString(), substitute.toString());
        assertEquals(jdk.length(), substitute.length());

        jdk.merge(new StringJoiner("-", "<", ">").add("c").add("d"));
        substitute.merge(new TStringJoiner("-", "<", ">").add("c").add("d"));

        assertEquals(jdk.toString(), substitute.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"192.168.0.1", "127.1", "10.258", "::1", "fe80::1:2", "2001:db8::ff00:42:8329", "::ffff:10.0.0.1",
            "::10.0.0.1", "[::1]", "1:2:3:4:5:6:7:8", "0.0.0.0"})
    void anAddressLiteralIsReadAsTheJdkReadsIt(String literal) throws Exception {
        InetAddress  jdk        = InetAddress.getByName(literal);
        TInetAddress substitute = TInetAddress.getByName(literal);

        assertArrayEquals(jdk.getAddress(), substitute.getAddress(), literal);
        assertEquals(jdk.getHostAddress(), substitute.getHostAddress(), literal);
        assertEquals(jdk.toString(), substitute.toString(), literal);
        assertEquals(jdk.getClass().getSimpleName(), substitute.getClass().getSimpleName().substring(1), literal);
        assertEquals(jdk.isLoopbackAddress(), substitute.isLoopbackAddress(), literal);
        assertEquals(jdk.isSiteLocalAddress(), substitute.isSiteLocalAddress(), literal);
        assertEquals(jdk.isLinkLocalAddress(), substitute.isLinkLocalAddress(), literal);
    }

    @ParameterizedTest
    @ValueSource(strings = {"256.1.1.1", "1.2.3.4.5", "1::2::3", "1:2:3:4:5:6:7:8:9", "12345::", "1.2.3."})
    void aBrokenLiteralIsNoAddressThereAsHere(String literal) {
        assertThrows(UnknownHostException.class, () -> InetAddress.getByName(literal), literal);
        assertThrows(TUnknownHostException.class, () -> TInetAddress.getByName(literal), literal);
    }

    @Test
    void anAddressOfBytesIsTheJdks() throws Exception {
        byte[] mapped = new byte[16];
        mapped[10] = (byte) 0xFF;
        mapped[11] = (byte) 0xFF;
        mapped[12] = 10;
        mapped[15] = 1;

        assertEquals(InetAddress.getByAddress(mapped).getHostAddress(), TInetAddress.getByAddress(mapped).getHostAddress());
        assertThrows(TUnknownHostException.class, () -> TInetAddress.getByAddress(new byte[3]));
    }

    @Test
    void aNameAPageCannotResolveIsRefused() {
        assertThrows(TUnknownHostException.class, () -> TInetAddress.getByName("example.com"));
    }

    @Test
    void aSetOfKeysIsTheMapsAsTheJdks() {
        ConcurrentHashMap.KeySetView<String, Boolean> jdk        = ConcurrentHashMap.newKeySet();
        TConcurrentHashMap$KeySetView<String, Boolean> substitute = TConcurrentHashMap$KeySetView.newKeySet();

        assertEquals(jdk.add("a"), substitute.add("a"));
        assertEquals(jdk.add("a"), substitute.add("a"));
        assertEquals(jdk.contains("a"), substitute.contains("a"));
        assertEquals(jdk.getMappedValue(), substitute.getMappedValue());
        assertEquals(jdk.remove("a"), substitute.remove("a"));
        assertEquals(jdk.size(), substitute.size());

        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        assertThrows(UnsupportedOperationException.class, () -> TConcurrentHashMap$KeySetView.keySet(map).add("x"));
        assertTrue(TConcurrentHashMap$KeySetView.keySet(map, 7).add("x"));
        assertEquals(7, map.get("x"));
    }

    record Point(double x, String label) {
        static final int ORIGIN = 0;
    }

    @Test
    void aRecordsComponentsAreItsFieldsAsTheJdkHasThem() throws Exception {
        RecordComponent[]  jdk        = Point.class.getRecordComponents();
        TRecordComponent[] substitute = TRecordComponent.getRecordComponents(Point.class);

        assertEquals(jdk.length, substitute.length);
        for (int index = 0; index < jdk.length; index++) {
            assertEquals(jdk[index].getName(), substitute[index].getName());
            assertEquals(jdk[index].getType(), substitute[index].getType());
            assertEquals(jdk[index].getAccessor(), substitute[index].getAccessor());
        }
        assertEquals(null, TRecordComponent.getRecordComponents(String.class));
    }

    @SuppressWarnings("unused")
    public void twoParameters(String first, int... rest) {
    }

    @Test
    void aParameterIsNamedAsTheJdkNamesOneWhoseNameIsNotKept() throws Exception {
        Method       method     = getClass().getMethod("twoParameters", String.class, int[].class);
        Parameter[]  jdk        = method.getParameters();
        TParameter[] substitute = TParameter.getParameters(method);

        assertEquals(jdk.length, substitute.length);
        assertEquals("arg0", substitute[0].getName());
        assertFalse(substitute[0].isNamePresent());
        assertEquals(Arrays.stream(jdk).map(Parameter::getType).toList(), Arrays.stream(substitute).map(TParameter::getType).toList());
        assertEquals(Arrays.stream(jdk).map(Parameter::isVarArgs).toList(), Arrays.stream(substitute).map(TParameter::isVarArgs).toList());
    }
}
