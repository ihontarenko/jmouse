package org.jmouse.search;

/**
 * 🏷️ A searchable kind, as an interface's type filter describes it.
 *
 * <p>⚠️ The {@code type} here is the same word a {@link SearchHit} carries and the same word a filter
 * sends back. One spelling, three places — which is the only reason a filter can be built generically
 * rather than written out per product.
 */
public record SearchType(String type, String label, String icon) {
}
