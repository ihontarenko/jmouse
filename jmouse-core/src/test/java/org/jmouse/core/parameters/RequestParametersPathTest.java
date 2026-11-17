package org.jmouse.core.parameters;

import org.jmouse.core.parameters.RequestParametersPathToken.ArrayAppendToken;
import org.jmouse.core.parameters.RequestParametersPathToken.ArrayIndexToken;
import org.jmouse.core.parameters.RequestParametersPathToken.PropertyNameToken;
import org.jmouse.core.parameters.support.RequestParametersTreeFlattener;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestParametersPathTest {

    private final RequestParametersPathParser parser = new RequestParametersPathParser();
    private final RequestParametersPathWriter writer = new RequestParametersPathWriter();

    @Test
    void readsPropertiesIndicesAndAppends() {
        assertEquals(List.of(new PropertyNameToken("papers"), new ArrayIndexToken(0), new PropertyNameToken("width")),
                     parser.parse("papers[0].width"));
        assertEquals(List.of(new PropertyNameToken("filter"), new PropertyNameToken("admin"), new PropertyNameToken("accesses"),
                             ArrayAppendToken.instance()), parser.parse("filter[admin][accesses][]"));
        assertEquals(List.of(new PropertyNameToken("looks"), new PropertyNameToken("pcb.look")), parser.parse("looks[pcb.look]"));
    }

    @Test
    void digitsAfterADotAreANameInBracketsAnIndex() {
        assertEquals(List.of(new PropertyNameToken("multipliers"), new PropertyNameToken("0")), parser.parse("multipliers.0"));
        assertEquals(List.of(new PropertyNameToken("multipliers"), new ArrayIndexToken(0)), parser.parse("multipliers[0]"));
    }

    @Test
    void digitsTooManyForAnIndexAreAName() {
        assertEquals(List.of(new PropertyNameToken("ids"), new PropertyNameToken("12345678901")), parser.parse("ids[12345678901]"));
    }

    @Test
    void writesThePathItsTokensWereReadFrom() {
        for (String path : List.of("papers[0].width", "files[11].role", "[0].ending", "multipliers.0", "looks[pcb.look].on",
                                   "jobs[1].formats[4]", "groups[]", "lang")) {
            assertEquals(path, writer.write(parser.parse(path)));
        }
    }

    @Test
    void writesOneSpellingForEveryWayOfReadingAPlace() {
        assertEquals("filter.admin.accesses[]", writer.write(parser.parse("filter[admin][accesses][]")));
    }

    @Test
    void refusesANameNoPathCanHold() {
        assertThrows(IllegalArgumentException.class, () -> writer.write(List.of(new PropertyNameToken("a[b"))));
        assertThrows(IllegalArgumentException.class, () -> writer.write(List.of(new PropertyNameToken(" "))));
        assertThrows(IllegalArgumentException.class, () -> writer.write(List.of()));
    }

    @Test
    void flattensATreeIntoNamesThatParseBackIntoIt() {
        Map<String, String[]> parameters = new LinkedHashMap<>();
        parameters.put("lang", new String[]{"uk"});
        parameters.put("ids[1]", new String[]{"444"});
        parameters.put("groups[]", new String[]{"admin"});
        parameters.put("filter[admin][accesses][]", new String[]{"-1"});
        RequestParametersTree tree = new RequestParametersTreeParser().parse(parameters);

        Map<String, List<String>> flattened = new RequestParametersTreeFlattener().flatten(tree);

        assertEquals(Map.of("lang", List.of("uk"), "ids[1]", List.of("444"), "groups[0]", List.of("admin"),
                            "filter.admin.accesses[0]", List.of("-1")), flattened);
    }
}
