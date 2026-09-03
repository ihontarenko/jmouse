package org.jmouse.money;

import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 💱 An ISO 4217 currency, recognised from whatever a person actually typed.
 *
 * <h3>⚠️ The whole reason this type exists is that its input is not a currency code</h3>
 *
 * <p>A currency reaches this library as free text — the unit of a form field, a column somebody
 * imported, a spreadsheet cell. {@code USD}, {@code usd}, {@code $}, {@code грн}, {@code UAH},
 * {@code ₴}, {@code €} — and {@code pcs}, from somebody who bound the wrong field entirely. A type
 * that assumed a clean three-letter code would simply move the problem to its first caller.</p>
 *
 * <h3>⚠️ An unrecognised string is an empty answer, and that is a SUCCESSFUL call</h3>
 *
 * <p>{@link #of(String)} never guesses. The caller's job is to report what it could not read, not to
 * fall back to a default — quietly treating {@code pcs} as the base currency produces a total that is
 * wrong by an unknown amount and looks exactly like a total that is right, which is the single worst
 * failure this library can have.</p>
 *
 * <h3>⚠️ An ambiguous symbol is refused too, and {@link #candidatesFor(String)} says why</h3>
 *
 * <p>{@code €} and {@code ₴} each name one currency. {@code $} names at least the American, Canadian
 * and Australian ones, and {@code ¥} names two. Reading {@code $} as {@code USD} is right most of the
 * time, and "right most of the time" is not a property a money conversion may have. So an ambiguous
 * symbol is refused like any other unrecognised string — but {@link #candidatesFor(String)} returns
 * what it could have meant, so a screen can say <em>"$ could be USD, CAD or AUD — write the code"</em>
 * rather than <em>"unrecognised"</em>.</p>
 *
 * <h3>Why a class rather than an enum</h3>
 *
 * <p>There are about 180 active currencies and the list changes — countries redenominate, join a union,
 * leave one. An enum would freeze that list into this library's release cycle. {@link Currency} already
 * carries the JDK's copy of it, and is used here as the <strong>check</strong>, never as the parser:
 * {@link Currency#getInstance(String)} throws on anything unknown, which is an exception thrown for a
 * routine outcome, and the parsing this class actually does happens before it is consulted.</p>
 */
public final class CurrencyCode {

    /** 📏 Every ISO 4217 alphabetic code is exactly three letters. */
    private static final int CODE_LENGTH = 3;

    /**
     * 🔣 Symbols that name exactly one currency.
     *
     * <p>⚠️ Deliberately short. Every entry here is a promise that the symbol has one meaning, and the
     * cost of a wrong entry is a silently mis-converted total — so a symbol belongs in
     * {@link #AMBIGUOUS_SYMBOLS} unless it genuinely has one reading.</p>
     */
    private static final Map<String, String> UNAMBIGUOUS_SYMBOLS = Map.of(
            "€",  "EUR",
            "₴",  "UAH",
            "£",  "GBP",
            "₽",  "RUB",
            "zł", "PLN",
            "Kč", "CZK",
            "₹",  "INR",
            "₩",  "KRW"
    );

    /**
     * 🔣 Symbols that name several currencies, and everything they could name.
     *
     * <p>⚠️ These are <strong>refused</strong>, not resolved. They are listed only so a caller can
     * explain the refusal in a sentence somebody can act on.</p>
     */
    private static final Map<String, List<String>> AMBIGUOUS_SYMBOLS = Map.of(
            "$",   List.of("USD", "CAD", "AUD", "NZD", "SGD", "HKD"),
            "¥",   List.of("JPY", "CNY"),
            "kr",  List.of("SEK", "NOK", "DKK", "ISK")
    );

    /**
     * 🗣️ Spellings people write instead of the code, lower-cased.
     *
     * <p>⚠️ Matched as a <strong>prefix</strong>, so one entry covers a whole family of endings —
     * {@code грн}, {@code гривня}, {@code гривень} are one line rather than three, and nobody has to
     * remember to add the next declension. The prefixes are long enough not to collide.</p>
     */
    private static final Map<String, String> LOCAL_SPELLINGS = Map.of(
            "грн",   "UAH",
            "гривн", "UAH",
            "дол",   "USD",
            "євро",  "EUR",
            "евро",  "EUR",
            "фунт",  "GBP",
            "злот",  "PLN"
    );

    private final String code;

    private CurrencyCode(String code) {
        this.code = code;
    }

    /**
     * 💱 Reads a currency out of whatever was written.
     *
     * <p>The order is: a three-letter ISO code, then a symbol that names exactly one currency, then a
     * spelling in words. An ambiguous symbol, an unknown code and {@code pcs} all come back empty, and
     * all three are a successful call.</p>
     *
     * @param raw whatever was stored — a code, a symbol, a word, {@code null}
     * @return the currency, or empty where nothing could be read without guessing
     */
    public static Optional<CurrencyCode> of(String raw) {
        if (raw == null) {
            return Optional.empty();
        }

        String trimmed = raw.trim();

        if (trimmed.isEmpty()) {
            return Optional.empty();
        }

        String upperCased = trimmed.toUpperCase(Locale.ROOT);

        if (upperCased.length() == CODE_LENGTH && isKnownToJdk(upperCased)) {
            return Optional.of(new CurrencyCode(upperCased));
        }

        String bySymbol = UNAMBIGUOUS_SYMBOLS.get(trimmed);

        if (bySymbol != null) {
            return Optional.of(new CurrencyCode(bySymbol));
        }

        return spellingOf(trimmed).map(CurrencyCode::new);
    }

    /**
     * 💱 The same reading, for a value that has already been established as a currency.
     *
     * <p>⚠️ Use this only where an empty answer would be a programming error — a constant in this
     * library's own code, or a value the caller has already run through {@link #of(String)}. Never on
     * stored input: that is what {@link #of(String)} is for, and turning its empty answer into an
     * exception is how an unreadable unit becomes a failed request instead of a reported one.</p>
     *
     * @param raw the text to read
     * @return the currency
     * @throws IllegalArgumentException where nothing could be read
     */
    public static CurrencyCode required(String raw) {
        return of(raw).orElseThrow(
                () -> new IllegalArgumentException("Not a currency this library recognises: " + raw));
    }

    /**
     * 🔣 What an unreadable string could have meant, for the sentence explaining that it was refused.
     *
     * <p>Empty for anything genuinely unknown. Non-empty only for an <em>ambiguous</em> symbol, which
     * is the case worth a better message than "unrecognised" — somebody wrote {@code $} and needs to
     * be told that it is not specific rather than that it is not known.</p>
     *
     * @param raw the text that {@link #of(String)} refused
     * @return every currency it might have named, in the order to offer them
     */
    public static List<CurrencyCode> candidatesFor(String raw) {
        if (raw == null) {
            return List.of();
        }

        List<String> candidates = AMBIGUOUS_SYMBOLS.get(raw.trim());

        if (candidates == null) {
            return List.of();
        }

        return candidates.stream().map(CurrencyCode::new).toList();
    }

    /** 💱 The ISO 4217 alphabetic code, upper case — {@code USD}, {@code UAH}. */
    public String code() {
        return code;
    }

    /**
     * 🔣 The symbol the JDK knows for this currency in a given locale, falling back to the code.
     *
     * <p>Display only. ⚠️ Never round-trip it through {@link #of(String)} — the JDK returns {@code $}
     * for the dollar, which this class deliberately refuses to read back.</p>
     *
     * @param locale whose spelling of the symbol is wanted
     * @return the symbol, or the code where the locale has no symbol for it
     */
    public String symbol(Locale locale) {
        return Currency.getInstance(code).getSymbol(locale);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CurrencyCode currency && code.equals(currency.code);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(code);
    }

    @Override
    public String toString() {
        return code;
    }

    /** ⚠️ {@link Currency#getInstance(String)} throws rather than answering, so the throw is the answer. */
    private static boolean isKnownToJdk(String upperCasedCode) {
        try {
            Currency.getInstance(upperCasedCode);
            return true;
        } catch (IllegalArgumentException notACurrency) {
            return false;
        }
    }

    private static Optional<String> spellingOf(String trimmed) {
        String lowerCased = trimmed.toLowerCase(Locale.ROOT);

        for (Map.Entry<String, String> spelling : LOCAL_SPELLINGS.entrySet()) {
            if (lowerCased.startsWith(spelling.getKey())) {
                return Optional.of(spelling.getValue());
            }
        }

        return Optional.empty();
    }
}
