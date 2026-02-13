package org.jmouse.web.binding.smoke;

import org.jmouse.core.mapping.Mapper;
import org.jmouse.core.mapping.Mappers;
import org.jmouse.core.mapping.config.MappingConfig;
import org.jmouse.validator.*;
import org.jmouse.web.binding.*;

import java.util.List;
import java.util.Map;

public class SmokeWebBinding {

    public static void main(String... args) {

        // 1) ValidatorRegistry + ValidationProcessor
        DefaultValidatorRegistry registry = new DefaultValidatorRegistry();
        registry.register(Validator.forInstance(UserForm.class, (form, errors) -> {
            if (form.name == null || form.name.isBlank()) {
                errors.rejectValue("name", "notBlank", "name is required");
            }
        }));

        ValidationProcessor validationProcessor = ValidationProcessors.builder()
                .validatorRegistry(registry)
                .validationPolicy(ValidationPolicy.COLLECT_ALL)
                .build();

        // 2) Errors infrastructure
        ErrorsFactory errorsFactory = new DefaultErrorsFactory();

        // 3) Binding context scope (THE LINK binder -> plugin)
        BindingContextScope bindingScope = new BindingContextScope();

        // 4) Mapping plugin (GLOBAL) - now reads BindingContextScope, not errorsScope::get
        BindingMappingPlugin bindingPlugin = new BindingMappingPlugin(
                bindingScope,
                List.of(
                        new FailureToErrorsProcessor(new DefaultMappingFailureTranslator()),
                        new ValidationBindingProcessor(validationProcessor)
                )
        );

        // 5) Build mapper with GLOBAL plugin
        Mapper mapper = Mappers.builder()
                .config(MappingConfig.builder()
                                .plugins(List.of(bindingPlugin))
                                .build())
                .build();

        // 6) DataBinder (mapper singleton) - opens BindingContextScope per bind() call
        ParametersDataBinder binder = new ParametersDataBinder(
                mapper,
                errorsFactory,
                bindingScope
        );

        // 7) Input (already java map)
        Map<String, Object> input = Map.of(
                "id", "555",
                "name", ""
        );

        BindingResult<UserForm> result = binder.bind(
                input,
                UserForm.class,
                "userForm",
                ValidationHints.empty()
        );

        System.out.println("Target: " + result.target());
        System.out.println("Has errors: " + result.hasErrors());

        System.out.println("--- Field errors ---");
        for (var e : result.errors().getErrors()) {
            System.out.println(e.getField() + " | " + e.getCodes()[0] + " | " + e.getDefaultMessage());
        }

        System.out.println("--- Global errors ---");
        for (var e : result.errors().getGlobalErrors()) {
            System.out.println(e.getCodes()[0] + " | " + e.getDefaultMessage());
        }
    }

    // simple DTO for smoke
    public static class UserForm {
        public int id;
        public String name;

        @Override
        public String toString() {
            return "UserForm{id=" + id + ", name='" + name + "'}";
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
