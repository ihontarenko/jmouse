package org.jmouse.mapper.el.examples;

import org.jmouse.mapper.Mapper;
import org.jmouse.mapper.Mappers;
import org.jmouse.mapper.binding.PropertyMapping;
import org.jmouse.mapper.binding.TypeMappingRegistry;
import org.jmouse.mapper.binding.TypeMappingRule;
import org.jmouse.mapper.el.JmmReader;
import org.jmouse.mapper.el.JmmRuleSource;
import org.jmouse.mapper.el.parser.JmmSyntaxException;
import org.jmouse.mapper.errors.MappingException;

import java.math.BigDecimal;

/**
 * Reads a {@code .jmm} file and maps with it, end to end. 🔬
 *
 * <p>The point is not that the parser runs — it is that what the parser produced reaches the engine as
 * ordinary rules and changes what a mapping does. A file that parses and then influences nothing is the
 * failure this whole language was shaped to avoid.</p>
 */
public class JmmSmoke {

    private static int failures = 0;

    private static final String FILE = """
            mapping "shop/checkout" {

                use org.jmouse.mapper.el.examples.JmmSmoke$OrderRequest
                use org.jmouse.mapper.el.examples.JmmSmoke$Order

                fragment auditing {
                    auditNote : ignore
                }

                target Order {

                    always {
                        include auditing
                        status : "CREATED"
                    }

                    from OrderRequest {
                        reference  : reference | trim | upper
                        buyerName  : firstName ~ " " ~ lastName
                        total      : amount
                        comment    : comment | default("none")
                        secret     : ignore
                    }
                }
            }
            """;

