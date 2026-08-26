package org.jmouse.mapper.el.examples;

import org.jmouse.core.convert.Conversion;
import org.jmouse.core.convert.ConverterNotFound;
import org.jmouse.core.convert.StandardConversion;
import org.jmouse.el.ExpressionLanguage;
import org.jmouse.mapper.Mapper;
import org.jmouse.mapper.Mappers;
import org.jmouse.mapper.el.JmmBinder;
import org.jmouse.mapper.el.JmmReader;

/**
 * {@code value | via("name")} — a named converter reached from a {@code .jmm} file. 🏷️
 *
 * <h2>⚠️ What this has to prove, and why the end-to-end run is the only proof</h2>
 *
 * <p>A filter that applies a converter is easy to verify in isolation and easy to ship broken: the
 * question is not whether {@code ViaFilter} works, it is whether the converter a <strong>product</strong>
 * registered is the one a <strong>file</strong> reaches. Those are the same object only if the binder's
 * evaluation context carries the same conversion the engine does, and nothing in the type system says
 * so. So the run below registers a converter, writes a file that names it, maps an object, and reads
 * the result.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class ViaFilterProbe {

    private static int checked;
    private static int failures;

    private static final String FILE = """
            mapping "shipping" {

                use org.jmouse.mapper.el.examples.ViaFilterProbe$OrderRequest
                use org.jmouse.mapper.el.examples.ViaFilterProbe$Order

                target Order {
                    from OrderRequest {
                        shipping : deliveryAddress | via("shop.address")
                        billing  : billingAddress | via("shop.address")
                    }
                }
            }
            """;

    private static final String UNKNOWN = """
            mapping "typo" {

                use org.jmouse.mapper.el.examples.ViaFilterProbe$OrderRequest
                use org.jmouse.mapper.el.examples.ViaFilterProbe$Order

                target Order {
                    from OrderRequest {
                        shipping : deliveryAddress | via("shop.addres")
                    }
                }
            }
            """;

    private ViaFilterProbe() {
    }

    public static void main(String... arguments) {
        verifyAFileReachesARegisteredConverter();
        verifyAnUnknownNameIsRefusedWithTheList();
        verifyNullPassesThrough();

        report();
    }

    /**
     * The whole point: a converter registered in Java, named in a file, applied to a mapped object.
     */
    private static void verifyAFileReachesARegisteredConverter() {
        Conversion conversion = conversionWithAddress();
        Mapper     mapper     = mapperReading(FILE, conversion);

        OrderRequest request = new OrderRequest();

        request.setDeliveryAddress(new AddressDto("Kyiv", "Khreshchatyk 1"));
        request.setBillingAddress(new AddressDto("Lviv", "Rynok 2"));

        Order order = mapper.map(request, Order.class);

        equal("the file's converter ran", "Kyiv / Khreshchatyk 1", order.getShipping().printed());
        equal("and it runs wherever it is named", "Lviv / Rynok 2", order.getBilling().printed());
    }

    /**
     * ⚠️ A name reaches this from a file somebody typed, so the refusal is judged on whether it can be
     * read — the list of what would have worked is the whole content of a typo's error message.
     */
    private static void verifyAnUnknownNameIsRefusedWithTheList() {
        Conversion conversion = conversionWithAddress();
        Mapper     mapper     = mapperReading(UNKNOWN, conversion);

        OrderRequest request = new OrderRequest();

        request.setDeliveryAddress(new AddressDto("Kyiv", "Khreshchatyk 1"));

        try {
            mapper.map(request, Order.class);
            fail("a mistyped converter name is refused", "nothing was thrown");
        } catch (RuntimeException thrown) {
            ConverterNotFound refused = notFoundIn(thrown);

            if (refused == null) {
                fail("a mistyped converter name is refused as ConverterNotFound",
                     thrown.getClass().getSimpleName() + ": " + thrown.getMessage());

                return;
            }

            System.out.printf("  ↯ %s%n", refused.getMessage());
            equal("the refusal names what would have worked",
                  true, refused.getMessage().contains("shop.address"));
        }
    }

    /**
     * A null is not handed to a converter — it passes through, so a chain may put {@code default}
     * either side of {@code via} and get the obvious answer.
     */
    private static void verifyNullPassesThrough() {
        Conversion conversion = conversionWithAddress();
        Mapper     mapper     = mapperReading(FILE, conversion);

        OrderRequest request = new OrderRequest();

        request.setBillingAddress(new AddressDto("Lviv", "Rynok 2"));

        Order order = mapper.map(request, Order.class);

        equal("a null is not handed to the converter", null, order.getShipping());
        equal("and the other rule is unaffected", "Lviv / Rynok 2", order.getBilling().printed());
    }

    /**
     * A conversion holding one named converter, and nothing registered for the pair.
     *
     * @return the conversion
     */
    private static Conversion conversionWithAddress() {
        Conversion conversion = new StandardConversion();

        conversion.registerConverter("shop.address", AddressDto.class, Address.class,
                                     dto -> new Address(dto.city() + " / " + dto.street()));

        return conversion;
    }

    /**
     * A mapper reading one file, whose expressions resolve converters against {@code conversion}.
     *
     * @param file       the document
     * @param conversion what its expressions may reach
     * @return the mapper
     */
    private static Mapper mapperReading(String file, Conversion conversion) {
        JmmReader reader = new JmmReader(
                new JmmBinder(new ExpressionLanguage(),
                              Thread.currentThread().getContextClassLoader(),
                              conversion));

        return Mappers.builder()
                .rules(builder -> builder.ruleSource(reader.read(file, "probe.jmm")))
                .build();
    }

    /**
     * The {@link ConverterNotFound} inside a wrapped failure, if there is one.
     *
     * @param thrown what came out of the mapper
     * @return the refusal, or {@code null}
     */
    private static ConverterNotFound notFoundIn(Throwable thrown) {
        for (Throwable cause = thrown; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConverterNotFound refused) {
                return refused;
            }
        }

        return null;
    }

    private static void equal(String what, Object expected, Object actual) {
        checked++;

        if (!java.util.Objects.equals(expected, actual)) {
            failures++;
            System.out.printf("  x %s: expected '%s', got '%s'%n", what, expected, actual);

            return;
        }

        System.out.printf("  + %s%n", what);
    }

    private static void fail(String what, String why) {
        checked++;
        failures++;
        System.out.printf("  x %s: %s%n", what, why);
    }

    private static void report() {
        if (failures == 0) {
            System.out.printf("%n%d checks, ALL PASS%n", checked);

            return;
        }

        System.out.printf("%n%d checks, %d failed%n", checked, failures);
        System.exit(1);
    }

    /** What a request carries. */
    public record AddressDto(String city, String street) {
    }

    /** What the domain carries. */
    public record Address(String printed) {
    }

    /** The source. */
    public static class OrderRequest {

        private AddressDto deliveryAddress;
        private AddressDto billingAddress;

        public AddressDto getDeliveryAddress() {
            return deliveryAddress;
        }

        public void setDeliveryAddress(AddressDto deliveryAddress) {
            this.deliveryAddress = deliveryAddress;
        }

        public AddressDto getBillingAddress() {
            return billingAddress;
        }

        public void setBillingAddress(AddressDto billingAddress) {
            this.billingAddress = billingAddress;
        }
    }

    /** The target. */
    public static class Order {

        private Address shipping;
        private Address billing;

        public Address getShipping() {
            return shipping;
        }

        public void setShipping(Address shipping) {
            this.shipping = shipping;
        }

        public Address getBilling() {
            return billing;
        }

        public void setBilling(Address billing) {
            this.billing = billing;
        }
    }
}
