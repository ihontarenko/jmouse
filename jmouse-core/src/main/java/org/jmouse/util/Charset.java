package org.jmouse.util;

import java.util.Collections;
import java.util.List;

/**
 * Enum containing many commonly used character encodings (charsets),
 * along with additional attributes that may be useful.
 */
public enum Charset {

    // ------------------------ ASCII / ANSI / ISO-8859 ------------------------
    US_ASCII(
            "US-ASCII",
            List.of("ASCII"),
            true,
            1,
            "Standard 7-bit ASCII encoding, compatible with early American ASCII.",
            List.of("English"),
            "IANA registered as ANSI X3.4-1968"),

    ISO_8859_1(
            "ISO-8859-1",
            List.of("ISO8859_1", "latin1", "l1"),
            true,
            1,
            "Common encoding for Western European languages (Latin-1).",
            List.of("Western European languages"),
            "ISO standard. Often used as a default encoding in older systems"),

    ISO_8859_2(
            "ISO-8859-2",
            List.of("ISO8859_2", "latin2", "l2"),
            true,
            1,
            "Support for Central and Eastern European languages (Latin-2).",
            List.of("Polish", "Czech", "Slovak", "Romanian", "Hungarian"),
            "ISO standard."),

    ISO_8859_3(
            "ISO-8859-3",
            List.of("ISO8859_3", "latin3", "l3"),
            true,
            1,
            "Primarily used for South European languages (Maltese, Esperanto, etc.).",
            Collections.emptyList(),
            "ISO standard."),

    ISO_8859_4(
            "ISO-8859-4",
            List.of("ISO8859_4", "latin4", "l4"),
            true,
            1,
            "Baltic languages (Lithuanian, Latvian).",
            List.of("Lithuanian", "Latvian"),
            "ISO standard."),

    ISO_8859_5(
            "ISO-8859-5",
            List.of("ISO8859_5", "cyrillic"),
            true,
            1,
            "Cyrillic script languages.",
            List.of("Russian", "Bulgarian", "Macedonian", "Serbian"),
            "ISO standard."),

    ISO_8859_6(
            "ISO-8859-6",
            List.of("ISO8859_6", "arabic"),
            true,
            1,
            "Arabic language.",
            List.of("Arabic"),
            "ISO standard."),

    ISO_8859_7(
            "ISO-8859-7",
            List.of("ISO8859_7", "greek"),
            true,
            1,
            "Greek language.",
            List.of("Greek"),
            "ISO standard."),

    ISO_8859_8(
            "ISO-8859-8",
            List.of("ISO8859_8", "hebrew"),
            true,
            1,
            "Hebrew script.",
            List.of("Hebrew"),
            "ISO standard."),

    ISO_8859_9(
            "ISO-8859-9",
            List.of("ISO8859_9", "latin5", "l5"),
            true,
            1,
            "Turkish (modified Latin-1).",
            List.of("Turkish"),
            "ISO standard."),

    ISO_8859_10(
            "ISO-8859-10",
            List.of("ISO8859_10", "latin6", "l6"),
            true,
            1,
            "Nordic languages (Norwegian, Icelandic, Sami, etc.).",
            Collections.emptyList(),
            "ISO standard."),

    ISO_8859_11(
            "ISO-8859-11",
            List.of("ISO8859_11", "thai"),
            true,
            1,
            "Thai language.",
            List.of("Thai"),
            "ISO standard."),

    // ISO-8859-12 was never officially published

    ISO_8859_13(
            "ISO-8859-13",
            List.of("ISO8859_13", "latin7", "l7"),
            true,
            1,
            "Baltic languages (alternative to ISO-8859-4).",
            Collections.emptyList(),
            "ISO standard."),

    ISO_8859_14(
            "ISO-8859-14",
            List.of("ISO8859_14", "latin8", "l8"),
            true,
            1,
            "Celtic languages (Irish, Welsh, etc.).",
            List.of("Irish", "Scottish Gaelic"),
            "ISO standard."),

    ISO_8859_15(
            "ISO-8859-15",
            List.of("ISO8859_15", "latin9", "l9"),
            true,
            1,
            "Includes the Euro symbol (€) and some other fixes for Latin characters.",
            List.of("Western European languages"),
            "ISO standard."),

