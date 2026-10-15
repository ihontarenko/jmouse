package org.jmouse.formula;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.Objects;

/**
 * An exact fraction of two integers of any size, always in lowest terms with a positive denominator — so two equal
 * rationals are equal records, and an integer is a rational whose denominator is one.
 */
public record Rational(BigInteger numerator, BigInteger denominator) implements Comparable<Rational> {

    public static final Rational ZERO = new Rational(BigInteger.ZERO, BigInteger.ONE);
    public static final Rational ONE  = new Rational(BigInteger.ONE, BigInteger.ONE);

    public Rational {
        Objects.requireNonNull(numerator, "numerator");
        Objects.requireNonNull(denominator, "denominator");
        if (denominator.signum() == 0) {
            throw new ArithmeticException("Division by zero in an exact formula.");
        }
        if (denominator.signum() < 0) {
            numerator = numerator.negate();
            denominator = denominator.negate();
        }
        BigInteger divisor = numerator.gcd(denominator);
        if (!divisor.equals(BigInteger.ONE) && divisor.signum() != 0) {
            numerator = numerator.divide(divisor);
            denominator = denominator.divide(divisor);
        }
        if (numerator.signum() == 0) {
            denominator = BigInteger.ONE;
        }
    }

    public static Rational of(BigInteger integer) {
        return new Rational(integer, BigInteger.ONE);
    }

    public static Rational of(long integer) {
        return of(BigInteger.valueOf(integer));
    }

    /**
     * The number a decimal literal means, exactly: {@code 0.1} is one tenth, not the binary fraction nearest to it.
     *
     * @throws ArithmeticException for an infinite or undefined value, which no fraction is
     */
    public static Rational ofDecimal(double value) {
        if (!Double.isFinite(value)) {
            throw new ArithmeticException("An exact formula has no fraction for " + value + ".");
        }
        BigDecimal decimal = new BigDecimal(Double.toString(value));
        return decimal.scale() <= 0
                ? of(decimal.toBigIntegerExact())
                : new Rational(decimal.unscaledValue(), BigInteger.TEN.pow(decimal.scale()));
    }

    public boolean isInteger() {
        return denominator.equals(BigInteger.ONE);
    }

    public int signum() {
        return numerator.signum();
    }

    public Rational add(Rational other) {
        return new Rational(numerator.multiply(other.denominator).add(other.numerator.multiply(denominator)),
                denominator.multiply(other.denominator));
    }

    public Rational subtract(Rational other) {
        return add(other.negate());
    }

    public Rational multiply(Rational other) {
        return new Rational(numerator.multiply(other.numerator), denominator.multiply(other.denominator));
    }

    public Rational divide(Rational other) {
        return new Rational(numerator.multiply(other.denominator), denominator.multiply(other.numerator));
    }

    public Rational negate() {
        return new Rational(numerator.negate(), denominator);
    }

    public Rational abs() {
        return signum() < 0 ? negate() : this;
    }

    /** This to an integer power; a negative exponent is the power of the reciprocal. */
    public Rational power(int exponent) {
        if (exponent < 0) {
            return new Rational(denominator.pow(-exponent), numerator.pow(-exponent));
        }
        return new Rational(numerator.pow(exponent), denominator.pow(exponent));
    }

    /** The greatest integer not above this. */
    public BigInteger floor() {
        BigInteger[] parts = numerator.divideAndRemainder(denominator);
        return parts[1].signum() < 0 ? parts[0].subtract(BigInteger.ONE) : parts[0];
    }

    /** The least integer not below this. */
    public BigInteger ceiling() {
        if (isInteger()) {
            return numerator;
        }
        return floor().add(BigInteger.ONE);
    }

    /** The nearest integer, a half going to the even one — as {@code Math.rint} rounds. */
    public BigInteger rounded() {
        BigInteger below = floor();
        Rational fraction = subtract(of(below));
        int againstHalf = fraction.compareTo(new Rational(BigInteger.ONE, BigInteger.TWO));
        if (againstHalf > 0 || (againstHalf == 0 && below.testBit(0))) {
            return below.add(BigInteger.ONE);
        }
        return below;
    }

    /** The larger of the numerator's and the denominator's sizes in bits: what a budget on exact work is kept against. */
    public int bitLength() {
        return Math.max(numerator.bitLength(), denominator.bitLength());
    }

    /** The nearest double; an integer or a fraction too large for one is infinite. */
    public double doubleValue() {
        if (isInteger()) {
            return numerator.doubleValue();
        }
        return new BigDecimal(numerator).divide(new BigDecimal(denominator), MathContext.DECIMAL64).doubleValue();
    }

    @Override
    public int compareTo(Rational other) {
        return numerator.multiply(other.denominator).compareTo(other.numerator.multiply(denominator));
    }

    @Override
    public String toString() {
        return isInteger() ? numerator.toString() : numerator + "/" + denominator;
    }
}
