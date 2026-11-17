package org.jmouse.j2js.teavm.classlib.java.net;

import java.util.Arrays;

/**
 * {@code java.net.InetAddress} in the browser: an IPv4 or IPv6 address, read from its literal or its bytes, as the JDK reads
 * them — an IPv4-mapped IPv6 address is an IPv4 one. A page resolves no name: a host that is not an address literal is an
 * {@link TUnknownHostException}.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public abstract class TInetAddress {

    private static final int IPV4_BYTES = 4;
    private static final int IPV6_BYTES = 16;

    private final String hostName;
    private final byte[] address;

    TInetAddress(String hostName, byte[] address) {
        this.hostName = hostName;
        this.address = address.clone();
    }

    /** The address a literal writes; {@code null} or blank is the loopback, as the JDK has it. */
    public static TInetAddress getByName(String host) throws TUnknownHostException {
        if (host == null || host.isEmpty()) {
            return getByAddress(null, new byte[]{127, 0, 0, 1});
        }

        String literal = host.startsWith("[") && host.endsWith("]") ? host.substring(1, host.length() - 1) : host;
        byte[] address = literal.indexOf(':') >= 0 ? TInet6Address.parse(literal) : TInet4Address.parse(literal);

        if (address == null) {
            throw new TUnknownHostException(host + ": a page resolves no host name; an address literal is required");
        }

        return getByAddress(null, address);
    }

    /** Every address of a host: its one literal address. */
    public static TInetAddress[] getAllByName(String host) throws TUnknownHostException {
        return new TInetAddress[]{getByName(host)};
    }

    public static TInetAddress getByAddress(byte[] address) throws TUnknownHostException {
        return getByAddress(null, address);
    }

    /** The address of four or sixteen bytes, named {@code host}; sixteen of an IPv4-mapped address are an IPv4 one. */
    public static TInetAddress getByAddress(String host, byte[] address) throws TUnknownHostException {
        if (address == null) {
            throw new TUnknownHostException("addr is of illegal length");
        }

        if (address.length == IPV6_BYTES && isIpv4Mapped(address)) {
            return new TInet4Address(host, Arrays.copyOfRange(address, 12, IPV6_BYTES));
        }

        return switch (address.length) {
            case IPV4_BYTES -> new TInet4Address(host, address);
            case IPV6_BYTES -> new TInet6Address(host, address);
            default -> throw new TUnknownHostException("addr is of illegal length");
        };
    }

    public static TInetAddress getLoopbackAddress() {
        return new TInet4Address("localhost", new byte[]{127, 0, 0, 1});
    }

    private static boolean isIpv4Mapped(byte[] address) {
        for (int index = 0; index < 10; index++) {
            if (address[index] != 0) {
                return false;
            }
        }
        return (address[10] & 0xFF) == 0xFF && (address[11] & 0xFF) == 0xFF;
    }

    public byte[] getAddress() {
        return address.clone();
    }

    /** The address as its literal. */
    public abstract String getHostAddress();

    /** The name it was given; its literal where it was given none, for a page looks up no name. */
    public String getHostName() {
        return hostName != null ? hostName : getHostAddress();
    }

    public String getCanonicalHostName() {
        return getHostAddress();
    }

    public boolean isAnyLocalAddress() {
        for (byte part : address) {
            if (part != 0) {
                return false;
            }
        }
        return true;
    }

    public abstract boolean isLoopbackAddress();

    public abstract boolean isLinkLocalAddress();

    public abstract boolean isSiteLocalAddress();

    public abstract boolean isMulticastAddress();

    final byte[] bytes() {
        return address;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TInetAddress that && getClass() == that.getClass() && Arrays.equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(address);
    }

    /** {@code name/literal}, the name empty where it was given none — as the JDK writes it. */
    @Override
    public String toString() {
        return (hostName != null ? hostName : "") + "/" + getHostAddress();
    }
}
