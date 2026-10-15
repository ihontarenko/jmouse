package org.jmouse.formula;

/** An immutable complex number with the principal branches of the multivalued functions. */
public record Complex(double real, double imaginary) {

    public static final Complex ZERO = new Complex(0, 0);
    public static final Complex ONE = new Complex(1, 0);
    public static final Complex I = new Complex(0, 1);

    public static Complex of(double real) {
        return new Complex(real, 0);
    }

    public static Complex polar(double modulus, double argument) {
        return new Complex(modulus * Math.cos(argument), modulus * Math.sin(argument));
    }

    public double modulus() {
        return Math.hypot(real, imaginary);
    }

    public double modulusSquared() {
        return real * real + imaginary * imaginary;
    }

    public double argument() {
        return Math.atan2(imaginary, real);
    }

    public boolean isFinite() {
        return Double.isFinite(real) && Double.isFinite(imaginary);
    }

    public Complex add(Complex other) {
        return new Complex(real + other.real, imaginary + other.imaginary);
    }

    public Complex subtract(Complex other) {
        return new Complex(real - other.real, imaginary - other.imaginary);
    }

    public Complex multiply(Complex other) {
        return new Complex(real * other.real - imaginary * other.imaginary,
                real * other.imaginary + imaginary * other.real);
    }

    public Complex divide(Complex other) {
        double denominator = other.modulusSquared();
        return new Complex((real * other.real + imaginary * other.imaginary) / denominator,
                (imaginary * other.real - real * other.imaginary) / denominator);
    }

    public Complex negate() {
        return new Complex(-real, -imaginary);
    }

    public Complex conjugate() {
        return new Complex(real, -imaginary);
    }

    public Complex exp() {
        return polar(Math.exp(real), imaginary);
    }

    public Complex log() {
        return new Complex(Math.log(modulus()), argument());
    }

    public Complex power(Complex exponent) {
        if (real == 0 && imaginary == 0) {
            return exponent.real == 0 && exponent.imaginary == 0 ? ONE : ZERO;
        }
        if (exponent.imaginary == 0 && exponent.real == Math.rint(exponent.real) && Math.abs(exponent.real) <= 64) {
            return integerPower((int) exponent.real);
        }
        return exponent.multiply(log()).exp();
    }

    private Complex integerPower(int exponent) {
        Complex base = exponent < 0 ? ONE.divide(this) : this;
        Complex result = ONE;
        for (int remaining = Math.abs(exponent); remaining > 0; remaining >>= 1) {
            if ((remaining & 1) == 1) {
                result = result.multiply(base);
            }
            base = base.multiply(base);
        }
        return result;
    }

    public Complex sqrt() {
        double modulus = modulus();
        double root = Math.sqrt((modulus + Math.abs(real)) / 2);
        if (root == 0) {
            return ZERO;
        }
        return real >= 0 ? new Complex(root, imaginary / (2 * root))
                : new Complex(Math.abs(imaginary) / (2 * root), Math.copySign(root, imaginary));
    }

    public Complex sin() {
        return new Complex(Math.sin(real) * Math.cosh(imaginary), Math.cos(real) * Math.sinh(imaginary));
    }

    public Complex cos() {
        return new Complex(Math.cos(real) * Math.cosh(imaginary), -Math.sin(real) * Math.sinh(imaginary));
    }

    public Complex tan() {
        return sin().divide(cos());
    }

    public Complex sinh() {
        return new Complex(Math.sinh(real) * Math.cos(imaginary), Math.cosh(real) * Math.sin(imaginary));
    }

    public Complex cosh() {
        return new Complex(Math.cosh(real) * Math.cos(imaginary), Math.sinh(real) * Math.sin(imaginary));
    }

    public Complex tanh() {
        return sinh().divide(cosh());
    }

    @Override
    public String toString() {
        return imaginary >= 0 ? real + "+" + imaginary + "i" : real + "" + imaginary + "i";
    }
}
