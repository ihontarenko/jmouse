package org.jmouse.suppliers;

import java.math.BigDecimal;

/**
 * What one costs when at least this many are bought.
 *
 * @param quantity  how many have to be bought
 * @param unitPrice what each then costs, in the offer's currency
 */
public record PriceBreak(long quantity, BigDecimal unitPrice) {
}
