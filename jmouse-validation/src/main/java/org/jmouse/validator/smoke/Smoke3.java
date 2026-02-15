package org.jmouse.validator.smoke;

import org.jmouse.core.mapping.Mapper;
import org.jmouse.core.mapping.Mappers;
import org.jmouse.el.ExpressionLanguage;

import org.jmouse.validator.*;
import org.jmouse.validator.constraint.adapter.core.ConstraintSchemaValidator;
import org.jmouse.validator.constraint.adapter.el.ConstraintELModule;
import org.jmouse.validator.constraint.adapter.el.ConstraintExpressionSupport;
import org.jmouse.validator.constraint.api.Constraint;
import org.jmouse.validator.constraint.constraint.MinMaxConstraint;
import org.jmouse.validator.constraint.constraint.OneOfConstraint;
import org.jmouse.validator.constraint.dsl.ConstraintSchemas;
import org.jmouse.validator.constraint.handler.ConstraintHandler;
import org.jmouse.validator.constraint.handler.SchemaSelector;
import org.jmouse.validator.constraint.model.ConstraintSchema;
import org.jmouse.validator.constraint.processor.ConstraintProcessor;
import org.jmouse.validator.constraint.registry.ConstraintTypeRegistry;
import org.jmouse.validator.constraint.registry.InMemoryConstraintSchemaRegistry;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Smoke3 {

    public enum SchemaHint { PROGRAMMATIC, EXPRESSION }

    public static void main(String[] args) {

        Map<String, Object> form = new LinkedHashMap<>();
        form.put("lang", "ru"); // invalid for uk/en
        form.put("filter", Map.of(
                "admin", Map.of(
                        "accesses", List.of("-5") // invalid for min >= -1
                )
        ));

        // -------- constraint infra
        ConstraintProcessor constraintProcessor = new ConstraintProcessor();
        ConstraintHandler constraintHandler = new ConstraintHandler(constraintProcessor);

        // -------- schemas registry
        InMemoryConstraintSchemaRegistry schemaRegistry = new InMemoryConstraintSchemaRegistry();
        schemaRegistry.register(buildProgrammaticSchema());
        schemaRegistry.register(buildExpressionSchema());

        // -------- selector
        SchemaSelector selector = (objectName, hints) -> {
            if (!"dynamicForm".equalsIgnoreCase(objectName)) return null;

            SchemaHint hint = (hints == null) ? null : hints.find(SchemaHint.class);
            if (hint == SchemaHint.EXPRESSION) return "dynamicForm.expression";
            return "dynamicForm.programmatic";
        };

        // -------- run #1: PROGRAMMATIC
        runOnce("PROGRAMMATIC", form, schemaRegistry, constraintHandler, selector, ValidationHints.of(SchemaHint.PROGRAMMATIC));

        // -------- run #2: EXPRESSION
        runOnce("EXPRESSION", form, schemaRegistry, constraintHandler, selector, ValidationHints.of(SchemaHint.EXPRESSION));
    }

    private static void runOnce(
            String title,
            Map<String, Object> form,
            InMemoryConstraintSchemaRegistry schemaRegistry,
            ConstraintHandler handler,
            SchemaSelector selector,
            ValidationHints hints
    ) {
        // Important: ConstraintSchemaValidator needs hintsSupplier because Validator#validate(...) doesn't have hints.
        ConstraintSchemaValidator.ValidationHintsSupplier hintsSupplier = () -> hints;

        Validator constraintValidator = new ConstraintSchemaValidator(
                schemaRegistry,
                handler,
                selector,
                hintsSupplier
        );

        DefaultValidatorRegistry registry = new DefaultValidatorRegistry();
        registry.register(constraintValidator);

        ValidationProcessor processor = ValidationProcessors.builder()
                .validatorRegistry(registry)
                .errorsFactory(new DefaultErrorsFactory())
                .validationPolicy(ValidationPolicy.COLLECT_ALL)
                .build();

        ValidationResult<Map<?, ?>> result = processor.validate((Map<?, ?>) form, "dynamicForm", hints);

        System.out.println("\n=== " + title + " ===");
        print(result.errors());
    }

    private static void print(Errors errors) {
        System.out.println("Has errors: " + errors.hasErrors());

        System.out.println("--- Field errors ---");
        errors.getErrors().forEach(e ->
                System.out.println(e.getField() + " | " + e.getCodes()[0] + " | " + e.getDefaultMessage())
        );

        System.out.println("--- Global errors ---");
        errors.getGlobalErrors().forEach(e ->
                System.out.println(e.getCodes()[0] + " | " + e.getDefaultMessage())
        );
    }

    // =========================
    // PROGRAMMATIC schema
    // =========================
    private static ConstraintSchema buildProgrammaticSchema() {

        OneOfConstraint oneOfLang = new OneOfConstraint();
        oneOfLang.setValues(List.of("uk", "en"));
        oneOfLang.setMessage("lang must be uk/en");

        MinMaxConstraint minAccess = new MinMaxConstraint();
        minAccess.setMode(MinMaxConstraint.Mode.MIN);
        minAccess.setMin(BigDecimal.valueOf(-1));
        minAccess.setMessage("must be >= -1 (programmatic)");

        return ConstraintSchemas.builder("dynamicForm.programmatic")
                .field("lang")
                .use(oneOfLang).add()
                .done()
                .field("filter.admin.accesses[0]")
                .use(minAccess).add()
                .done()
                .build();
    }

    // =========================
    // EXPRESSION schema
    // (here we show *two* expr constraints too)
    // =========================
    private static ConstraintSchema buildExpressionSchema() {

        ConstraintTypeRegistry typeRegistry = new ConstraintTypeRegistry()
                .register("minmax", MinMaxConstraint.class)
                .register("oneof", OneOfConstraint.class);

        ExpressionLanguage el = new ExpressionLanguage();
        Mapper mapper = Mappers.defaultMapper();

        ConstraintExpressionSupport support = ConstraintELModule.create(el, typeRegistry, mapper);

        Constraint oneOf = support.parse(
                "@OneOf('values':['uk','en'],'message':'lang must be uk/en (expr)')"
        );

        Constraint minConstraint = support.parse(
                "@MinMax('mode':'min','min':-1,'message':'must be >= -1 (expr)')"
        );

        return ConstraintSchemas.builder("dynamicForm.expression")
                .field("lang")
                .use(oneOf).add()
                .done()
                .field("filter.admin.accesses[0]")
                .use(minConstraint).add()
                .done()
                .build();
    }
}