    ISO_8859_16(
            "ISO-8859-16",
            List.of("ISO8859_16", "latin10", "l10"),
            true,
            1,
            "Balkan/Southeast European languages.",
            List.of("Romanian", "Albanian", "Croatian"),
            "ISO standard."),


    // ------------------------ Windows (CP125x) ------------------------
    WINDOWS_1250(
            "windows-1250",
            Collections.singletonList("Cp1250"),
            true,
            1,
            "Central and Eastern European (Latin script).",
            List.of("Polish", "Czech", "Slovak", "Hungarian"),
            "Used in Windows."),
    WINDOWS_1251(
            "windows-1251",
            Collections.singletonList("Cp1251"),
            true,
            1,
            "Cyrillic.",
            List.of("Ukrainian", "Russian", "Bulgarian"),
            "Used in Windows."),
    WINDOWS_1252(
            "windows-1252",
            Collections.singletonList("Cp1252"),
            true,
            1,
            "Western Europe (practically a superset of ISO-8859-1).",
            List.of("English", "German", "French", "etc."),
            "Used in Windows."),
    WINDOWS_1253(
            "windows-1253",
            Collections.singletonList("Cp1253"),
            true,
            1,
            "Greek.",
            List.of("Greek"),
            "Used in Windows."),
    WINDOWS_1254(
            "windows-1254",
            Collections.singletonList("Cp1254"),
            true,
            1,
            "Turkish.",
            List.of("Turkish"),
            "Used in Windows."),
    WINDOWS_1255(
            "windows-1255",
            Collections.singletonList("Cp1255"),
            true,
            1,
            "Hebrew.",
            List.of("Hebrew"),
            "Used in Windows."),
    WINDOWS_1256(
            "windows-1256",
            Collections.singletonList("Cp1256"),
            true,
            1,
            "Arabic.",
            List.of("Arabic"),
            "Used in Windows."),
    WINDOWS_1257(
            "windows-1257",
            Collections.singletonList("Cp1257"),
            true,
            1,
            "Baltic languages.",
            List.of("Lithuanian", "Latvian", "Estonian"),
            "Used in Windows."),
    WINDOWS_1258(
            "windows-1258",
            Collections.singletonList("Cp1258"),
            true,
            1,
            "Vietnamese.",
            List.of("Vietnamese"),
            "Used in Windows."),


    // ------------------------ Additional Cyrillic encodings ------------------------
    KOI8_R(
            "KOI8-R",
            Collections.emptyList(),
            true,
            1,
            "Cyrillic, variant for Russian (historically very common in Unix systems).",
            List.of("Russian"),
            "KOI8 stands for 'Kod Obmena Informatsiey, 8-bit'"),
    KOI8_U(
            "KOI8-U",
            Collections.emptyList(),
            true,
            1,
            "Cyrillic, variant supporting the Ukrainian language.",
            List.of("Ukrainian", "Belarusian"),
            "Extension of KOI8-R."),


    // ------------------------ Unicode (UTF) ------------------------
    UTF_8(
            "UTF-8",
            List.of("UTF8"),
            false,
            4,
            "Universal 8-bit variable-length Unicode encoding.",
            List.of("All languages"),
            "The most popular encoding on the Web and in modern systems."),
    UTF_16(
            "UTF-16",
            List.of("UTF_16"),
            false,
            4,
            "16-bit Unicode encoding (with possible surrogate pairs).",
            List.of("All languages"),
            "May include a BOM (Byte Order Mark)."),
    UTF_16BE(
            "UTF-16BE",
            List.of("UTF_16BE"),
            false,
            4,
            "UTF-16 Big Endian.",
            List.of("All languages"),
            "Fixed byte order (most significant byte first)."),
    UTF_16LE(
            "UTF-16LE",
            List.of("UTF_16LE"),
            false,
            4,
            "UTF-16 Little Endian.",
            List.of("All languages"),
            "Least significant byte first."),
    UTF_32(
            "UTF-32",
            List.of("UTF32"),
            false,
            4,
            "32-bit Unicode encoding (fixed length).",
            List.of("All languages"),
            "Used less frequently than UTF-8 or UTF-16."),
    UTF_32BE(
            "UTF-32BE",
            Collections.emptyList(),
            false,
            4,
            "UTF-32 Big Endian.",
            List.of("All languages"),
            "Fixed byte order (most significant byte first)."),
    UTF_32LE(
            "UTF-32LE",
            Collections.emptyList(),
            false,
            4,
            "UTF-32 Little Endian.",
            List.of("All languages"),
            "Least significant byte first."),


