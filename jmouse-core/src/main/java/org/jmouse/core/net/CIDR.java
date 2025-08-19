package org.jmouse.core.net;

import java.math.BigInteger;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Comparator;
import java.util.Objects;

import static java.math.BigInteger.ONE;

/**
 * Immutable representation of a CIDR block for IPv4/IPv6.
 *
 * <p>Instances are always normalized: the stored {@code network} value has all host bits zeroed,
 * i.e. {@code network == (value & mask(bits, prefix))}. The class is thread-safe by virtue of
 * immutability.</p>
 *
 * <h2>Key semantics</h2>
 * <ul>
 *   <li><b>Containment (IP):</b> {@code contains(ip)} if {@code (ip & mask) == network}.</li>
 *   <li><b>Containment (CIDR):</b> {@code A.contains(B)} if families match, {@code A.prefix <= B.prefix},
 *       and {@code (B.network & A.mask) == A.network}.</li>
 *   <li><b>Range:</b> Inclusive interval {@code [network, network + size - 1]}.</li>
 *   <li><b>Split:</b> {@code split(newPrefix)} produces aligned subnets covering the original block.</li>
 * </ul>
 *
 * <p><b>Address families.</b> IPv4 uses 32 bits, IPv6 uses 128 bits. IPv4-mapped IPv6 addresses
 * (i.e. {@code ::ffff:w.x.y.z}) are normalized to pure IPv4 for consistent family checks.</p>
 *
 * @implNote All arithmetic is performed with {@link java.math.BigInteger} using unsigned
 * conversion ({@code new BigInteger(1, bytes)}) to avoid sign issues and to unify v4/v6 logic.
 */
public final class CIDR implements Comparable<CIDR> {

    /**
     * Comparator for Longest-Prefix Match (LPM).
     *
     * <p>Orders blocks by:
     * <ol>
     *   <li>address family width ({@code 128} before {@code 32} by default here, so IPv6 then IPv4),</li>
     *   <li>prefix length descending (more specific first),</li>
     *   <li>numeric network value ascending (stable tie-breaker).</li>
     * </ol>
     *
     * <p>Use when building match tables where the most specific route/prefix should be considered first.</p>
     */
    public static final Comparator<CIDR> LPM_COMPARATOR = Comparator
            .comparingInt(CIDR::bitWidth)                // group by family
            .thenComparingInt(CIDR::prefix).reversed()   // longer prefix first
            .thenComparing(CIDR::network);               // stable order within same prefix

    /** Bit width for IPv6 addresses. */
    public static final int IPV6_BITS = 128;
    /** Bit width for IPv4 addresses. */
    public static final int IPV4_BITS = 32;

    /** Normalized network value (host bits = 0). */
    private final BigInteger network;
    /** Address space width: {@code 32} for IPv4 or {@code 128} for IPv6. */
    private final int        bits;
    /** Prefix length in {@code [0, bits]}. */
    private final int        prefix;
    /** Canonical MSB-aligned mask for {@code (bits, prefix)}. */
    private final BigInteger mask;
    /** Number of addresses in the block: {@code 2^(bits - prefix)}. */
    private final BigInteger size;
    /** Fast flag for {@code bits == 128}. */
    private final boolean    ipv6;

    /**
     * Constructs an immutable CIDR with fully computed, normalized fields.
     *
     * @param network normalized network value (host bits must be zero)
     * @param prefix  prefix length in {@code [0, bits]}
     * @param bits    address width ({@code 32} or {@code 128})
     * @param mask    canonical mask for {@code (bits, prefix)}
     * @param size    address count for {@code (bits, prefix)}
     */
    private CIDR(BigInteger network, int prefix, int bits, BigInteger mask, BigInteger size) {
        this.network = network;
        this.prefix = prefix;
        this.bits = bits;
        this.mask = mask;
        this.size = size;
        this.ipv6 = (bits == IPV6_BITS);
    }

    /**
     * Parses {@code "ip/prefix"} or just {@code "ip"} (in which case the maximal prefix is used:
     * {@code /32} for IPv4 or {@code /128} for IPv6).
     *
     * @param cidr textual form
     * @return a normalized CIDR instance
     * @throws IllegalArgumentException if the input is not a valid IP or has an invalid prefix
     */
    public static CIDR parse(String cidr) {
        int slash = cidr.indexOf("/");

        if (slash == -1) {
            try {
                int prefix = (InetAddress.getByName(cidr) instanceof Inet6Address) ? IPV6_BITS : IPV4_BITS;
                return of(cidr, prefix);
            } catch (UnknownHostException e) {
                throw new IllegalArgumentException("Invalid IP: " + cidr, e);
            }
        }

        String ip     = cidr.substring(0, slash);
        int    prefix = Integer.parseInt(cidr.substring(slash + 1));

        return of(ip, prefix);
    }

