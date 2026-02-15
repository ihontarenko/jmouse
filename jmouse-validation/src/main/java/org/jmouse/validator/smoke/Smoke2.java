package org.jmouse.validator.smoke;

import org.jmouse.core.parameters.RequestParametersJavaStructureConverter;
import org.jmouse.core.parameters.RequestParametersJavaStructureOptions;
import org.jmouse.core.parameters.RequestParametersTree;
import org.jmouse.core.parameters.RequestParametersTreeParser;
import org.jmouse.core.parameters.support.RequestParametersTreePrinter;
import org.jmouse.el.ExpressionLanguage;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.expression.FunctionNode;
import org.jmouse.validator.*;
import org.jmouse.validator.dynamic.ValidationSchema;
import org.jmouse.validator.dynamic.ValidationSchemas;

import java.util.LinkedHashMap;
import java.util.List;
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
//
        Map<String,Object> map = parseExpression(expressionLanguage, "max({'class': 'org.jmouse.validator.dynamic.constraints.MinMaxConstraint'|class, 'min':3, 'msg':'range.. bla'})");

        // temporary test for example
        // non prod version
        String validatorName = (String) map.get("name");
        Map<String, Object> arguments = (Map<String, Object>) map.get("arguments");

        System.out.println(validatorName);
    }

    public static Map<String,Object> parseExpression(ExpressionLanguage el, String expression) {
        if (el.compile(expression) instanceof FunctionNode functionNode) {
            String name = functionNode.getName();
            if (functionNode.getArguments().getFirst() instanceof Expression e) {
                if (e.evaluate(el.newContext()) instanceof Map<?,?> map) {
                    return Map.of(
                            "name", name,
                            "arguments", map
                    );
                }
            }
        }

        return null;
    }


    public static ValidationSchema smokeA(ValidatorRegistry registry) {

        ValidationSchema schema = ValidationSchemas.builder("dynamicForm")
                .field("lang")
                    .use("oneOf").argument("values", List.of("uk", "en")).message("lang must be uk/en").add()
                    .done()
                .field("filter.admin.accesses[0]")
                    .use("minMax")
                        .argument("mode", "min")
                        .argument("min", -1)
                        .message("must be >= -1")
                        .add()
                    .done()
                .build();

        return schema;
    }

    public record User(Profile profile, String name) {
    }

    public record Profile(String email) {
    }

}