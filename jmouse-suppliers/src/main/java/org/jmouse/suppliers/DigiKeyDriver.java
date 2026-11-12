package org.jmouse.suppliers;

import org.jmouse.http.client.HttpCall;
import org.jmouse.http.client.HttpTransport;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 🏬 DigiKey's keyword search (Product Information V4), behind its client-credentials exchange.
 *
 * <p>⚠️ <strong>The token is cached per set of credentials, not per driver.</strong> One cached token would be issued
 * under one account and sent under another after a secret is rotated, and DigiKey answers that with a 401 that reads as a
 * wrong secret rather than as a stale cache. So the key is the address, the client and a digest of the secret together,
 * and a 401 evicts the token it was sent with: the next search asks for a fresh one.</p>
 */
public final class DigiKeyDriver implements SupplierDriver {

    /**
     * Which of DigiKey's sites answers, and in what.
     *
     * @param site     {@code US}
     * @param language {@code en}
     * @param currency {@code USD}; what the prices are in
     */
    public record Locale(String site, String language, String currency) {

        /** DigiKey's United States site, in English and dollars. */
        public static final Locale UNITED_STATES = new Locale("US", "en", "USD");
    }

    private static final URI ADDRESS = URI.create("https://api.digikey.com");
    private static final String TOKEN_PATH = "/v1/oauth2/token";
    private static final String SEARCH_PATH = "/products/v4/search/keyword";
    private static final int MOST_RECORDS = 50;
    private static final String JSON = "application/json";
    private static final String FORM = "application/x-www-form-urlencoded";

    /** How long a token is trusted when DigiKey does not say: short, as one outliving the real lifetime is a 401. */
    private static final long USUAL_TOKEN_SECONDS = 300;

    /** A token is asked for again this long before it expires. */
    private static final long EXPIRY_MARGIN_SECONDS = 60;

    /** Whose token it is. The secret is never a key, only its digest. */
    private record TokenOwner(URI address, String clientIdentifier, String secretDigest) {
    }

    private record Token(String value, Instant expires) {
    }

    private final SupplierExchange         exchange;
    private final Clock                    clock;
    private final Supplier<Locale>         locale;
    private final Map<TokenOwner, Token>   tokens = new ConcurrentHashMap<>();

    public DigiKeyDriver(HttpTransport transport, Clock clock, Locale locale) {
        this(transport, clock, () -> locale);
    }

    /** @param locale what is asked for, read at each call: changed while the server runs, it is the next call's */
    public DigiKeyDriver(HttpTransport transport, Clock clock, Supplier<Locale> locale) {
        this.exchange = new SupplierExchange(transport);
        this.clock = clock;
        this.locale = locale;
    }

    @Override
    public String kind() {
        return "digikey";
    }

    @Override
    public String name() {
        return "DigiKey";
    }

    @Override
    public URI defaultAddress() {
        return ADDRESS;
    }

    @Override
    public int mostPerSearch() {
        return MOST_RECORDS;
    }

    @Override
    public List<SupplierOffer> search(SupplierAccess access, String query, int limit) {
        if (!(access.credentials() instanceof SupplierCredentials.ClientCredentials credentials) || !credentials.complete()) {
            throw new SupplierFailure(SupplierFailure.Reason.NOT_CONFIGURED, access.label() + " has no client identifier and secret.");
        }
        URI base = access.address() == null ? ADDRESS : access.address();
        TokenOwner owner = new TokenOwner(base, credentials.clientIdentifier(), digestOf(credentials.clientSecret()));
        String token = tokenOf(owner, base, access, credentials);
        String request = SupplierExchange.json(Map.of("keywords", query, "limit", Math.min(limit, MOST_RECORDS), "offset", 0));
        HttpCall call = HttpCall.post(base.resolve(SEARCH_PATH)).header("Accept", JSON)
                .header("Authorization", "Bearer " + token)
                .header("X-DIGIKEY-Client-Id", credentials.clientIdentifier())
                .header("X-DIGIKEY-Locale-Site", locale.get().site())
                .header("X-DIGIKEY-Locale-Language", locale.get().language())
                .header("X-DIGIKEY-Locale-Currency", locale.get().currency())
                .text(request, JSON).timeout(access.timeout()).build();
        JsonNode answer;
        try {
            answer = exchange.answerTo(call, access);
        } catch (SupplierFailure refused) {
            if (refused.reason() == SupplierFailure.Reason.CREDENTIALS_REFUSED) {
                tokens.remove(owner);
            }
            throw refused;
        }
        List<SupplierOffer> offers = new ArrayList<>();
        for (JsonNode product : answer.path("Products")) {
            offers.add(offerOf(product));
        }
        return List.copyOf(offers);
    }

