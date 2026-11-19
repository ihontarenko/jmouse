package org.jmouse.java.analysis;

import org.jmouse.java.model.version.JavaSourceProfile;
import org.jmouse.java.parser.JavaSyntaxResult;
import org.jmouse.java.parser.node.unit.JavaCompilationUnitNode;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record JavaAnalysisRequest(
        List<JavaSyntaxResult<JavaCompilationUnitNode>> units,
        List<Path> classPath,
        List<Path> modulePath,
        JavaSourceProfile profile) {
    public JavaAnalysisRequest {
        units = List.copyOf(units);
        classPath = List.copyOf(classPath);
        modulePath = List.copyOf(modulePath);
        Objects.requireNonNull(profile, "profile");
        if (units.isEmpty()
                || units.stream().map(unit -> unit.source().name()).distinct().count()
                        != units.size()) {
            throw new IllegalArgumentException("Analysis requires uniquely named source units");
        }
        for (JavaSyntaxResult<JavaCompilationUnitNode> unit : units) {
            if (!unit.profile().release().equals(profile.release())
                    || !unit.profile().preview().equals(profile.preview())
                    || !unit.profile()
                            .specification()
                            .specificationFingerprint()
                            .equals(profile.specification().specificationFingerprint())) {
                throw new IllegalArgumentException(
                        "Source units and analysis must use the same source profile");
            }
        }
    }

    public static JavaAnalysisRequest of(List<JavaSyntaxResult<JavaCompilationUnitNode>> units) {
        if (units.isEmpty()) {
            throw new IllegalArgumentException("Analysis requires a source unit");
        }
        return new JavaAnalysisRequest(units, List.of(), List.of(), units.getFirst().profile());
    }

    public JavaAnalysisRequest withClassPath(List<Path> paths) {
        return new JavaAnalysisRequest(units, paths, modulePath, profile);
    }
}
