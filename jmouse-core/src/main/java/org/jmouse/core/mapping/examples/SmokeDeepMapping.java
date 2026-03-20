package org.jmouse.core.mapping.examples;

import org.jmouse.core.access.TypedValue;
import org.jmouse.core.mapping.Mapper;
import org.jmouse.core.mapping.Mappers;
import org.jmouse.core.mapping.binding.TypeMappingRegistry;
import org.jmouse.core.mapping.strategy.MappingStrategyRegistry;
import org.jmouse.core.mapping.strategy.direct.TypeMapperStrategyContributor;
import org.jmouse.core.mapping.typed.TypeMapper;
import org.jmouse.core.reflection.InferredType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class SmokeDeepMapping {

    public static void main(String... arguments) {
        Mapper mapper = Mappers.builder()
                .registry(TypeMappingRegistry.builder()
                                  .mapping(AddressFlat.class, AddressView.class, mapping -> mapping
                                          .property("line1", property -> property.reference("street"))
                                          .property("postalCode", property -> property.reference("zip"))
                                  )
                                  .mapping(AddressView.class, AddressFlat.class, mapping -> mapping
                                          .property("street", property -> property.reference("line1"))
                                          .property("zip", property -> property.reference("postalCode"))
                                  )
                                  .mapping(OrderLineSource.class, OrderLineTarget.class, mapping -> mapping
                                          .property("productCode", property -> property.reference("sku"))
                                          .property("qty", property -> property.reference("quantity"))
                                          .property("lineAmount", property -> property.reference("price"))
                                  )
                                  .mapping(OrderLineTarget.class, OrderLineSource.class, mapping -> mapping
                                          .property("sku", property -> property.reference("productCode"))
                                          .property("quantity", property -> property.reference("qty"))
                                          .property("price", property -> property.reference("lineAmount"))
                                  )
                                  .mapping(OrderSource.class, OrderTarget.class, mapping -> mapping
                                          .property("orderId", property -> property.reference("id"))
                                          .property("orderedAt", property -> property.reference("createdAt"))
                                          .property("customer", property -> property.reference("buyer"))
                                          .property("shipping", property -> property.reference("deliveryAddress"))
                                          .property("lines", property -> property.reference("items"))
                                          .property("status", property -> property.constant("CREATED"))
                                  )
                                  .mapping(OrderTarget.class, OrderSource.class, mapping -> mapping
                                          .property("id", property -> property.reference("orderId"))
                                          .property("createdAt", property -> property.reference("orderedAt"))
                                          .property("buyer", property -> property.reference("customer"))
                                          .property("deliveryAddress", property -> property.reference("shipping"))
                                          .property("items", property -> property.reference("lines"))
                                  )
                                  .build())
                .strategyRegistry(
                        new MappingStrategyRegistry(Mappers.DEFAULT_CONTRIBUTORS)
                                .register(new TypeMapperStrategyContributor(
                                        new BuyerToCustomerMapper(),
                                        new CustomerToBuyerMapper()
                                ))
                )
                .build();

        OrderSource source = new OrderSource();
        source.setId(1001L);
        source.setCreatedAt(Instant.now());

        BuyerSource buyer = new BuyerSource();
        buyer.setFirstName("John");
        buyer.setLastName("Doe");
        buyer.setEmail("john.doe@gmail.com");
        source.setBuyer(buyer);

        AddressFlat address = new AddressFlat();
        address.setCountry("Ukraine");
        address.setCity("Kyiv");
        address.setStreet("Khreshchatyk 1");
        address.setZip("01001");
        source.setDeliveryAddress(address);

        OrderLineSource line1 = new OrderLineSource();
        line1.setSku("KB-100");
        line1.setQuantity(2);
        line1.setPrice(new BigDecimal("120.50"));

        OrderLineSource line2 = new OrderLineSource();
        line2.setSku("MS-200");
        line2.setQuantity(1);
        line2.setPrice(new BigDecimal("89.99"));

        source.setItems(List.of(line1, line2));

        OrderTarget target = mapper.map(source, OrderTarget.class);
        OrderSource restored = mapper.map(target, OrderSource.class);

        AddressView mappedFromMap = mapper.map(
                Map.of(
                        "country", "Ukraine",
                        "city", "Lviv",
                        "street", "Svobody Ave 10",
                        "zip", "79000"
                ),
                TypedValue.of(AddressView.class)
        );

        Map<String, Object> sourceAsMap = mapper.map(
                source,
                InferredType.forParametrizedClass(Map.class, String.class, Object.class)
        );

        Map<String, Object> targetAsMap = mapper.map(
                target,
                InferredType.forParametrizedClass(Map.class, String.class, Object.class)
        );

        System.out.println("=== SOURCE ===");
        System.out.println(source);

        System.out.println("\n=== TARGET ===");
        System.out.println(target);

        System.out.println("\n=== RESTORED SOURCE ===");
        System.out.println(restored);

        System.out.println("\n=== MAP -> ADDRESS VIEW ===");
        System.out.println(mappedFromMap);

        System.out.println("\n=== SOURCE -> MAP ===");
        System.out.println(sourceAsMap);

        System.out.println("\n=== TARGET -> MAP ===");
        System.out.println(targetAsMap);
    }

    /**
     * Deep mapper: BuyerSource -> CustomerTarget.
     *
     * Combines flat name fields into a nested target structure.
     */
    public static class BuyerToCustomerMapper implements TypeMapper<BuyerSource, CustomerTarget> {

        @Override
        public Class<BuyerSource> sourceType() {
            return BuyerSource.class;
        }

        @Override
        public Class<CustomerTarget> targetType() {
            return CustomerTarget.class;
        }

        @Override
        public CustomerTarget map(BuyerSource source) {
            CustomerTarget target = new CustomerTarget();
            map(source, target);
            return target;
        }

        @Override
        public void map(BuyerSource source, CustomerTarget target) {
            ProfileTarget profile = new ProfileTarget();
            profile.setFullName(source.getFirstName() + " " + source.getLastName());

            ContactTarget contact = new ContactTarget();
            contact.setPrimaryEmail(source.getEmail());

            target.setProfile(profile);
            target.setContact(contact);
        }

        @Override
        public boolean supportsInPlace() {
            return true;
        }
    }

    /**
     * Reverse deep mapper: CustomerTarget -> BuyerSource.
     */
    public static class CustomerToBuyerMapper implements TypeMapper<CustomerTarget, BuyerSource> {

        @Override
        public Class<CustomerTarget> sourceType() {
            return CustomerTarget.class;
        }

        @Override
        public Class<BuyerSource> targetType() {
            return BuyerSource.class;
        }

        @Override
        public BuyerSource map(CustomerTarget source) {
            BuyerSource target = new BuyerSource();
            map(source, target);
            return target;
        }

        @Override
        public void map(CustomerTarget source, BuyerSource target) {
            String fullName = source.getProfile() != null ? source.getProfile().getFullName() : null;

            if (fullName != null) {
                String[] parts = fullName.trim().split("\\s+", 2);
                target.setFirstName(parts.length > 0 ? parts[0] : null);
                target.setLastName(parts.length > 1 ? parts[1] : null);
            }

            if (source.getContact() != null) {
                target.setEmail(source.getContact().getPrimaryEmail());
            }
        }

        @Override
        public boolean supportsInPlace() {
            return true;
        }
    }

    // =========================================================
    // Source model
    // =========================================================

    public static class OrderSource {

        private Long                  id;
        private Instant               createdAt;
        private BuyerSource           buyer;
        private AddressFlat           deliveryAddress;
        private List<OrderLineSource> items;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }

        public BuyerSource getBuyer() {
            return buyer;
        }

        public void setBuyer(BuyerSource buyer) {
            this.buyer = buyer;
        }

        public AddressFlat getDeliveryAddress() {
            return deliveryAddress;
        }

        public void setDeliveryAddress(AddressFlat deliveryAddress) {
            this.deliveryAddress = deliveryAddress;
        }

        public List<OrderLineSource> getItems() {
            return items;
        }

        public void setItems(List<OrderLineSource> items) {
            this.items = items;
        }

        @Override
        public String toString() {
            return "OrderSource{" +
                    "id=" + id +
                    ", createdAt=" + createdAt +
                    ", buyer=" + buyer +
                    ", deliveryAddress=" + deliveryAddress +
                    ", items=" + items +
                    '}';
        }
    }

    public static class BuyerSource {

        private String firstName;
        private String lastName;
        private String email;

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        @Override
        public String toString() {
            return "BuyerSource{" +
                    "firstName='" + firstName + '\'' +
                    ", lastName='" + lastName + '\'' +
                    ", email='" + email + '\'' +
                    '}';
        }
    }

    public static class AddressFlat {

        private String country;
        private String city;
        private String street;
        private String zip;

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getStreet() {
            return street;
        }

        public void setStreet(String street) {
            this.street = street;
        }

        public String getZip() {
            return zip;
        }

        public void setZip(String zip) {
            this.zip = zip;
        }

        @Override
        public String toString() {
            return "AddressFlat{" +
                    "country='" + country + '\'' +
                    ", city='" + city + '\'' +
                    ", street='" + street + '\'' +
                    ", zip='" + zip + '\'' +
                    '}';
        }
    }

    public static class OrderLineSource {

        private String     sku;
        private int        quantity;
        private BigDecimal price;

        public String getSku() {
            return sku;
        }

        public void setSku(String sku) {
            this.sku = sku;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }

        @Override
        public String toString() {
            return "OrderLineSource{" +
                    "sku='" + sku + '\'' +
                    ", quantity=" + quantity +
                    ", price=" + price +
                    '}';
        }
    }

    // =========================================================
    // Target model
    // =========================================================

    public static class OrderTarget {

        private Long                   orderId;
        private Date                   orderedAt;
        private CustomerTarget         customer;
        private AddressView            shipping;
        private List<OrderLineTarget>  lines;
        private String                 status;

        public Long getOrderId() {
            return orderId;
        }

        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }

        public Date getOrderedAt() {
            return orderedAt;
        }

        public void setOrderedAt(Date orderedAt) {
            this.orderedAt = orderedAt;
        }

        public CustomerTarget getCustomer() {
            return customer;
        }

        public void setCustomer(CustomerTarget customer) {
            this.customer = customer;
        }

        public AddressView getShipping() {
            return shipping;
        }

        public void setShipping(AddressView shipping) {
            this.shipping = shipping;
        }

        public List<OrderLineTarget> getLines() {
            return lines;
        }

        public void setLines(List<OrderLineTarget> lines) {
            this.lines = lines;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        @Override
        public String toString() {
            return "OrderTarget{" +
                    "orderId=" + orderId +
                    ", orderedAt=" + orderedAt +
                    ", customer=" + customer +
                    ", shipping=" + shipping +
                    ", lines=" + lines +
                    ", status='" + status + '\'' +
                    '}';
        }
    }

    public static class CustomerTarget {

        private ProfileTarget profile;
        private ContactTarget contact;

        public ProfileTarget getProfile() {
            return profile;
        }

        public void setProfile(ProfileTarget profile) {
            this.profile = profile;
        }

        public ContactTarget getContact() {
            return contact;
        }

        public void setContact(ContactTarget contact) {
            this.contact = contact;
        }

        @Override
        public String toString() {
            return "CustomerTarget{" +
                    "profile=" + profile +
                    ", contact=" + contact +
                    '}';
        }
    }

    public static class ProfileTarget {

        private String fullName;

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        @Override
        public String toString() {
            return "ProfileTarget{" +
                    "fullName='" + fullName + '\'' +
                    '}';
        }
    }

    public static class ContactTarget {

        private String primaryEmail;

        public String getPrimaryEmail() {
            return primaryEmail;
        }

        public void setPrimaryEmail(String primaryEmail) {
            this.primaryEmail = primaryEmail;
        }

        @Override
        public String toString() {
            return "ContactTarget{" +
                    "primaryEmail='" + primaryEmail + '\'' +
                    '}';
        }
    }

    public static class AddressView {

        private String country;
        private String city;
        private String line1;
        private String postalCode;

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getLine1() {
            return line1;
        }

        public void setLine1(String line1) {
            this.line1 = line1;
        }

        public String getPostalCode() {
            return postalCode;
        }

        public void setPostalCode(String postalCode) {
            this.postalCode = postalCode;
        }

        @Override
        public String toString() {
            return "AddressView{" +
                    "country='" + country + '\'' +
                    ", city='" + city + '\'' +
                    ", line1='" + line1 + '\'' +
                    ", postalCode='" + postalCode + '\'' +
                    '}';
        }
    }

    public static class OrderLineTarget {

        private String     productCode;
        private int        qty;
        private BigDecimal lineAmount;

        public String getProductCode() {
            return productCode;
        }

        public void setProductCode(String productCode) {
            this.productCode = productCode;
        }

        public int getQty() {
            return qty;
        }

        public void setQty(int qty) {
            this.qty = qty;
        }

        public BigDecimal getLineAmount() {
            return lineAmount;
        }

        public void setLineAmount(BigDecimal lineAmount) {
            this.lineAmount = lineAmount;
        }

        @Override
        public String toString() {
            return "OrderLineTarget{" +
                    "productCode='" + productCode + '\'' +
                    ", qty=" + qty +
                    ", lineAmount=" + lineAmount +
                    '}';
        }
    }
}