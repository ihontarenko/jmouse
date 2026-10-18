package org.jmouse.java.parser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

/**
 * Explicit corpus tool, not a machine-specific test. Arguments are caller-selected source roots.
 */
public final class CorpusReport {
    public static void main(String[] roots) throws Exception {
        JavaParser parser = new JavaParser();
        for (String root : roots) {
            int count = 0, structural = 0, body = 0;
            long references = 0;
            Map<String, Integer> problems = new TreeMap<>();
            long started = System.nanoTime();
            try (var paths = Files.walk(Path.of(root))) {
                for (Path path :
                        paths.filter(file -> file.toString().endsWith(".java")).sorted().toList()) {
                    String source = Files.readString(path);
                    var declarations = parser.parse(path.toString(), source);
                    var expressions =
                            parser.parse(path.toString(), source, JavaParser.Level.EXPRESSIONS);
                    count++;
                    if (!declarations.isComplete()) {
                        structural++;
                    }
                    if (!expressions.isComplete()) {
                        body++;
                    }
                    references +=
                            expressions.bodies().stream()
                                    .mapToLong(value -> value.references().size())
                                    .sum();
                    for (var problem : expressions.problems()) {
                        problems.merge(problem.kind() + ": " + problem.message(), 1, Integer::sum);
                    }
                }
            }
            System.out.printf(
                    "%s: files=%d structureProblemFiles=%d expressionProblemFiles=%d"
                            + " bodyReferences=%d elapsedMs=%d%n",
                    root,
                    count,
                    structural,
                    body,
                    references,
                    (System.nanoTime() - started) / 1_000_000);
            problems.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(15)
                    .forEach(entry -> System.out.println(entry.getValue() + " " + entry.getKey()));
        }
    }
}
