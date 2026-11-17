package org.jmouse.json.patch;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The examples of RFC 6902, appendix A, and what a board's layout is patched with. */
class JsonPatchTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode json(String text) {
        return mapper.readTree(text.replace('\'', '"'));
    }

    private JsonNode patched(String document, String patch) {
        return JsonPatch.of(json(patch)).appliedTo(json(document));
    }

    @Test
    void addsAMemberAndAnElement() {
        assertEquals(json("{'foo':'bar','baz':'qux'}"), patched("{'foo':'bar'}", "[{'op':'add','path':'/baz','value':'qux'}]"));
        assertEquals(json("{'foo':['bar','qux','baz']}"), patched("{'foo':['bar','baz']}", "[{'op':'add','path':'/foo/1','value':'qux'}]"));
        assertEquals(json("{'foo':['bar',['abc','def']]}"), patched("{'foo':['bar']}", "[{'op':'add','path':'/foo/-','value':['abc','def']}]"));
    }

    @Test
    void removesReplacesMovesAndCopies() {
        assertEquals(json("{'foo':'bar'}"), patched("{'baz':'qux','foo':'bar'}", "[{'op':'remove','path':'/baz'}]"));
        assertEquals(json("{'foo':['bar','baz']}"), patched("{'foo':['bar','qux','baz']}", "[{'op':'remove','path':'/foo/1'}]"));
        assertEquals(json("{'baz':'boo','foo':'bar'}"), patched("{'baz':'qux','foo':'bar'}", "[{'op':'replace','path':'/baz','value':'boo'}]"));
        assertEquals(json("{'foo':{'bar':'baz'},'qux':{'corge':'grault','thud':'fred'}}"),
                patched("{'foo':{'bar':'baz','waldo':'fred'},'qux':{'corge':'grault'}}", "[{'op':'move','from':'/foo/waldo','path':'/qux/thud'}]"));
        assertEquals(json("{'foo':['all','cows','eat','grass']}"),
                patched("{'foo':['all','grass','cows','eat']}", "[{'op':'move','from':'/foo/1','path':'/foo/3'}]"));
        assertEquals(json("{'a':[1],'b':[1]}"), patched("{'a':[1]}", "[{'op':'copy','from':'/a','path':'/b'}]"));
        assertEquals(json("[1,2]"), patched("{'a':1}", "[{'op':'replace','path':'','value':[1,2]}]"));
    }

    @Test
    void escapesSlashesAndTildesInNames() {
        assertEquals(json("{'a/b':1,'m~n':2}"), patched("{'a/b':0,'m~n':0}",
                "[{'op':'replace','path':'/a~1b','value':1},{'op':'replace','path':'/m~0n','value':2}]"));
    }

    @Test
    void refusesWholeAndLeavesTheDocumentAsItWas() {
        JsonNode document = json("{'baz':'qux','foo':['a',2,'c']}");
        JsonPatch patch = JsonPatch.of(json("[{'op':'replace','path':'/baz','value':'boo'},{'op':'test','path':'/foo/1','value':'b'}]"));
        assertThrows(JsonPatchRefusal.class, () -> patch.appliedTo(document));
        assertEquals(json("{'baz':'qux','foo':['a',2,'c']}"), document);
        assertThrows(JsonPatchRefusal.class, () -> patched("{'foo':'bar'}", "[{'op':'add','path':'/baz/bat','value':'qux'}]"));
        assertThrows(JsonPatchRefusal.class, () -> patched("{'foo':[1]}", "[{'op':'add','path':'/foo/01','value':2}]"));
        assertThrows(JsonPatchRefusal.class, () -> patched("{'foo':[1]}", "[{'op':'remove','path':'/foo/1'}]"));
        assertThrows(JsonPatchRefusal.class, () -> patched("{'foo':1}", "[{'op':'remove','path':'/bar'}]"));
        assertThrows(JsonPatchRefusal.class, () -> patched("{'foo':1}", "[{'op':'jump','path':'/foo'}]"));
    }

    @Test
    void theDifferenceBetweenTwoDocumentsLeadsFromOneToTheOtherAndIsWrittenAsItIsRead() {
        String[][] pairs = {
                {"{'a':[1,2,3]}", "{'a':[1,9]}"},
                {"{'a':[1]}", "{'a':[1,2,{'b':'c'}]}"},
                {"{'a/b':1,'m~n':[1]}", "{'a/b':2,'m~n':[]}"},
                {"{'gone':1,'kept':null}", "{'kept':false,'come':[1]}"},
                {"[1,'two']", "{'now':'an object'}"},
                {"{'tracks':[{'points':[[0,0],[1,1]]}]}", "{'tracks':[{'points':[[0,0],[1,1]]}]}"},
        };
        for (String[] pair : pairs) {
            JsonNode before = json(pair[0]);
            JsonNode after = json(pair[1]);
            JsonPatch difference = JsonDifference.between(before, after);
            assertEquals(after, difference.appliedTo(before), pair[0] + " → " + pair[1]);
            assertEquals(after, JsonPatch.of(difference.written(mapper.getNodeFactory())).appliedTo(before), "as written: " + pair[0]);
        }
        assertEquals(0, JsonDifference.between(json("{'a':[1]}"), json("{'a':[1]}")).operations().size());
    }

    @Test
    void changesALayoutsTracksInPlace() {
        JsonNode layout = json("{'tracks':[{'points':[[0,0],[1,0]]},{'points':[[2,2],[3,3]]}],'placements':[{'x':1}]}");
        JsonNode kept = JsonPatch.of(json("[{'op':'replace','path':'/placements/0/x','value':7},"
                + "{'op':'replace','path':'/tracks/1/points/1','value':[4,4]},{'op':'add','path':'/tracks/-','value':{'points':[]}}]"))
                .applyInPlace(layout);
        assertEquals(json("{'tracks':[{'points':[[0,0],[1,0]]},{'points':[[2,2],[4,4]]},{'points':[]}],'placements':[{'x':7}]}"), kept);
    }
}
