package org.jmouse.j2js.teavm.classlib.java.net;

/**
 * {@code java.net.Inet4Address} in the browser: four bytes, written {@code 192.168.0.1}, and read as the JDK reads a literal —
 * one to four decimal parts, the last filling the bytes left ({@code 127.1} is {@code 127.0.0.1}).
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class TInet4Address extends TInetAddress {

    private static final int BYTES = 4;

    TInet4Address(String hostName, byte[] address) {
        super(hostName, address);
    }

    /** The four bytes a literal writes; null for one that writes no IPv4 address. */
    static byte[] parse(String literal) {
        if (literal.isEmpty() || literal.endsWith(".")) {
            return null;
        }

        String[] parts = literal.split("\\.", -1);

        if (parts.length > BYTES) {
            return null;
        }

        long[] values = new long[parts.length];

        for (int index = 0; index < parts.length; index++) {
            String part = parts[index];
            if (part.isEmpty() || part.length() > 10 || !part.chars().allMatch(Character::isDigit)) {
                return null;
            }
            values[index] = Long.parseLong(part);
        }

        byte[] address = new byte[BYTES];

        // Every part but the last is one byte; the last fills the bytes left.
        for (int index = 0; index < parts.length - 1; index++) {
            if (values[index] > 0xFF) {
                return null;
            }
            address[index] = (byte) values[index];
        }

        int  left = BYTES - (parts.length - 1);
        long last = values[parts.length - 1];

        if (last >= (1L << (8 * left))) {
            return null;
        }

        for (int index = BYTES - 1; index >= parts.length - 1; index--) {
            address[index] = (byte) (last & 0xFF);
            last >>= 8;
        }

        return address;
    }

    @Override
    public String getHostAddress() {
        byte[] address = bytes();
        return (address[0] & 0xFF) + "." + (address[1] & 0xFF) + "." + (address[2] & 0xFF) + "." + (address[3] & 0xFF);
    }

    @Override
    public boolean isLoopbackAddress() {
        return bytes()[0] == 127;
    }

    @Override
    public boolean isLinkLocalAddress() {
        return (bytes()[0] & 0xFF) == 169 && (bytes()[1] & 0xFF) == 254;
    }

    @Override
    public boolean isSiteLocalAddress() {
        int first  = bytes()[0] & 0xFF;
        int second = bytes()[1] & 0xFF;
        return first == 10 || (first == 172 && (second & 0xF0) == 16) || (first == 192 && second == 168);
    }

    @Override
    public boolean isMulticastAddress() {
        return (bytes()[0] & 0xF0) == 0xE0;
    }
}