    // ------------------------ Other common Asian encodings ------------------------
    SHIFT_JIS(
            "Shift_JIS",
            List.of("SJIS"),
            false,
            2,
            "Japanese encoding (variable-length), historically popular in Windows.",
            List.of("Japanese"),
            "Also known as MS932 in some contexts."),
    EUC_JP(
            "EUC-JP",
            Collections.emptyList(),
            false,
            3,
            "Japanese encoding (Extended Unix Code).",
            List.of("Japanese"),
            "Popular in Unix/Linux environments."),
    ISO_2022_JP(
            "ISO-2022-JP",
            Collections.emptyList(),
            false,
            8,
            "Japanese encoding using ESC sequences.",
            List.of("Japanese"),
            "Often used in emails."),
    EUC_KR(
            "EUC-KR",
            Collections.emptyList(),
            false,
            2,
            "Korean (Hangul).",
            List.of("Korean"),
            "Also known as KSC5601."),
    GB2312(
            "GB2312",
            Collections.emptyList(),
            false,
            2,
            "Simplified Chinese (Mainland China).",
            List.of("Chinese (Simplified)"),
            "Basic national standard in China."),
    GBK(
            "GBK",
            Collections.emptyList(),
            false,
            2,
            "Extension of GB2312 for more Chinese characters.",
            List.of("Chinese"),
            "Commonly used in Windows."),
    GB18030(
            "GB18030",
            Collections.emptyList(),
            false,
            4,
            "Further extension of GBK, fully covering Unicode.",
            List.of("Chinese"),
            "Mandatory standard in China (backward-compatible with GBK/GB2312)."),
    BIG5(
            "Big5",
            Collections.emptyList(),
            false,
            2,
            "Traditional Chinese (Taiwan, Hong Kong).",
            List.of("Chinese (Traditional)"),
            "Historically the most used in Taiwanese systems."),
    BIG5_HKSCS(
            "Big5-HKSCS",
            Collections.emptyList(),
            false,
            2,
            "Extended Big5 for Hong Kong Supplementary Characters.",
            List.of("Chinese (Traditional)"),
            "Includes additional characters for Cantonese."),


    // ------------------------ "Mac" encodings ------------------------
    MAC_ROMAN(
            "x-MacRoman",
            List.of("MacRoman"),
            true,
            1,
            "Used on older Macintosh systems for Latin script.",
            List.of("English", "Western European languages"),
            "Classic Mac OS (pre-OS X)."),
    MAC_CYRILLIC(
            "x-MacCyrillic",
            List.of("MacCyrillic"),
            true,
            1,
            "Cyrillic encoding for older Macintosh systems.",
            List.of("Russian", "Ukrainian", "etc."),
            "Classic Mac OS (pre-OS X).");

    private final String       name;                // Main (canonical) charset name
    private final List<String> aliases;             // Possible aliases
    private final boolean      singleByte;          // true if single-byte, false if variable-length
    private final int          bytesPerChar;        // Maximum number of bytes for a single character
    private final String       description;         // Short description of this charset
    private final List<String> primaryLanguages;    // Common/primary languages covered by this charset
    private final String       notes;               // Additional notes or historical info

    Charset(String name, List<String> aliases, boolean singleByte, int bytesPerChar,
            String description, List<String> primaryLanguages, String notes) {
        this.name = name;
        this.aliases = aliases != null ? aliases : Collections.emptyList();
        this.singleByte = singleByte;
        this.bytesPerChar = bytesPerChar;
        this.description = description;
        this.primaryLanguages = primaryLanguages != null ? primaryLanguages : Collections.emptyList();
        this.notes = notes;
    }

    public String getName() {
        return name;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public boolean isSingleByte() {
        return singleByte;
    }

    public int getBytesPerChar() {
        return bytesPerChar;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getPrimaryLanguages() {
        return primaryLanguages;
    }

    public String getNotes() {
        return notes;
    }

}
