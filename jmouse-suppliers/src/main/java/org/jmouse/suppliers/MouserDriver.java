package org.jmouse.suppliers;

import org.jmouse.http.client.HttpCall;
import org.jmouse.http.client.HttpTransport;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 🏬 Mouser's keyword search.
 *
 * <p>⚠️ <strong>The key is a query parameter, and getting that wrong looks nothing like an authentication error.</strong>
 * Mouser's gateway routes on it, so a request carrying it as a header resolves to no route at all and is answered
 * <em>404 — No HTTP resource was found</em> with a perfectly valid key. It also means the key is in the address: the
 * transport this runs over must not follow a redirect, and a message must not quote the address.</p>
 */
public final class MouserDriver implements SupplierDriver {

    private static final URI ADDRESS = URI.create("https://api.mouser.com");
    private static final String SEARCH_PATH = "/api/v1/search/keyword";
    private static final int MOST_RECORDS = 50;
    private static final String JSON = "application/json";

    private final SupplierExchange exchange;

    public MouserDriver(HttpTransport transport) {
        this.exchange = new SupplierExchange(transport);
    }

    @Override
    public String kind() {
        return "mouser";
    }

    @Override
    public String name() {
        return "Mouser";
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
        if (!(access.credentials() instanceof SupplierCredentials.ApiKey credentials) || !credentials.complete()) {
            throw new SupplierFailure(SupplierFailure.Reason.NOT_CONFIGURED, access.label() + " has no API key.");
        }
        String request = SupplierExchange.json(Map.of("SearchByKeywordRequest", Map.of("keyword", query,
                "records", Math.min(limit, MOST_RECORDS), "startingIndex", 0)));
        HttpCall call = HttpCall.post(searchAddress(access, credentials)).header("Accept", JSON).text(request, JSON)
                .timeout(access.timeout()).build();
        JsonNode answer = exchange.answerTo(call, access);
        refuseOnReportedErrors(answer, access);
        List<SupplierOffer> offers = new ArrayList<>();
        for (JsonNode part : answer.path("SearchResults").path("Parts")) {
            offers.add(offerOf(part));
        }
        return List.copyOf(offers);
    }

    private static URI searchAddress(SupplierAccess access, SupplierCredentials.ApiKey credentials) {
        URI base = access.address() == null ? ADDRESS : access.address();
        return base.resolve(SEARCH_PATH + "?apiKey=" + URLEncoder.encode(credentials.key(), StandardCharsets.UTF_8));
    }

    /** Mouser may answer 200 with its errors listed beside no results: that is a refusal, not an empty search. */
    private static void refuseOnReportedErrors(JsonNode answer, SupplierAccess access) {
        JsonNode errors = answer.path("Errors");
        if (errors.isArray() && !errors.isEmpty()) {
            String words = SupplierExchange.textOf(errors.path(0).path("Message"));
            throw new SupplierFailure(SupplierFailure.Reason.REFUSED, access.label() + " refused the search: "
                    + access.credentials().withoutSecrets(words == null ? "no reason given." : words));
        }
    }

    private SupplierOffer offerOf(JsonNode part) {
        JsonNode breaks = part.path("PriceBreaks");
        return new SupplierOffer(kind(), SupplierExchange.textOf(part.path("MouserPartNumber")),
                SupplierExchange.textOf(part.path("ManufacturerPartNumber")),
                SupplierExchange.textOf(part.path("Manufacturer")), SupplierExchange.textOf(part.path("Description")), null,
                SupplierExchange.textOf(part.path("Category")), SupplierExchange.textOf(part.path("LifecycleStatus")),
                SupplierExchange.textOf(part.path("ROHSStatus")),
                SupplierExchange.digitsOf(SupplierExchange.textOf(part.path("Availability"))),
                SupplierExchange.wholeNumberOf(part.path("MinimumOrderQuantity")),
                SupplierExchange.textOf(part.path("LeadTime")), currencyOf(breaks), priceBreaksOf(breaks),
                SupplierExchange.textOf(part.path("ProductDetailUrl")), SupplierExchange.textOf(part.path("DataSheetUrl")),
                SupplierExchange.textOf(part.path("ImagePath")));
    }

    /** The currency Mouser names on its breaks; it is the account's, so nothing is assumed where it names none. */
    private static String currencyOf(JsonNode breaks) {
        return SupplierExchange.textOf(breaks.path(0).path("Currency"));
    }

    /** A break missing either half cannot be shown or compared: it is dropped, never defaulted to a price of zero. */
    private static List<PriceBreak> priceBreaksOf(JsonNode breaks) {
        List<PriceBreak> priceBreaks = new ArrayList<>();
        for (JsonNode written : breaks) {
            Long quantity = SupplierExchange.wholeNumberOf(written.path("Quantity"));
            BigDecimal price = WrittenPrice.parse(SupplierExchange.textOf(written.path("Price")));
            if (quantity != null && price != null) {
                priceBreaks.add(new PriceBreak(quantity, price));
            }
        }
        return priceBreaks;
    }
}
