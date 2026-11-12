package org.jmouse.suppliers;

import java.util.List;

/**
 * What a distributor offers of one part. A field it did not say is null — never zero: a stock of zero is a statement,
 * and so is a price of zero.
 *
 * @param supplier            the driver's {@link SupplierDriver#kind()}
 * @param supplierPartNumber  the distributor's own number for it, the one it is ordered by
 * @param partNumber          the manufacturer's part number
 * @param manufacturer        who makes it
 * @param description         what it says the part is, in a line
 * @param detailedDescription what it says at length
 * @param category            the distributor's category
 * @param status              its life cycle, in the distributor's words: {@code Active}, {@code Obsolete}
 * @param rohs                its RoHS status, in the distributor's words
 * @param stock               how many it holds
 * @param minimumOrder        the fewest it sells
 * @param leadTime            how long it takes to get more, in the distributor's words
 * @param currency            what {@code priceBreaks} are in
 * @param priceBreaks         what one costs, by how many are bought, the fewest first as the distributor lists them
 * @param page                its page on the distributor's site
 * @param datasheet           where its datasheet is
 * @param image               where its picture is
 */
public record SupplierOffer(String supplier, String supplierPartNumber, String partNumber, String manufacturer,
                            String description, String detailedDescription, String category, String status, String rohs,
                            Long stock, Long minimumOrder, String leadTime, String currency, List<PriceBreak> priceBreaks,
                            String page, String datasheet, String image) {

    public SupplierOffer {
        priceBreaks = priceBreaks == null ? List.of() : List.copyOf(priceBreaks);
    }
}
