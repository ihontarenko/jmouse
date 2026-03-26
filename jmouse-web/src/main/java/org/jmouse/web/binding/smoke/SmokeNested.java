package org.jmouse.web.binding.smoke;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.jmouse.core.context.ContextScope;
import org.jmouse.core.mapping.Mapper;
import org.jmouse.core.mapping.Mappers;
import org.jmouse.core.mapping.binding.TypeMappingRegistry;
import org.jmouse.core.mapping.binding.annotation.AnnotationRuleSource;
import org.jmouse.core.mapping.binding.annotation.MappingReference;
import org.jmouse.core.mapping.config.MappingConfig;
import org.jmouse.validator.*;
import org.jmouse.validator.jsr380.Jsr380Support;
import org.jmouse.validator.jsr380.StrongPassword;
import org.jmouse.web.binding.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SmokeNested {

    public static void main(String... args) {

        // 1) ValidatorRegistry + ValidationProcessor
        ValidatorRegistry registry = new DefaultValidatorRegistry();

/*        registry.register(Validator.forInstance(SearchRequestForm.class, (form, errors) -> {
            if (form.getFilter() == null) {
                errors.rejectValue("filter", "notNull", "filter is required");
                return;
            }

            if (form.getFilter().getSearch() == null) {
                errors.rejectValue("filter.search", "notNull", "search is required");
                return;
            }

            if (form.getFilter().getSearch().getUser() == null) {
                errors.rejectValue("filter.search.user", "notNull", "user is required");
                return;
            }

            UserCriteriaForm user = form.getFilter().getSearch().getUser();

            if (user.getName() == null || user.getName().isBlank()) {
                errors.rejectValue("filter.search.user.name", "notBlank", "user name is required");
            }

            if (user.getEmail() == null || !user.getEmail().contains("@")) {
                errors.rejectValue("filter.search.user.email", "email", "user email is invalid");
            }
        }));*/

        Jsr380Support.registerInto(registry);

        ValidationProcessor validationProcessor = ValidationProcessors.builder()
                .validatorRegistry(registry)
                .validationPolicy(ValidationPolicy.COLLECT_ALL)
                .build();

        // 2) Errors infrastructure
        ErrorsFactory errorsFactory = new DefaultErrorsFactory();

        // 3) Binding context scope
        ContextScope<BindingContext> bindingScope = new ContextScope<>();

        // 4) Mapping plugin
        BindingMappingPlugin bindingPlugin = new BindingMappingPlugin(
                bindingScope,
                List.of(
                        new FailureToErrorsProcessor(new DefaultMappingFailureTranslator()),
                        new ValidationBindingProcessor(validationProcessor)
                )
        );

        // 5) Build mapper with global plugin
        Mapper mapper = Mappers.builder()
                .config(MappingConfig.builder()
                                .plugins(List.of(bindingPlugin))
                                .build())
                .mappingRegistry(TypeMappingRegistry.builder()
                                         .ruleSource(new AnnotationRuleSource())
                                         .build())
                .build();

        // 6) DataBinder
        ParametersDataBinder binder = new ParametersDataBinder(
                mapper,
                errorsFactory,
                bindingScope
        );

        // 7) Nested input
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("page", "1");

        Map<String, Object> filter = new LinkedHashMap<>();
        Map<String, Object> search = new LinkedHashMap<>();
        Map<String, Object> user = new LinkedHashMap<>();

        user.put("name", "");
        user.put("email", "invalid-email");
        user.put("statusCode", "STATUS_CODE");

        search.put("query", "resistor");
        search.put("user", user);

        filter.put("search", search);
        input.put("filter", filter);

        BindingResult<SearchRequestForm> result = binder.bind(
                input,
                SearchRequestForm.class,
                "searchRequest",
                Hints.empty()
        );

        System.out.println("Target: " + result.target());
        System.out.println("Has errors: " + result.hasErrors());

        if (result.target() != null
                && result.target().getFilter() != null
                && result.target().getFilter().getSearch() != null
                && result.target().getFilter().getSearch().getUser() != null) {

            System.out.println("--- Nested values ---");
            System.out.println("query = " + result.target().getFilter().getSearch().getQuery());
            System.out.println("user.name = " + result.target().getFilter().getSearch().getUser().getName());
            System.out.println("user.email = " + result.target().getFilter().getSearch().getUser().getEmail());
        }

        System.out.println("--- Field errors ---");
        for (var e : result.errors().getErrors()) {
            System.out.println(e.getField() + " | " + e.getCodes()[0] + " | " + e.getDefaultMessage());
        }

        System.out.println("--- Global errors ---");
        for (var e : result.errors().getGlobalErrors()) {
            System.out.println(e.getCodes()[0] + " | " + e.getDefaultMessage());
        }
    }

    public static class SearchRequestForm {
        private int page;
        @Valid
        private FilterForm filter;

        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = page;
        }

        public FilterForm getFilter() {
            return filter;
        }

        public void setFilter(FilterForm filter) {
            this.filter = filter;
        }

        @Override
        public String toString() {
            return "SearchRequestForm{" +
                    "page=" + page +
                    ", filter=" + filter +
                    '}';
        }
    }

    public static class FilterForm {

        @Valid
        private SearchForm search;

        public SearchForm getSearch() {
            return search;
        }

        public void setSearch(SearchForm search) {
            this.search = search;
        }

        @Override
        public String toString() {
            return "FilterForm{" +
                    "search=" + search +
                    '}';
        }
    }

    public static class SearchForm {
        private String query;

        @Valid
        private UserCriteriaForm user;

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public UserCriteriaForm getUser() {
            return user;
        }

        public void setUser(UserCriteriaForm user) {
            this.user = user;
        }

        @Override
        public String toString() {
            return "SearchForm{" +
                    "query='" + query + '\'' +
                    ", user=" + user +
                    '}';
        }
    }

    public static class UserCriteriaForm {

        @StrongPassword(/*groups = {CreateOp.class}*/)
        @NotBlank
        private String name;

        @Email
        private String email;

        private String status;

        public String getStatus() {
            return status;
        }

        @MappingReference("statusCode")
        public void setStatus(String status) {
            this.status = status;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        @Override
        public String toString() {
            return "UserCriteriaForm{" +
                    "name='" + name + '\'' +
                    ", email='" + email + '\'' +
                    '}';
        }
    }
}