    public static void main(String... arguments) {
        JmmRuleSource rules = new JmmReader().read(FILE, "shop/checkout.jmm");

        report("the file declares one pair", rules.size(), 1);

        Mapper mapper = Mappers.builder()
                .rules(builder -> builder.ruleSource(rules))
                .build();

        OrderRequest request = new OrderRequest();

        request.setReference("  ord-1  ");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setAmount(new BigDecimal("120.50"));
        request.setSecret("do not carry");

        Order order = mapper.map(request, Order.class);

        report("a filter chain runs", order.getReference(), "ORD-1");
        report("a concatenation runs", order.getBuyerName(), "John Doe");
        report("a same-named property still travels", order.getTotal(), new BigDecimal("120.50"));
        report("an always rule applies", order.getStatus(), "CREATED");
        report("default() fills a null", order.getComment(), "none");
        report("ignore is honoured", order.getSecret(), null);
        report("a fragment's ignore is honoured", order.getAuditNote(), null);

        refused("a dotted target is refused",
                "mapping \"x\" { use java.lang.String\n target String { from String { a.b : c } } }",
                "is a path");
        refused("refuse source after is refused",
                "mapping \"x\" { use java.lang.String\n target String { from String { refuse source after { a : \"b\" } } } }",
                "refuse source before");
        refused("a fragment including a fragment is refused",
                "mapping \"x\" { fragment a { include b } }",
                "does not include another fragment");

        refused("a mistyped TARGET property is refused",
                pair("buyerNmae : firstName"), "is not a writable property of Order");
        refused("a mistyped SOURCE path is refused",
                pair("buyerName : firstNmae"), "cannot be read from OrderRequest");
        refused("a let shadowing a source property is refused",
                pair("let comment = 1\nbuyerName : firstName"), "is already a property of OrderRequest");
        refused("unmapped fail names what nothing fills",
                pair("unmapped fail\n", "buyerName : firstName"), "has nothing to fill");

        // ⚠️ The constraint is that a refusal opens the block it guards. What is checked here is not the
        // constraint but the MESSAGE: read as a rule, the same file failed on a colon that never came and
        // named two tokens nobody typed.
        refused("a refusal written after a rule is refused",
                pair("secret : ignore\nrefuse source before { reference is null : \"no reference\" }"),
                "opens the block it guards");
        refused("a second 'refuse source' block is refused",
                pair("refuse source before { reference is null : \"a\" }\n"
                             + "refuse source before { firstName is null : \"b\" }\n"
                             + "buyerName : firstName"),
                "already refuses its source");

        // ⚠️ And the recognition stays narrow. A target property called 'refuse' is ordinary, and is told
        // apart by the colon that always follows a property name — so this must fail as a MISSING
        // PROPERTY, proving the line was read as a rule rather than as a refusal block.
        refused("a property called 'refuse' is still a rule",
                pair("refuse : ignore"), "is not a writable property of Order");

        // ⚠️ A bare `let` name on the right — the shape §6 and §14 of the reference document both use,
        // and the shape that was refused outright: the validator checked it against the source's
        // properties, where a binding is guaranteed not to be.
        JmmRuleSource bound = new JmmReader().read(
                pair("let full = firstName ~ \" \" ~ lastName\n"
                             + "buyerName : full\n"
                             + "reference : full | upper\n"
                             + "total     : amount"), "bound.jmm");

        OrderRequest boundRequest = new OrderRequest();

        boundRequest.setFirstName("John");
        boundRequest.setLastName("Doe");
        boundRequest.setReference("ignored");

        Order boundOrder = Mappers.builder()
                .rules(builder -> builder.ruleSource(bound))
                .build()
                .map(boundRequest, Order.class);

        report("a bare 'let' name on the right resolves", boundOrder.getBuyerName(), "John Doe");
        report("...and the same name inside an expression too", boundOrder.getReference(), "JOHN DOE");

        // ⚠️ The throughput half, observed directly rather than inferred from a value. A bare path in a
        // block that also binds is STILL a Reference — read straight through the source accessor, with
        // no expression tree, no context and no property sweep. It used to become a compiled expression
        // the moment any `let` appeared anywhere in the block, however unrelated.
        TypeMappingRule boundRule = bound.find(OrderRequest.class, Order.class, null);

        report("a plain path in a binding block is still a reference",
               boundRule.find("total") instanceof PropertyMapping.Reference, true);
        report("...and a bound name is not, which is what tells them apart",
               boundRule.find("buyerName") instanceof PropertyMapping.Expression, true);

        // ⚠️ And the check did not simply stop running. A block with a binding must still refuse a typo
        // in a rule that has nothing to do with the binding — which is what "skip the check whenever the
        // block binds anything" would have quietly given up.
        refused("a typo is still refused in a block that binds",
                pair("let full = firstName\nbuyerName : firstNmae"),
                "cannot be read from OrderRequest");

        // ⚠️ A binding is scoped to ONE block, and a fragment is a block. Checked in both directions,
        // because "scoped" is only worth stating if crossing the boundary actually fails: the fragment
        // reaches its own binding, and the block that includes it does not.
        JmmRuleSource scoped = new JmmReader().read(FRAGMENT_BINDING_FILE, "scoped.jmm");

        OrderRequest scopedRequest = new OrderRequest();

        scopedRequest.setFirstName("John");

        Order scopedOrder = Mappers.builder()
                .rules(builder -> builder.ruleSource(scoped))
                .build()
                .map(scopedRequest, Order.class);

        report("a fragment reaches its own binding", scopedOrder.getAuditNote(), "AUDITED");
        refused("...and the block including it does not",
                FRAGMENT_BINDING_LEAK_FILE, "cannot be read from OrderRequest");

        // ⚠️ An ignore takes no condition — documented, and until now enforced only by accident: the
        // reader returned before looking, so the 'when' was read as the NEXT line's property name and
        // failed on a colon that never came, naming two tokens nobody typed.
        refused("'ignore' with a 'when' is refused as an ignore",
                pair("secret : ignore when firstName is null"), "an 'ignore' is unconditional");

        // ⚠️ Two failures raised past the parser, where no cursor exists. Both used to report 0:0 —
        // the check inside refused() is what holds every one of these to naming a line.
        refused("an undeclared fragment names its include line",
                pair("include auditing\nbuyerName : firstName"), "no fragment called 'auditing'");
        refused("a value that will not compile names its rule",
                pair("buyerName : firstName ~~~ lastName"), "cannot compile");

        // ⚠️ And it keeps what the compiler threw. The compiler's MESSAGE was folded into ours and used
        // to be all that survived; the stack under it — the only thing that says where inside the
        // compiler the refusal came from — was dropped on the floor.
        try {
            new JmmReader().read(pair("buyerName : firstName ~~~ lastName"), "probe.jmm");
            report("a refusal keeps what caused it", "parsed", "refused");
        } catch (JmmSyntaxException refusal) {
            report("a refusal keeps what caused it", refusal.getCause() != null, true);
        }

        // ⚠️ A record target is built through components and has not one setter. A check that
        // understood only setters would refuse every record, which is a category rather than an edge.
        JmmRuleSource records = new JmmReader().read(RECORD_FILE, "records.jmm");

        report("a RECORD target is accepted", records.size(), 1);

        // ⚠️ `when` means the property is LEFT ALONE, not set to null. The only way to tell the two
        // apart is to map into an instance that already holds something and check it survived.
        Mapper guarded = Mappers.builder()
                .rules(builder -> builder.ruleSource(new JmmReader().read(GUARD_FILE, "guard.jmm")))
                .build();

        Order held = new Order();

        held.setStatus("UNTOUCHED");
        guarded.map(request, held);

        report("a false 'when' leaves the property alone", held.getStatus(), "UNTOUCHED");

        request.setReference("  big  ");

        Order written = new Order();

        written.setStatus("UNTOUCHED");
        guarded.map(request, written);

        report("a true 'when' writes", written.getStatus(), "BIG");

        Mapper guarding = Mappers.builder()
                .rules(builder -> builder.ruleSource(new JmmReader().read(REFUSE_FILE, "refuse.jmm")))
                .build();

        OrderRequest broken = new OrderRequest();

        broken.setFirstName("John");
        refuses("refuse source before stops the mapping", () -> guarding.map(broken, Order.class),
                "a request with no reference cannot be mapped");

        OrderRequest fine = new OrderRequest();

        fine.setReference("ok");
        fine.setFirstName("John");
        fine.setLastName("Doe");
        fine.setComment("fine");

        report("a passing source maps", guarding.map(fine, Order.class).getBuyerName(), "John Doe");

        Order locked = new Order();

        locked.setStatus("LOCKED");
        refuses("refuse target before sees the supplied instance",
                () -> guarding.map(fine, locked), "a locked order cannot be remapped");

        // ⚠️ This source passes every source assertion — otherwise the mapping would be refused before
        // the target existed, and the 'after' phase would never be reached to be tested.
        OrderRequest commentless = new OrderRequest();

        commentless.setReference("ok");
        commentless.setFirstName("John");
        refuses("refuse target after sees what was built",
                () -> guarding.map(commentless, Order.class), "an order was produced with no comment");

        // ⚠️ Every assertion is evaluated, so one run reports everything wrong rather than one thing.
        OrderRequest doublyBroken = new OrderRequest();

        refuses("both source refusals are reported at once",
                () -> guarding.map(doublyBroken, Order.class), ";");

        System.out.println(failures == 0 ? "ALL PASS" : failures + " FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static final String REFUSE_FILE = """
            mapping "refusals" {
                use org.jmouse.mapper.el.examples.JmmSmoke$OrderRequest
                use org.jmouse.mapper.el.examples.JmmSmoke$Order

                target Order {

                    refuse target before {
                        status == "LOCKED" : "a locked order cannot be remapped"
                    }

                    refuse target after {
                        comment is null : "an order was produced with no comment"
                    }

                    from OrderRequest {
                        refuse source before {
                            reference is null : "a request with no reference cannot be mapped"
                            firstName is null : "a request with no buyer cannot be mapped"
                        }

                        buyerName : firstName ~ " " ~ lastName
                    }
                }
            }
            """;

    /** A fragment that binds a name and uses it — the binding is the fragment's own. */
    private static final String FRAGMENT_BINDING_FILE = """
            mapping "scoped" {
                use org.jmouse.mapper.el.examples.JmmSmoke$OrderRequest
                use org.jmouse.mapper.el.examples.JmmSmoke$Order

                fragment auditing {
                    let mark  = "audited"
                    auditNote : mark | upper
                }

                target Order {
                    always { include auditing }

                    from OrderRequest {
                        buyerName : firstName
                    }
                }
            }
            """;

    /** The same fragment, with the INCLUDING block reaching for the fragment's binding. */
    private static final String FRAGMENT_BINDING_LEAK_FILE = """
            mapping "leak" {
                use org.jmouse.mapper.el.examples.JmmSmoke$OrderRequest
                use org.jmouse.mapper.el.examples.JmmSmoke$Order

                fragment auditing {
                    let mark  = "audited"
                    auditNote : mark
                }

                target Order {
                    always { include auditing }

                    from OrderRequest {
                        buyerName : mark
                    }
                }
            }
            """;

    private static final String GUARD_FILE = """
            mapping "guard" {
                use org.jmouse.mapper.el.examples.JmmSmoke$OrderRequest
                use org.jmouse.mapper.el.examples.JmmSmoke$Order

                target Order {
                    from OrderRequest {
                        status : reference | trim | upper when reference | trim == "big"
                    }
                }
            }
            """;

    private static final String RECORD_FILE = """
            mapping "receipts" {
                use org.jmouse.mapper.el.examples.JmmSmoke$OrderRequest
                use org.jmouse.mapper.el.examples.JmmSmoke$Receipt

                target Receipt {
                    from OrderRequest {
                        who : firstName
                    }
                }
            }
            """;

    /** A one-rule file for the checks, so each case differs by the line being tested and nothing else. */
    private static String pair(String rules) {
        return pair("", rules);
    }

    private static String pair(String targetHeader, String rules) {
        return """
                mapping "probe" {
                    use org.jmouse.mapper.el.examples.JmmSmoke$OrderRequest
                    use org.jmouse.mapper.el.examples.JmmSmoke$Order

                    target Order {
                        %s
                        from OrderRequest {
                            %s
                        }
                    }
                }
                """.formatted(targetHeader, rules);
    }

    private static void report(String what, Object actual, Object expected) {
        boolean passed = expected == null ? actual == null : expected.equals(actual);

        if (!passed) {
            failures++;
        }

        System.out.printf("%s  %-38s -> %s%s%n", passed ? "PASS" : "FAIL", what, actual,
                          passed ? "" : "  (expected " + expected + ")");
    }

    /** Runs a mapping that must be refused, and checks the refusal says what it should. */
    private static void refuses(String what, Runnable mapping, String expected) {
        try {
            mapping.run();
            failures++;
            System.out.printf("FAIL  %-46s -> mapped, and should not have%n", what);
        } catch (MappingException refusal) {
            boolean passed = String.valueOf(refusal.getMessage()).contains(expected);

            if (!passed) {
                failures++;
            }

            System.out.printf("%s  %-46s -> %s%n", passed ? "PASS" : "FAIL", what, refusal.getMessage());
        }
    }

    /**
     * Reads a file that must be refused, and checks the refusal on two counts.
     *
     * <p>⚠️ <strong>Every refusal has to name a line, and that is checked here rather than case by
     * case.</strong> A message positioned at {@code 0:0} reads as a file with no lines in it, and it is
     * what every binder- and validator-stage failure used to say: the parser stamps a position on each
     * node precisely so a later failure can carry it, and the stages past the parser threw it away.
     * Asserting it in one place makes every negative case below enforce it, including the ones nobody
     * has written yet.</p>
     */
    private static void refused(String what, String source, String expected) {
        try {
            new JmmReader().read(source, "probe.jmm");
            failures++;
            System.out.printf("FAIL  %-38s -> parsed, and should not have%n", what);
        } catch (JmmSyntaxException refusal) {
            boolean says   = refusal.getMessage().contains(expected);
            boolean placed = refusal.lineNumber() > 0;

            if (!says || !placed) {
                failures++;
            }

            System.out.printf("%s  %-38s -> %s%s%n", says && placed ? "PASS" : "FAIL", what,
                              refusal.getMessage().replaceAll("\\s+", " "),
                              placed ? "" : "   <- names no line");
        }
    }

    /** A record target — built through its components, with not a setter in sight. */
    public record Receipt(String who) {
    }

    public static class OrderRequest {
        private String     reference;
        private String     firstName;
        private String     lastName;
        private BigDecimal amount;
        private String     comment;
        private String     secret;

        public String getReference() { return reference; }
        public void setReference(String reference) { this.reference = reference; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
    }

    public static class Order {
        private String     reference;
        private String     buyerName;
        private BigDecimal total;
        private String     status;
        private String     comment;
        private String     secret;
        private String     auditNote;

        public String getReference() { return reference; }
        public void setReference(String reference) { this.reference = reference; }
        public String getBuyerName() { return buyerName; }
        public void setBuyerName(String buyerName) { this.buyerName = buyerName; }
        public BigDecimal getTotal() { return total; }
        public void setTotal(BigDecimal total) { this.total = total; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public String getAuditNote() { return auditNote; }
        public void setAuditNote(String auditNote) { this.auditNote = auditNote; }
    }
}
