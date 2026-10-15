package org.jmouse.parallax.analysis;

import org.jmouse.formula.FormulaPrinter;
import org.jmouse.parallax.object.BoundFunction;
import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.DefinitionCompiler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exact first and second derivatives of z = f(x, y), shared by gradient, normals, curvature, tangent plane and
 * critical points so they all agree with the surface and with each other. The two variables are the surface's own,
 * whatever its author named them.
 */
final class Differentials {

    final BoundFunction value;
    final BoundFunction dx;
    final BoundFunction dy;
    final BoundFunction dxx;
    final BoundFunction dyy;
    final BoundFunction dxy;
    private final CompiledObject object;
    private final String source;
    private final String first;
    private final String second;

    Differentials(CompiledObject object, String source) {
        List<String> plane = DefinitionCompiler.variablesOf(object.definition());
        this.object = object;
        this.source = source;
        this.first = plane.get(0);
        this.second = plane.get(1);
        this.value = object.function(source, plane);
        org.jmouse.formula.Formula fx = object.derivativeExpression(source, first);
        org.jmouse.formula.Formula fy = object.derivativeExpression(source, second);
        this.dx = object.function(fx, plane);
        this.dy = object.function(fy, plane);
        this.dxx = object.function(object.derivativeExpression(fx, first), plane);
        this.dyy = object.function(object.derivativeExpression(fy, second), plane);
        this.dxy = object.function(object.derivativeExpression(fx, second), plane);
    }

    double gaussian(double x, double y) {
        double p = dx.at(x, y);
        double q = dy.at(x, y);
        double denominator = 1 + p * p + q * q;
        return (dxx.at(x, y) * dyy.at(x, y) - dxy.at(x, y) * dxy.at(x, y)) / (denominator * denominator);
    }

    double mean(double x, double y) {
        double p = dx.at(x, y);
        double q = dy.at(x, y);
        double denominator = 1 + p * p + q * q;
        return ((1 + p * p) * dyy.at(x, y) - 2 * p * q * dxy.at(x, y) + (1 + q * q) * dxx.at(x, y))
                / (2 * Math.pow(denominator, 1.5));
    }

    Map<String, String> symbolicText() {
        Map<String, String> text = new LinkedHashMap<>();
        text.put("f", FormulaPrinter.text(value.expression()));
        text.put("∂f/∂" + first, FormulaPrinter.text(dx.expression()));
        text.put("∂f/∂" + second, FormulaPrinter.text(dy.expression()));
        text.put("∂²f/∂" + first + "²", FormulaPrinter.text(dxx.expression()));
        text.put("∂²f/∂" + second + "²", FormulaPrinter.text(dyy.expression()));
        text.put("∂²f/∂" + first + "∂" + second, FormulaPrinter.text(dxy.expression()));
        return text;
    }

    Map<String, String> symbolicLatex() {
        Map<String, String> latex = new LinkedHashMap<>();
        latex.put("f", FormulaPrinter.latex(value.expression()));
        latex.put("∂f/∂" + first, FormulaPrinter.latex(dx.expression()));
        latex.put("∂f/∂" + second, FormulaPrinter.latex(dy.expression()));
        latex.put("∂²f/∂" + first + "²", FormulaPrinter.latex(dxx.expression()));
        latex.put("∂²f/∂" + second + "²", FormulaPrinter.latex(dyy.expression()));
        latex.put("∂²f/∂" + first + "∂" + second, FormulaPrinter.latex(dxy.expression()));
        return latex;
    }

    String source() {
        return source;
    }

    CompiledObject object() {
        return object;
    }
}
