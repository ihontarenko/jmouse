package org.jmouse.j2js.teavm.classlib.java.net;

/**
 * {@code java.net.Inet6Address} in the browser: sixteen bytes, read from a literal with {@code ::} and a dotted IPv4 tail, and
 * written as the JDK writes them — eight groups of hexadecimal, none shortened ({@code 0:0:0:0:0:0:0:1}). A scope
 * ({@code %eth0}) names an interface a page has none of, so a literal with one writes no address here.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class TInet6Address extends TInetAddress {

    private static final int BYTES  = 16;
    private static final int GROUPS = 8;

    TInet6Address(String hostName, byte[] address) {
        super(hostName, address);
    }

    /** The sixteen bytes a literal writes; null for one that writes no IPv6 address. */
    static byte[] parse(String literal) {
        if (literal.indexOf('%') >= 0) {
            return null;
        }

        int gap = literal.indexOf("::");

        if (gap >= 0 && literal.indexOf("::", gap + 1) >= 0) {
            return null;
        }

        String   head       = gap >= 0 ? literal.substring(0, gap) : literal;
        String   tail       = gap >= 0 ? literal.substring(gap + 2) : "";
        int[]    headGroups = groups(head);
        int[]    tailGroups = groups(tail);

        if (headGroups == null || tailGroups == null) {
            return null;
        }

        int written = headGroups.length + tailGroups.length;

        if (gap < 0 ? written != GROUPS : written >= GROUPS) {
            return null;
        }

        byte[] address = new byte[BYTES];

        for (int index = 0; index < headGroups.length; index++) {
            put(address, index, headGroups[index]);
        }

        for (int index = 0; index < tailGroups.length; index++) {
            put(address, GROUPS - tailGroups.length + index, tailGroups[index]);
        }

        return address;
    }

    /** The groups of one side of {@code ::}: hexadecimal groups, the last of the literal maybe a dotted IPv4 address. */
    private static int[] groups(String side) {
        if (side.isEmpty()) {
            return new int[0];
        }

        String[] parts  = side.split(":", -1);
        boolean  dotted = parts[parts.length - 1].indexOf('.') >= 0;
        int[]    groups = new int[parts.length + (dotted ? 1 : 0)];

        for (int index = 0; index < parts.length; index++) {
            String part = parts[index];

            if (dotted && index == parts.length - 1) {
                byte[] ipv4 = part.chars().filter(character -> character == '.').count() == 3 ? TInet4Address.parse(part) : null;
                if (ipv4 == null) {
                    return null;
                }
                groups[index] = ((ipv4[0] & 0xFF) << 8) | (ipv4[1] & 0xFF);
                groups[index + 1] = ((ipv4[2] & 0xFF) << 8) | (ipv4[3] & 0xFF);
                continue;
            }

            if (part.isEmpty() || part.length() > 4) {
                return null;
            }

            int value = 0;
            for (int position = 0; position < part.length(); position++) {
                int digit = Character.digit(part.charAt(position), 16);
                if (digit < 0) {
                    return null;
                }
                value = (value << 4) | digit;
            }
            groups[index] = value;
        }

        return groups;
    }

    private static void put(byte[] address, int group, int value) {
        address[group * 2] = (byte) (value >> 8);
        address[group * 2 + 1] = (byte) value;
    }

    @Override
    public String getHostAddress() {
        byte[]        address = bytes();
        StringBuilder written = new StringBuilder();

        for (int group = 0; group < GROUPS; group++) {
            if (group > 0) {
                written.append(':');
            }
            written.append(Integer.toHexString(((address[group * 2] & 0xFF) << 8) | (address[group * 2 + 1] & 0xFF)));
        }

        return written.toString();
    }

    @Override
    public boolean isLoopbackAddress() {
        byte[] address = bytes();
        for (int index = 0; index < BYTES - 1; index++) {
            if (address[index] != 0) {
                return false;
            }
        }
        return address[BYTES - 1] == 1;
    }

    @Override
    public boolean isLinkLocalAddress() {
        return (bytes()[0] & 0xFF) == 0xFE && (bytes()[1] & 0xC0) == 0x80;
    }

    @Override
    public boolean isSiteLocalAddress() {
        return (bytes()[0] & 0xFF) == 0xFE && (bytes()[1] & 0xC0) == 0xC0;
    }

    @Override
    public boolean isMulticastAddress() {
        return (bytes()[0] & 0xFF) == 0xFF;
    }

    /** Whether the address is an IPv4 address in the old compatible form, {@code ::a.b.c.d}. */
    public boolean isIPv4CompatibleAddress() {
        byte[] address = bytes();
        for (int index = 0; index < 12; index++) {
            if (address[index] != 0) {
                return false;
            }
        }
        return true;
    }
}
