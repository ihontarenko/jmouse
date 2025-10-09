package org.jmouse.web.http;

import org.jmouse.util.StringHelper;

import java.util.*;

/**
 * 🌐 Client Hints & Fetch Metadata enums + parsers.
 * <p>
 * Covers:
 * <ul>
 *   <li>sec-ch-ua (brand tokens)</li>
 *   <li>sec-ch-ua-mobile</li>
 *   <li>sec-ch-ua-platform</li>
 *   <li>sec-fetch-dest</li>
 *   <li>sec-fetch-mode</li>
 *   <li>sec-fetch-site</li>
 *   <li>sec-fetch-user</li>
 * </ul>
 */
public final class ClientHints {

    private ClientHints() {
    }

    /**
     * Determine the primary brand from tokens, honoring list order and skipping NOT_A_BRAND.
     */
    public static KnownBrand primaryOf(List<BrandToken> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return KnownBrand.UNKNOWN;
        }

        for (BrandToken token : tokens) {
            KnownBrand known = toKnownBrand(normalize(token.brand()));
            if (known != KnownBrand.UNKNOWN && known != KnownBrand.NOT_A_BRAND) {
                return known;
            }
        }

        // Fallbacks if only Chromium or only Not;A=Brand present
        for (BrandToken t : tokens) {
            KnownBrand known = toKnownBrand(normalize(t.brand()));
            if (known == KnownBrand.CHROMIUM) {
                return known;
            }
        }

        for (BrandToken t : tokens) {
            KnownBrand known = toKnownBrand(normalize(t.brand()));
            if (known == KnownBrand.NOT_A_BRAND) {
                return known;
            }
        }