    private String tokenOf(TokenOwner owner, URI base, SupplierAccess access, SupplierCredentials.ClientCredentials credentials) {
        Token held = tokens.get(owner);
        Instant now = clock.instant();
        if (held != null && now.isBefore(held.expires())) {
            return held.value();
        }
        String form = "grant_type=client_credentials&client_id=" + encoded(credentials.clientIdentifier())
                + "&client_secret=" + encoded(credentials.clientSecret());
        HttpCall call = HttpCall.post(base.resolve(TOKEN_PATH)).header("Accept", JSON).text(form, FORM)
                .timeout(access.timeout()).build();
        JsonNode answer = exchange.answerTo(call, access);
        String issued = SupplierExchange.textOf(answer.path("access_token"));
        if (issued == null) {
            throw new SupplierFailure(SupplierFailure.Reason.UNREADABLE, access.label() + " issued no token.");
        }
        Long lifetime = SupplierExchange.wholeNumberOf(answer.path("expires_in"));
        long seconds = lifetime == null ? USUAL_TOKEN_SECONDS : lifetime;
        tokens.put(owner, new Token(issued, now.plusSeconds(Math.max(0, seconds - EXPIRY_MARGIN_SECONDS))));
        return issued;
    }

    private SupplierOffer offerOf(JsonNode product) {
        JsonNode variation = product.path("ProductVariations").path(0);
        JsonNode description = product.path("Description");
        return new SupplierOffer(kind(), SupplierExchange.textOf(variation.path("DigiKeyProductNumber")),
                SupplierExchange.textOf(product.path("ManufacturerProductNumber")),
                SupplierExchange.textOf(product.path("Manufacturer").path("Name")),
                SupplierExchange.textOf(description.path("ProductDescription")),
                SupplierExchange.textOf(description.path("DetailedDescription")),
                SupplierExchange.textOf(product.path("Category").path("Name")),
                SupplierExchange.textOf(product.path("ProductStatus").path("Status")), null,
                SupplierExchange.wholeNumberOf(variation.path("QuantityAvailableforPackageType")),
                SupplierExchange.wholeNumberOf(variation.path("MinimumOrderQuantity")), null, locale.get().currency(),
                priceBreaksOf(variation.path("StandardPricing")), SupplierExchange.textOf(product.path("ProductUrl")),
                SupplierExchange.textOf(product.path("DatasheetUrl")), SupplierExchange.textOf(product.path("PhotoUrl")));
    }

    /** A break missing either half cannot be shown or compared: it is dropped, never defaulted to a price of zero. */
    private static List<PriceBreak> priceBreaksOf(JsonNode pricing) {
        List<PriceBreak> priceBreaks = new ArrayList<>();
        for (JsonNode written : pricing) {
            Long quantity = SupplierExchange.wholeNumberOf(written.path("BreakQuantity"));
            BigDecimal price = SupplierExchange.decimalOf(written.path("UnitPrice"));
            if (quantity != null && price != null) {
                priceBreaks.add(new PriceBreak(quantity, price));
            }
        }
        return priceBreaks;
    }

    private static String encoded(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String digestOf(String secret) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is required of every Java runtime.", impossible);
        }
    }
}
