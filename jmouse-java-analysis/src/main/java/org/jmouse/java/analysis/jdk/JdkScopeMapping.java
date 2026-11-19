package org.jmouse.java.analysis.jdk;

import com.sun.source.tree.Scope;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;

import org.jmouse.java.analysis.JavaScopeLayer;
import org.jmouse.java.analysis.JavaScopeView;
import org.jmouse.java.analysis.JavaVisibleBinding;
import org.jmouse.java.model.symbol.JavaSymbolKey;
import org.jmouse.java.model.symbol.JavaTypeKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.lang.model.element.Element;

final class JdkScopeMapping {
    private final Trees trees;
    private final JdkSymbolMapping symbols;

    JdkScopeMapping(Trees trees, JdkSymbolMapping symbols) {
        this.trees = trees;
        this.symbols = symbols;
    }

    JavaScopeView scope(TreePath path) {
        Scope current = trees.getScope(path);
        Optional<JavaTypeKey> type =
                current.getEnclosingClass() == null
                        ? Optional.empty()
                        : Optional.of(symbols.typeKey(current.getEnclosingClass()));
        Optional<JavaSymbolKey> executable =
                Optional.ofNullable(symbols.key(current.getEnclosingMethod()));
        List<JavaScopeLayer> layers = new ArrayList<>();
        for (Scope scope = current; scope != null; scope = scope.getEnclosingScope()) {
            List<JavaVisibleBinding> bindings = new ArrayList<>();
            for (Element element : scope.getLocalElements()) {
                Optional<JavaSymbolKey> identity =
                        trees.getPath(element) == null
                                ? Optional.empty()
                                : Optional.ofNullable(symbols.key(element));
                bindings.add(
                        new JavaVisibleBinding(
                                element.getSimpleName().toString(),
                                symbols.typeMapping.type(element.asType()),
                                identity));
            }
            layers.add(new JavaScopeLayer(bindings));
        }
        return new JavaScopeView(layers, type, executable);
    }
}