    /**
     * Builds a CIDR from textual IP and prefix.
     *
     * @param ip textual IP literal (no DNS resolution needed for literals)
     * @param prefix prefix length
     * @return a normalized CIDR instance
     * @throws IllegalArgumentException if the IP is invalid or the prefix is out of range
     */
    public static CIDR of(String ip, int prefix) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            return of(address, prefix);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Invalid IP: " + ip, e);
        }
    }

    /**
     * Builds a CIDR from {@link InetAddress} and prefix. IPv4-mapped IPv6 addresses are normalized
     * to pure IPv4 before processing.
     *
     * @param ip {@link InetAddress} value
     * @param prefix prefix length
     * @return a normalized CIDR instance
     * @throws IllegalArgumentException if {@code prefix} is out of range
     */
    public static CIDR of(InetAddress ip, int prefix) {
        InetAddress address = normalize(ip);
        final int   bits    = (address instanceof Inet6Address) ? IPV6_BITS : IPV4_BITS;
        BigInteger  network = unsigned(address.getAddress());
        BigInteger  mask    = mask(bits, prefix);
        BigInteger  size    = size(bits, prefix);

        // normalize to canonical base address (zero host bits)
        network = network.and(mask);

        return new CIDR(network, prefix, bits, mask, size);
    }

    /**
     * @return {@code true} if this block is IPv6 ({@code bits == 128})
     */
    public boolean isIpv6() {
        return ipv6;
    }

    /**
     * Checks whether the CIDR contains the textual IP address.
     *
     * <p>Returns {@code false} if the string cannot be parsed as an IP literal.</p>
     *
     * @param ip textual IP
     * @return {@code true} if contained, {@code false} otherwise
     */
    public boolean contains(String ip) {
        try {
            return contains(InetAddress.getByName(ip));
        } catch (UnknownHostException ignored) {}
        return false;
    }

    /**
     * Checks whether the CIDR contains the given address.
     *
     * <p>Family must match (IPv4 vs IPv6). The check is {@code (addr & mask) == network}.</p>
     *
     * @param ip {@link InetAddress} to test
     * @return {@code true} if contained, {@code false} otherwise
     */
    public boolean contains(InetAddress ip) {
        InetAddress address = normalize(ip);

        if (isIpv6() != (address instanceof Inet6Address)) {
            return false;
        }

        BigInteger value = unsigned(address.getAddress());
        return value.and(mask).equals(this.network);
    }

    /**
     * Computes the numeric last address in the block:
     * {@code network + size - 1}.
     *
     * @return inclusive upper bound as {@link BigInteger}
     */
    public BigInteger lastNetwork() {
        return network.add(size.subtract(ONE));
    }

    /**
     * Splits this CIDR into aligned subnets with a longer prefix.
     *
     * <p>Let {@code Δ = newPrefix - prefix}. The method returns {@code 2^Δ} contiguous subnets
     * of equal size whose union is exactly this block. Families and alignment are preserved.</p>
     *
     * @param newPrefix target (longer) prefix
     * @return array of sub-CIDRs in ascending order
     * @throws IllegalArgumentException if {@code newPrefix} is outside {@code [prefix, bits]}
     */
    public CIDR[] split(int newPrefix) {
        if (newPrefix < prefix || newPrefix > bits) {
            throw new IllegalArgumentException("newPrefix must be in [%d, %d]".formatted(prefix, bits));
        }

        int        delta  = newPrefix - prefix;
        int        count  = 1 << Math.min(delta, 30); // guard against insane splits
        CIDR[]     result = new CIDR[count];
        BigInteger base   = this.network;
        BigInteger step   = size(bits, newPrefix);
        BigInteger mask   = mask(bits, newPrefix);

        for (int i = 0; i < count; i++) {
            BigInteger network = base.add(step.multiply(BigInteger.valueOf(i)));
            System.out.println(toBinaryGrouped(network));
            result[i] = new CIDR(network, newPrefix, bits, mask, step);
        }

        return result;
    }

    /**
     * @return the normalized base (network) address as {@link InetAddress}
     */
    public InetAddress networkAddress() {
        return toInet(this.network, this.bits);
    }

    /**
     * @return the inclusive last address of the block as {@link InetAddress}
     */
    public InetAddress lastAddress() {
        return toInet(lastNetwork(), this.bits);
    }

    public InetAddress firstUsable() {
        return toInet(this.network.add(ONE), this.bits);
    }

    public InetAddress lastUsable() {
        return toInet(lastNetwork().subtract(ONE), this.bits);
    }

    /**
     * @return the address space width ({@code 32} or {@code 128})
     */
    public int bitWidth() {
        return bits;
    }

    /**
     * @return normalized network value (host bits zero)
     */
    public BigInteger network() {
        return network;
    }

    /**
     * @return prefix length
     */
    public int prefix() {
        return prefix;
    }

    /**
     * @return canonical MSB-aligned mask for this CIDR
     */
    public BigInteger mask() {
        return mask;
    }

    /**
     * @return number of addresses in this block ({@code 2^(bits - prefix)})
     */
    public BigInteger size() {
        return size;
    }

    /**
     * @return inclusive address range {@code [network, last]} as a pair of {@link InetAddress}
     */
    public Range range() {
        return new Range(networkAddress(), lastAddress());
    }

    /**
     * Returns the CIDR bitmask for a {@code B}-bit address space and prefix length {@code p}.
     *
     * <p><b>Shape.</b> The result has the top {@code p} bits set to {@code 1} and the remaining
     * {@code (B - p)} low-order bits set to {@code 0}.
     * Example: {@code B=32, p=24 → 0xFFFF_FF00} ({@code 255.255.255.0}).</p>
     *
     * <p><b>Construction.</b> Build {@code p} ones in the low-order side and then align them to MSB:
     * <pre>{@code
     * mask(B, p) = ((2^p - 1) << (B - p))
     * }</pre>
     * Step 1 produces {@code 000…0011…11}; Step 2 shifts left by {@code B - p} to obtain
     * {@code 11…1100…00}.</p>
     *
     * <p><b>Semantics.</b> For any address {@code x ∈ [0, 2^B)}, the expression {@code (x & mask)}
     * preserves the {@code p} most-significant bits (network part) and zeroes the host part.</p>
     *
     * <p><b>Edge cases.</b> {@code p = 0 → 0}. {@code p = B → 2^B - 1} (all ones).</p>
     *
     * <p><b>Notes.</b> {@link java.math.BigInteger} has unbounded width; avoid bitwise NOT
     * to prevent sign/width artifacts. Use this construction instead.</p>
     *
     * @param bits   total bit width (e.g., {@code 32} for IPv4, {@code 128} for IPv6)
     * @param prefix prefix length in {@code [0, bits]}
     * @return the canonical MSB-aligned CIDR mask
     * @throws IllegalArgumentException if {@code prefix} is out of range
     */
    public static BigInteger mask(int bits, int prefix) {
        if (prefix < 0 || prefix > bits) {
            throw new IllegalArgumentException("prefix must be in [0," + bits + "], got " + prefix);
        }

        if (prefix == 0) {
            return BigInteger.ZERO;
        }

        // step 1: build p low-order ones (…000111…1)
        BigInteger ones = BigInteger.ONE.shiftLeft(prefix).subtract(BigInteger.ONE);
        // step 2: align to MSB → p high-order ones followed by (B−p) zeros
        return ones.shiftLeft(bits - prefix);
    }

    /**
     * Computes the block size for {@code (bits, prefix)}: {@code 2^(bits - prefix)}.
     *
     * @param bits   address space width
     * @param prefix prefix length
     * @return number of addresses in the block
     */
    public static BigInteger size(int bits, int prefix) {
        return ONE.shiftLeft(bits - prefix);
    }

    /**
     * Normalizes an {@link InetAddress} by converting IPv4-mapped IPv6 addresses
     * ({@code ::ffff:w.x.y.z}) to pure IPv4.
     *
     * <p>This ensures consistent family checks (IPv4 vs IPv6) and avoids treating mapped IPv6
     * as IPv6 proper in containment operations.</p>
     *
     * @param address input address
     * @return the same address if not IPv4-mapped; otherwise a pure IPv4 address
     *
     * @implNote For a mapped address the first 10 bytes are {@code 0x00}, then bytes
     *           10..11 are {@code 0xFF}, and bytes 12..15 carry the IPv4 payload.
     */
    public static InetAddress normalize(InetAddress address) {
        InetAddress ipv4 = address;

        if (address instanceof Inet6Address ipv6)  {
            byte[] bytes = ipv6.getAddress();
            boolean mapped = true;

            // First 10 bytes must be 0x00
            for (int i = 0; i < 10; i++) {
                if (bytes[i] != 0) {
                    mapped = false;
                    break;
                }
            }

            // Next two bytes must be 0xFF (i.e., ::ffff:0:0/96 space)
            if (mapped && bytes[10] == (byte) 0xFF && bytes[11] == (byte) 0xFF) {
                byte[] v4 = new byte[]{bytes[12], bytes[13], bytes[14], bytes[15]};
                try {
                    ipv4 = InetAddress.getByAddress(v4);
                } catch (UnknownHostException ignored) {
                }
            }
        }

        return ipv4;
    }

    /**
     * Converts an unsigned numeric address into an {@link InetAddress} of the given width.
     *
     * <p>Performs left-padding with zeros to reach the exact byte-length (4 for IPv4,
     * 16 for IPv6). If the numeric value has more bytes than {@code length}, the most-significant
     * extra bytes are truncated.</p>
     *
     * @param network unsigned numeric address
     * @param bits    address space width ({@code 32} or {@code 128})
     * @return {@link InetAddress} backed by exactly {@code bits/8} bytes
     * @throws IllegalArgumentException if the resulting byte array is not a valid address
     */
    public static InetAddress toInet(BigInteger network, int bits) {
        int    length   = bits / 8;
        byte[] rawBytes = network.toByteArray();
        byte[] array    = new byte[length];

        // copy the least-significant 'length' bytes, left-padding with zeros if needed
        int offset     = Math.max(0, rawBytes.length - length);
        int byteToCopy = Math.min(length, rawBytes.length);

        System.arraycopy(rawBytes, offset, array, length - byteToCopy, byteToCopy);

        try {
            return InetAddress.getByAddress(array);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Invalid address bytes", e);
        }
    }

    /**
     * Interprets {@code bytes} as an unsigned big-endian integer.
     *
     * @param bytes network-order bytes
     * @return non-negative {@link BigInteger} with the same magnitude
     */
    private static BigInteger unsigned(byte[] bytes) {
        return new BigInteger(1, bytes);
    }

    public String toBinary(BigInteger x) {
        String binaryString = x.toString(2);

        if (binaryString.length() < bits) {
            binaryString = "0".repeat(bits - binaryString.length()) + binaryString;
        }

        return binaryString;
    }

    public String toBinaryGrouped(BigInteger x) {
        return toBinary(x).replaceAll("(.{8})(?!$)", "$1 ");  // групи по 8 біт: 00000000 11111111 ...
    }

    @Override
    public String toString() {
        return networkAddress().getHostAddress() + "/" + prefix;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof CIDR cidr)) {
            return false;
        }

        return prefix == cidr.prefix && bits == cidr.bits && network.equals(cidr.network);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(network, prefix, bits);
    }

    /**
     * Natural ordering: by family width, then by numeric network value, then by prefix length.
     *
     * <p>This is suitable for stable listings. For longest-prefix match (LPM) tables consider
     * a comparator that orders by descending prefix.</p>
     */
    @Override
    public int compareTo(CIDR that) {
        int c = Integer.compare(this.bits, that.bits);

        if (c != 0) {
            return c;
        }

        c = this.network.compareTo(that.network);

        if (c != 0){
            return c;
        }

        return Integer.compare(this.prefix, that.prefix);
    }

    /**
     * Inclusive address range for a CIDR block.
     *
     * <p>Both {@code start} and {@code end} are canonical {@link InetAddress} values obtained from
     * the numeric bounds.</p>
     *
     * @param start first address in the block (network address)
     * @param end   last address in the block (broadcast for IPv4)
     */
    public record Range(InetAddress start, InetAddress end) {

        @Override
        public String toString() {
            return start.getHostAddress() + " - " + end.getHostAddress();
        }

    }

    public static void main(String[] args) {
        CIDR c1 = CIDR.parse("192.168.1.1/24");
        CIDR c2 = CIDR.parse("192.168.1.96/29");

        c1.split(29);
        c1.networkAddress();

        System.out.println(c1);
    }

}
