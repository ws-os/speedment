package com.speedment.runtime.compute.internal.expression;

import com.speedment.runtime.compute.*;
import com.speedment.runtime.compute.expression.Expression;
import com.speedment.runtime.compute.expression.UnaryExpression;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Utility class for creating expressions that gives the sign ({@code 1},
 * {@code -1} or {@code 0}) depending on if the result of another expression is
 * positive, negative or {@code 0}.
 *
 * @author Emil Forslund
 * @since  3.1.0
 */
public final class SignUtil {

    private final static byte NEGATIVE = -1, POSITIVE = 1, ZERO = 0;

    /**
     * Returns an expression that wraps another expression and returns
     * {@code -1} if its result is negative, {@code 1} if its result is positive
     * and {@code 0} if its result is equal to {@code 0}.
     *
     * @param expression  the expression to wrap
     * @param <T>  the input entity type
     * @return  sign of the result of the wrapped expression
     */
    public static <T> ToByte<T> sign(ToDouble<T> expression) {
        class DoubleSign extends AbstractSign<T, ToDouble<T>> {
            private DoubleSign(ToDouble<T> tToDouble) {
                super(tToDouble);
            }

            @Override
            public byte applyAsByte(T object) {
                final double value = inner.applyAsDouble(object);
                return value < 0 ? NEGATIVE : (value > 0 ? POSITIVE : ZERO);
            }
        }

        return new DoubleSign(expression);
    }

    /**
     * Returns an expression that wraps another expression and returns
     * {@code -1} if its result is negative, {@code 1} if its result is positive
     * and {@code 0} if its result is equal to {@code 0}.
     *
     * @param expression  the expression to wrap
     * @param <T>  the input entity type
     * @return  sign of the result of the wrapped expression
     */
    public static <T> ToByte<T> sign(ToFloat<T> expression) {
        class FloatSign extends AbstractSign<T, ToFloat<T>> {
            private FloatSign(ToFloat<T> tToFloat) {
                super(tToFloat);
            }

            @Override
            public byte applyAsByte(T object) {
                final float value = inner.applyAsFloat(object);
                return value < 0 ? NEGATIVE : (value > 0 ? POSITIVE : ZERO);
            }
        }

        return new FloatSign(expression);
    }

    /**
     * Returns an expression that wraps another expression and returns
     * {@code -1} if its result is negative, {@code 1} if its result is positive
     * and {@code 0} if its result is equal to {@code 0}.
     *
     * @param expression  the expression to wrap
     * @param <T>  the input entity type
     * @return  sign of the result of the wrapped expression
     */
    public static <T> ToByte<T> sign(ToLong<T> expression) {
        class LongSign extends AbstractSign<T, ToLong<T>> {
            private LongSign(ToLong<T> tToLong) {
                super(tToLong);
            }

            @Override
            public byte applyAsByte(T object) {
                final long value = inner.applyAsLong(object);
                return value < 0 ? NEGATIVE : (value > 0 ? POSITIVE : ZERO);
            }
        }

        return new LongSign(expression);
    }

    /**
     * Returns an expression that wraps another expression and returns
     * {@code -1} if its result is negative, {@code 1} if its result is positive
     * and {@code 0} if its result is equal to {@code 0}.
     *
     * @param expression  the expression to wrap
     * @param <T>  the input entity type
     * @return  sign of the result of the wrapped expression
     */
    public static <T> ToByte<T> sign(ToInt<T> expression) {
        class IntSign extends AbstractSign<T, ToInt<T>> {
            private IntSign(ToInt<T> tToInt) {
                super(tToInt);
            }

            @Override
            public byte applyAsByte(T object) {
                final int value = inner.applyAsInt(object);
                return value < 0 ? NEGATIVE : (value > 0 ? POSITIVE : ZERO);
            }
        }

        return new IntSign(expression);
    }

    /**
     * Returns an expression that wraps another expression and returns
     * {@code -1} if its result is negative, {@code 1} if its result is positive
     * and {@code 0} if its result is equal to {@code 0}.
     *
     * @param expression  the expression to wrap
     * @param <T>  the input entity type
     * @return  sign of the result of the wrapped expression
     */
    public static <T> ToByte<T> sign(ToShort<T> expression) {
        class ShortSign extends AbstractSign<T, ToShort<T>> {
            private ShortSign(ToShort<T> tToShort) {
                super(tToShort);
            }

            @Override
            public byte applyAsByte(T object) {
                final short value = inner.applyAsShort(object);
                return value < 0 ? NEGATIVE : (value > 0 ? POSITIVE : ZERO);
            }
        }

        return new ShortSign(expression);
    }

    /**
     * Returns an expression that wraps another expression and returns
     * {@code -1} if its result is negative, {@code 1} if its result is positive
     * and {@code 0} if its result is equal to {@code 0}.
     *
     * @param expression  the expression to wrap
     * @param <T>  the input entity type
     * @return  sign of the result of the wrapped expression
     */
    public static <T> ToByte<T> sign(ToByte<T> expression) {
        class ByteSign extends AbstractSign<T, ToByte<T>> {
            private ByteSign(ToByte<T> tToByte) {
                super(tToByte);
            }

            @Override
            public byte applyAsByte(T object) {
                final byte value = inner.applyAsByte(object);
                return value < 0 ? NEGATIVE : (value > 0 ? POSITIVE : ZERO);
            }
        }

        return new ByteSign(expression);
    }

    /**
     * Internal base implementation for a sign-operation.
     *
     * @param <T>      the input entity type
     * @param <INNER>  the inner expression type
     */
    private abstract static class AbstractSign<T, INNER extends Expression<T>>
    implements UnaryExpression<T, INNER>, ToByte<T> {
        final INNER inner;

        AbstractSign(INNER inner) {
            this.inner = requireNonNull(inner);
        }

        @Override
        public final INNER getInner() {
            return inner;
        }

        @Override
        public final Operator getOperator() {
            return Operator.SIGN;
        }

        @Override
        public final boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof UnaryExpression)) return false;
            final UnaryExpression<?, ?> that = (UnaryExpression<?, ?>) o;
            return Objects.equals(getInner(), that.getInner())
                && Objects.equals(getOperator(), that.getOperator());
        }

        @Override
        public final int hashCode() {
            return Objects.hash(getInner(), getOperator());
        }
    }

    /**
     * Utility classes should not be instantiated.
     */
    private SignUtil() {}
}
