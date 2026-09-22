package org.jmouse.grabber.smoke;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.util.List;

import org.jmouse.core.matcher.TextMatchers;
import org.jmouse.grabber.Attributes;
import org.jmouse.grabber.GrabListener;
import org.jmouse.grabber.GrabRun;
import org.jmouse.grabber.Grabber;
import org.jmouse.grabber.ItemSink;
import org.jmouse.grabber.extract.Extraction;
import org.jmouse.grabber.extract.FieldRule;

/**
 * 🧪 The whole module in one run: two routes, a baton, extraction into records, and a sink.
 *
 * <p>Run its {@code main} from the reactor root. It touches no network.</p>
 */
public final class GrabberSmoke {

    /**
     * 📦 What a product page produces. A record, bound by component name.
     */
    public record Product(String title, BigDecimal price, String sku, List<URI> images, String category) {
    }

    /**
     * 📦 What one card on a listing page produces.
     */
    public record Card(String title, URI address, BigDecimal price, String sku) {
    }

    private static final Extraction<Card> CARD = Extraction.into(Card.class)
            .each(".product-card")
            .field("title", FieldRule.css("a.product-link").text())
            .field("address", FieldRule.css("a.product-link").link().as(URI.class))
            .field("price", FieldRule.css(".price").text().as(BigDecimal.class))
            .field("sku", FieldRule.self().attribute("data-sku"));

    private static final Extraction<Product> PRODUCT = Extraction.into(Product.class)
            .field("title", FieldRule.css("h1.title").text())
            .field("price", FieldRule.css(".price").text().as(BigDecimal.class))
            .field("sku", FieldRule.css(".sku").attribute("data-sku"))
            .field("images", FieldRule.css(".gallery img").link("src").as(URI.class).many());

    public static void main(String[] arguments) {
        ItemSink.Collecting        collected = ItemSink.collecting();
        GrabListener.Recording     watched   = new GrabListener.Recording();

        Grabber grabber = Grabber.builder()
                .fetcher(Fixtures.fetcher())
                .policy(policy -> policy
                        .concurrency(4)
                        .delay(Duration.ZERO)
                        .maximumDepth(3))
                .seed(Fixtures.CATALOG)
                .route("listing", TextMatchers.contains("/catalog"), page -> {
                    page.extractAll(CARD).forEach(page::emit);

                    return page.follow(
                            page.links("a.product-link, .pagination a.next"),
                            null,
                            Attributes.of("category", page.text("h1").orElse("unknown")));
                })
                .route("product", TextMatchers.contains("/product/"), page -> {
                    // ⚠️ The category came off the LISTING page — the product page's own markup does
                    // not carry it, and this is what the baton exists for.
                    page.extract(PRODUCT)
                            .map(product -> new Product(
                                    product.title(), product.price(), product.sku(), product.images(),
                                    page.attributes().text("category", "unknown")))
                            .ifPresent(page::emit);

                    return page.leaf();
                })
                .into(collected)
                .listener(watched)
                .listener(GrabListener.logging())
                .build();

        GrabRun run = grabber.run();

        System.out.println("== run ==");
        System.out.println(run);

        System.out.println("== cards ==");
        collected.itemsOf(Card.class).forEach(card -> System.out.println("  " + card));

        System.out.println("== products ==");
        collected.itemsOf(Product.class).forEach(product -> System.out.println("  " + product));

        System.out.println("== checks ==");
        check("both catalogue pages fetched", watched.fetchedVisits().size() == 5, watched.fetchedVisits().size());
        check("three cards extracted", collected.itemsOf(Card.class).size() == 3, collected.itemsOf(Card.class).size());
        check("three products extracted", collected.itemsOf(Product.class).size() == 3,
              collected.itemsOf(Product.class).size());
        check("the baton reached the product pages",
              collected.itemsOf(Product.class).stream().allMatch(product -> "Catalog".equals(product.category())),
              collected.itemsOf(Product.class).stream().map(Product::category).toList());
        check("a tracking parameter did not make a second page",
              watched.fetchedVisits().stream().noneMatch(visit -> visit.address().toString().contains("utm_")),
              watched.fetchedVisits().stream().map(visit -> visit.address().toString()).toList());
        check("nothing failed", watched.failures().isEmpty(), watched.failures());
    }

    private static void check(String what, boolean held, Object saw) {
        System.out.println((held ? "  OK   " : "  FAIL ") + what + (held ? "" : " — saw " + saw));
    }

}
