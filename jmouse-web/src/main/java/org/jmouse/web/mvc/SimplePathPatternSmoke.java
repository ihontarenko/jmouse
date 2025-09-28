package org.jmouse.web.mvc;

import org.jmouse.web.match.PathPattern;
import org.jmouse.web.match.RouteMatch;
import org.jmouse.web.match.SimplePathPattern;

import java.util.Map;

/**
 * Console smoke tests for SimplePathPattern — no frameworks, just stdout.
 */
public final class SimplePathPatternSmoke {

    public static void main(String[] args) {
        heading("Embedded placeholder inside a segment");
        demo("/users/ID_{id:\\d+}", "/users/ID_42;f=a;f=b"); // ✅ expecting {id=42} and optional matrix under ";id"
        demo("/users/ID_{id:\\d+}", "/users/ID_X");       // ❌ should not match

        heading("Regex-constrained variable");
        demo("/{name:[a-z]+}", "/john");                    // ✅ {name=john}
        demo("/{name:[a-z]+}", "/john123");                 // ❌

        heading("Matrix params bound to {id}");
        demo("/users/{id}", "/users/42;trace=1;flags=a;flags=b"); // expected keys: id=42 and ;id -> {trace=[1], flags=[a,b]}

        heading("Combo template in one segment");
        demo("/report_{y:\\d+}-{m:\\d+}.csv", "/report_2024-09.csv"); // ✅ y=2024, m=09

        heading("Catch-all ** and {*rest}");
        demo("/docs/**", "/docs");
        demo("/docs/**", "/docs/a/b/c");
        demo("/docs/{*rest}", "/docs");
        demo("/docs/{*rest}", "/docs/a/b/c");

        heading("Wildcards & single-char inside segments");
        demo("/img/*.png", "/img/logo.png");                 // ✅
        demo("/files/??.txt", "/files/ab.txt");              // ✅
        demo("/files/??.txt", "/files/a.txt");               // ❌

        heading("Single-char segment and wildcard segment");
        demo("/a/?/c", "/a/b/c");                            // ✅
        demo("/a/?/c", "/a/bb/c");                           // ❌
        demo("/a/*/c", "/a/bbb/c");                          // ✅
        demo("/a/*/c", "/a//c");                             // may depend on split() collapsing empty segments

        heading("Trailing slash sensitivity");
        demo("/users/{id}", "/users/42/");                    // likely ❌ in strict mode
        demo("/users/{id}/", "/users/42/");                   // ✅

        heading("Case sensitivity");
        demo("/Users/{id}", "/users/42");                     // ❌ (engine is case-sensitive)

        heading("Matrix + embedded placeholder");
        demo("/u_{id:\\d+}", "/u_7;role=admin;tag=x;tag=y"); // expected id=7 and ;id map if wired
    }

    private static void demo(String pattern, String path) {
        PathPattern         pp   = SimplePathPattern.parse(pattern);
        Map<String, Object> vars = pp.extractVariables(path);
        String     extracted = pp.extractPath(path);
        RouteMatch match     = pp.match(path);
        System.out.printf("%-34s ~ %-46s => %s : %s%n", pattern, path, vars, extracted);
        System.out.println("  match: " + (match == null ? "null" : match));
    }

    private static void heading(String title) {
        System.out.println();
        System.out.println("== " + title + " ==");
    }
}
