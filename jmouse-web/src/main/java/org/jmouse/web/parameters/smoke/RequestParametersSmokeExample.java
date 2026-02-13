package org.jmouse.web.parameters.smoke;

import org.jmouse.web.parameters.RequestParametersJavaStructureConverter;
import org.jmouse.web.parameters.RequestParametersJavaStructureOptions;
import org.jmouse.web.parameters.RequestParametersTree;
import org.jmouse.web.parameters.RequestParametersTreeParser;
import org.jmouse.web.parameters.support.RequestParametersTreePrinter;

import java.util.LinkedHashMap;
import java.util.Map;

public final class RequestParametersSmokeExample {

    public static void main(String[] args) {
        Map<String, String[]> parameters = new LinkedHashMap<>();
        parameters.put("lang", new String[]{"uk"});
        parameters.put("ids[1]", new String[]{"444"});
        parameters.put("groups[]", new String[]{"admin"});
        parameters.put("filter[admin][accesses][]", new String[]{"-1"});

        RequestParametersTreeParser treeParser = new RequestParametersTreeParser();
        RequestParametersTree tree = treeParser.parse(parameters);

        System.out.println("TREE:");
        System.out.println(RequestParametersTreePrinter.toPrettyString(tree));

        RequestParametersJavaStructureConverter converter =
                new RequestParametersJavaStructureConverter(RequestParametersJavaStructureOptions.defaults());

        Object java = converter.toJavaObject(tree);



        System.out.println("\nJAVA:");
        System.out.println(java);
    }
}