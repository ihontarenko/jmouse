package org.jmouse.validator.smoke;

import org.jmouse.core.mapping.Mapper;
import org.jmouse.core.mapping.Mappers;
import org.jmouse.core.parameters.RequestParametersJavaStructureConverter;
import org.jmouse.core.parameters.RequestParametersJavaStructureOptions;
import org.jmouse.core.parameters.RequestParametersTree;
import org.jmouse.core.parameters.RequestParametersTreeParser;
import org.jmouse.core.parameters.support.RequestParametersTreePrinter;
import org.jmouse.el.ExpressionLanguage;
import org.jmouse.validator.*;
import org.jmouse.validator.constraint.constraint.MinMaxConstraint;
import org.jmouse.validator.constraint.model.ValidationDefinition;
import org.jmouse.validator.constraint.adapter.el.ValidatorDefinitionParser;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Smoke2 {

    public static void main(String[] args) {
        Map<String, String[]> parameters = new LinkedHashMap<>();
        parameters.put("lang", new String[]{"uk"});
        parameters.put("ids[1]", new String[]{"444"});
        parameters.put("groups[]", new String[]{"admin"});
        parameters.put("filter[admin][accesses][]", new String[]{"-1"});
        parameters.put("filter[admin][user]", new String[]{"-1"});

        RequestParametersTreeParser treeParser = new RequestParametersTreeParser();
        RequestParametersTree tree = treeParser.parse(parameters);

        System.out.println("TREE:");
        System.out.println(RequestParametersTreePrinter.toPrettyString(tree));

        RequestParametersJavaStructureConverter converter =
                new RequestParametersJavaStructureConverter(RequestParametersJavaStructureOptions.defaults());

        Object java = converter.toJavaObject(tree);

        if (java instanceof Map<?,?> map) {
            ((Map<String, Object>)map).put("user", new User(new Profile("profile_email"), "name"));
        }

        ValidatorRegistry registry = new DefaultValidatorRegistry();

        registry.register(Validator.forInstance(Map.class, (map, errors) -> {

        }));

        ValidationProcessor processor = ValidationProcessors.builder()
                .validatorRegistry(registry)     // registry == provider тут автоматично ок
                .errorsFactory(new DefaultErrorsFactory())
                .validationPolicy(ValidationPolicy.COLLECT_ALL)
                .build();

        ValidationResult<Map<?, ?>> result = processor.validate((Map<?, ?>) java, "dynamicForm");


        ExpressionLanguage expressionLanguage = new ExpressionLanguage();

        expressionLanguage.getExtensions()
                .addParser(new ValidatorDefinitionParser());

        Object evaluated = expressionLanguage.evaluate("@MinMax('min':3, 'message':'out of range', 'mode':'min')");

        if (evaluated instanceof ValidationDefinition definition) {


            if (definition.getName().equalsIgnoreCase("minmax")) {
                MinMaxConstraint constraint = getMapper().map(definition, MinMaxConstraint.class);
            }

        }
    }


    public static Mapper getMapper() {
        return Mappers.builder().build();
    }

    public record User(Profile profile, String name) {
    }

    public record Profile(String email) {
    }

}