        return KnownBrand.UNKNOWN;
    }

    /**
     * All recognized brands present in tokens (excludes UNKNOWN).
     */
    public static EnumSet<KnownBrand> allOf(List<BrandToken> tokens) {
        EnumSet<KnownBrand> brands = EnumSet.noneOf(KnownBrand.class);

        if (tokens == null) {
            return brands;
        }

        for (BrandToken token : tokens) {
            KnownBrand known = toKnownBrand(normalize(token.brand()));
            if (known != KnownBrand.UNKNOWN) {
                brands.add(known);
            }
        }

        return brands;
    }

    // =====================================================================================
    // sec-ch-ua-mobile → "?0" (desktop) | "?1" (mobile)
    // =====================================================================================

    /**
     * Return version string for the first token that matches the given known brand; null if absent.
     */
    public static String versionOf(KnownBrand brand, List<BrandToken> tokens) {
        if (brand == null || tokens == null) {
            return null;
        }

        for (BrandToken token : tokens) {
            if (toKnownBrand(normalize(token.brand())) == brand) {
                return token.version();
            }
        }

        return null;
    }

    // =====================================================================================
    // sec-ch-ua-platform → quoted name (Windows, macOS, Android, iOS, Linux, Chrome OS, ...)
    // =====================================================================================

    public static boolean isChrome(List<BrandToken> token) {
        return has(KnownBrand.CHROME, token);
    }

    // =====================================================================================
    // Fetch Metadata: sec-fetch-dest, sec-fetch-mode, sec-fetch-site, sec-fetch-user
    // Specs: https://wicg.github.io/sec-metadata/
    // =====================================================================================

    public static boolean isChromium(List<BrandToken> token) {
        return has(KnownBrand.CHROMIUM, token);
    }

    public static boolean isEdge(List<BrandToken> token) {
        return has(KnownBrand.EDGE, token);
    }

    public static boolean isOpera(List<BrandToken> token) {
        return has(KnownBrand.OPERA, token);
    }

    public static boolean isVivaldi(List<BrandToken> token) {
        return has(KnownBrand.VIVALDI, token);
    }

    public static boolean isBrave(List<BrandToken> token) {
        return has(KnownBrand.BRAVE, token);
    }

    public static boolean isSamsungInternet(List<BrandToken> token) {
        return has(KnownBrand.SAMSUNG_INTERNET, token);
    }

    public static boolean isWebView(List<BrandToken> token) {
        return has(KnownBrand.ANDROID_WEBVIEW, token);
    }

    /**
     * Parse header and return primary known brand.
     */
    public static KnownBrand parsePrimary(String secChUa) {
        return primaryOf(SecChUa.parse(secChUa));
    }

    /**
     * Parse header and return all recognized brands.
     */
    public static EnumSet<KnownBrand> parseAll(String secChUa) {
        return allOf(SecChUa.parse(secChUa));
    }

    private static boolean has(KnownBrand brand, List<BrandToken> tokens) {
        if (tokens == null) {
            return false;
        }

        for (BrandToken token : tokens) {
            if (toKnownBrand(normalize(token.brand())) == brand) {
                return true;
            }
        }

        return false;
    }

    private static String normalize(String brand) {
        return brand == null ? "" : brand.trim().toLowerCase(Locale.ROOT);
    }

    private static KnownBrand toKnownBrand(String normalized) {
        return switch (normalized) {
            case "google chrome" -> KnownBrand.CHROME;
            case "chromium" -> KnownBrand.CHROMIUM;
            case "microsoft edge" -> KnownBrand.EDGE;
            case "opera" -> KnownBrand.OPERA;
            case "vivaldi" -> KnownBrand.VIVALDI;
            case "brave" -> KnownBrand.BRAVE;
            case "samsung internet" -> KnownBrand.SAMSUNG_INTERNET;
            case "android webview" -> KnownBrand.ANDROID_WEBVIEW;
            case "not;a=brand" -> KnownBrand.NOT_A_BRAND;
            default -> KnownBrand.UNKNOWN;
        };
    }

    /**
     * 📱 Mobile hint.
     */
    public enum SecChUaMobile {

        DESKTOP("?0"),
        MOBILE("?1");

        private final String wire; // wire-format value

        SecChUaMobile(String wire) {
            this.wire = wire;
        }

        /**
         * Parse {@code ?0|?1}; null/unknown → DESKTOP by default.
         */
        public static SecChUaMobile parse(String headerValue) {
            if ("?1".equals(headerValue)) {
                return MOBILE;
            }

            return DESKTOP; // treat null, "?0", or anything else as desktop
        }

        public String wire() {
            return wire;
        }
    }

    /**
     * 💻 Known platforms for convenience (unknowns fall back to OTHER).
     */
    public enum SecChUaPlatform {

        WINDOWS("Windows"),
        MACOS("macOS"),
        ANDROID("Android"),
        IOS("iOS"),
        LINUX("Linux"),
        CHROME_OS("Chrome OS"),
        FUCHSIA("Fuchsia"),
        OTHER("");

        private final String canonical;

        SecChUaPlatform(String canonical) {
            this.canonical = canonical;
        }

        /**
         * Parse a quoted or raw value; returns OTHER when not recognized.
         */
        public static SecChUaPlatform parse(String header) {
            if (header == null || header.isBlank()) {
                return OTHER;
            }

            String value = StringHelper.unquote(header).trim();

            for (SecChUaPlatform platform : values()) {
                if (!platform.equals(OTHER) && platform.canonical.equalsIgnoreCase(value)) {
                    return platform;
                }
            }

            return OTHER;
        }

        public String canonical() {
            return canonical;
        }
    }

    /**
     * 🎯 Destination type for the request.
     */
    public enum SecFetchDest {

        EMPTY(""),
        AUDIO("audio"),
        AUDIO_WORKLET("audioworklet"),
        DOCUMENT("document"),
        EMBED("embed"),
        FONT("font"),
        FRAME("frame"),
        IFRAME("iframe"),
        IMAGE("image"),
        MANIFEST("manifest"),
        OBJECT("object"),
        PAINT_WORKLET("paintworklet"),
        REPORT("report"),
        SCRIPT("script"),
        SERVICE_WORKER("serviceworker"),
        SHARED_WORKER("sharedworker"),
        STYLE("style"),
        TRACK("track"),
        VIDEO("video"),
        WORKER("worker"),
        XSLT("xslt"),
        UNKNOWN("");

        private final String wire;

        SecFetchDest(String wire) {
            this.wire = wire;
        }

        public static SecFetchDest parse(String header) {
            if (header == null) {
                return UNKNOWN;
            }

            String value = header.trim().toLowerCase(Locale.ROOT);

            for (SecFetchDest fetchDest : values()) {
                if (!fetchDest.equals(UNKNOWN) && fetchDest.wire.equals(value)) {
                    return fetchDest;
                }
            }

            if (value.isEmpty()) {
                return EMPTY;
            }

            return UNKNOWN;
        }

        public String wire() {
            return wire;
        }
    }

    /**
     * 🚦 Request mode.
     */
    public enum SecFetchMode {
        CORS("cors"),
        NAVIGATE("navigate"),
        NO_CORS("no-cors"),
        SAME_ORIGIN("same-origin"),
        WEBSOCKET("websocket"),
        UNKNOWN("");

        private final String wire;

        SecFetchMode(String wire) {
            this.wire = wire;
        }

        public static SecFetchMode parse(String header) {
            if (header == null) {
                return UNKNOWN;
            }

            String value = header.trim().toLowerCase(Locale.ROOT);

            for (SecFetchMode fetchMode : values()) {
                if (!fetchMode.equals(UNKNOWN) && fetchMode.wire.equals(value)) {
                    return fetchMode;
                }
            }

            return UNKNOWN;
        }

        public String wire() {
            return wire;
        }
    }

    /**
     * 🌍 Site relationship.
     */
    public enum SecFetchSite {
        CROSS_SITE("cross-site"),
        SAME_ORIGIN("same-origin"),
        SAME_SITE("same-site"),
        NONE("none"),
        UNKNOWN("");

        private final String wire;

        SecFetchSite(String wire) {
            this.wire = wire;
        }

        public static SecFetchSite parse(String header) {
            if (header == null) {
                return UNKNOWN;
            }

            String value = header.trim().toLowerCase(Locale.ROOT);

            for (SecFetchSite fetchSite : values()) {
                if (!fetchSite.equals(UNKNOWN) && fetchSite.wire.equals(value)) {
                    return fetchSite;
                }
            }

            return UNKNOWN;
        }

        public String wire() {
            return wire;
        }
    }

    /**
     * 👤 User activation indicator. Present as {@code ?1} when a user gesture triggered the request.
     */
    public enum SecFetchUser {

        NONE("?0"), // rarely sent; treat absence as NONE
        USER("?1"),
        ABSENT(null); // header not present at all

        private final String wire;

        SecFetchUser(String wire) {
            this.wire = wire;
        }

        /**
         * Parse value; {@code null} → ABSENT. Any non-"?1" value maps to NONE.
         */
        public static SecFetchUser parse(String headerValue) {
            if (headerValue == null) {
                return ABSENT;
            }

            return "?1".equals(headerValue) ? USER : NONE;
        }

        public String wire() {
            return wire;
        }
    }

    /**
     * 🏷 Known Chromium-family brands observed in UA-CH brand tokens.
     * <p>Note: some browsers (e.g., Brave) may omit their brand; detection then falls back to Chrome/Chromium tokens.</p>
     */
    public enum KnownBrand {

        CHROME("Google Chrome"),
        CHROMIUM("Chromium"),
        EDGE("Microsoft Edge"),
        OPERA("Opera"),
        VIVALDI("Vivaldi"),
        BRAVE("Brave"),
        SAMSUNG_INTERNET("Samsung Internet"),
        ANDROID_WEBVIEW("Android WebView"),
        NOT_A_BRAND("Not;A=Brand"),
        UNKNOWN("");

        private final String canonical;

        KnownBrand(String canonical) {
            this.canonical = canonical;
        }

        /**
         * Canonical human-readable brand name.
         */
        public String canonical() {
            return canonical;
        }
    }

    /**
     * 🧩 A single UA brand token entry like: {@code "Google Chrome";v="139"}.
     *
     * @param brand   e.g. "Google Chrome", "Chromium", "Not;A=Brand"
     * @param version e.g. "139", may be composite depending on browser
     */
    public record BrandToken(String brand, String version) { }

    /**
     * 🔎 Parser for {@code sec-ch-ua} header value.
     * <p>Accepts a comma-separated list like:
     * <pre>
     * {@code "Not;A=Brand";v="99", "Google Chrome";v="139", "Chromium";v="139"}
     * </pre>
     */
    public static final class SecChUa {

        /**
         * Parse into ordered list of brand tokens; returns empty list on null/blank.
         */
        public static List<BrandToken> parse(String value) {
            if (value == null || value.isBlank()) {
                return Collections.emptyList();
            }

            int              position = 0;
            List<BrandToken> tokens   = new ArrayList<>();

            while (position < value.length()) {
                int comma = value.indexOf(',', position);

                if (comma == -1) {
                    comma = value.length();
                }

                String segment = value.substring(position, comma).trim();

                if (!segment.isBlank()) {
                    int    separator  = segment.lastIndexOf(';');
                    String name       = segment.substring(0, separator);
                    String parameters = segment.substring(separator + 1);
                    int    equal      = parameters.indexOf('=');
                    String version    = parameters.substring(equal + 1);

                    name = StringHelper.unquote(name);
                    version = StringHelper.unquote(version);

                    tokens.add(new BrandToken(name, version));
                }

                position = comma + 1;
            }

            return tokens;
        }
    }

}

