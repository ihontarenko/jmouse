package svit.graph;

import java.util.List;

public class Example {

    public static void main(String[] args) {
        List<String> strings = List.of("A", "B", "C", "D");

        for (Edge<String> edge : Graph.toEdges(strings)) {
            System.out.println(edge);
        }
    }

}
