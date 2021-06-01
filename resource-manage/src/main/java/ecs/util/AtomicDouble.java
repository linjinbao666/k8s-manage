package ecs.util;

import static java.lang.Double.doubleToLongBits;
import static java.lang.Double.longBitsToDouble;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

public final class AtomicDouble extends Number {
    private static final long serialVersionUID = 12327722191124184L;

    private final AtomicLong bits;

    public AtomicDouble() {
        this(0.0d);
    }

    public AtomicDouble( double initialValue ) {
        bits = new AtomicLong( toLong(initialValue) );
    }

    /**
     * Atomically sets the value to the given updated value
     * if the current value {@code ==} the expected value.
     *
     * @param expect the expected value
     * @param update the new value
     * @return {@code true} if successful. False return indicates that
     * the actual value was not equal to the expected value.
     */
    public final boolean compareAndSet( double expect, double update ) {
        return bits.compareAndSet(toLong(expect), toLong(update));
    }

    /**
     * Sets to the given value.
     *
     * @param newValue the new value
     */
    public final void set( double newValue ) {
        bits.set(toLong(newValue));
    }

    public final double get() {
        return toDouble(bits.get());
    }

    /**
     * Atomically sets to the given value and returns the old value.
     *
     * @param newValue the new value
     * @return the previous value
     */
    public final double getAndSet( double newValue ) {
        return toDouble( bits.getAndSet(toLong(newValue)) );
    }

    /**
     * Atomically sets the value to the given updated value
     * if the current value {@code ==} the expected value.
     *
     * <p><a href="package-summary.html#weakCompareAndSet">May fail
     * spuriously and does not provide ordering guarantees</a>, so is
     * only rarely an appropriate alternative to {@code compareAndSet}.
     *
     * @param expect the expected value
     * @param update the new value
     * @return {@code true} if successful
     */
    public final boolean weakCompareAndSet( double expect, double update ) {
        return bits.weakCompareAndSet(toLong(expect), toLong(update));
    }

    /**
     * Atomically updates the current value with the results of
     * applying the given function to the current and given values,
     * returning the updated value. The function should be
     * side-effect-free, since it may be re-applied when attempted
     * updates fail due to contention among threads.  The function
     * is applied with the current value as its first argument,
     * and the given update as the second argument.
     *
     * @param x                   the update value
     * @param accumulatorFunction a side-effect-free function of two arguments
     * @return the updated value
     * @since 1.8
     */
    public final double accumulateAndGet( double x, DoubleBinaryOperator accumulatorFunction ) {
        double prev, next;
        do {
            prev = get();
            next = accumulatorFunction.applyAsDouble(prev, x);
        } while (!compareAndSet(prev, next));
        return next;
    }

    /**
     * Atomically adds the given value to the current value.
     *
     * @param delta the value to add
     * @return the updated value
     */
    public final double addAndGet( double delta ) {
        return toDouble(bits.addAndGet(toLong(delta)));
    }

    /**
     * Atomically decrements by one the current value.
     *
     * @return the updated value
     */
    public final double decrementAndGet() {
        return addAndGet(-1.0d);
    }

    /**
     * Atomically updates the current value with the results of
     * applying the given function to the current and given values,
     * returning the previous value. The function should be
     * side-effect-free, since it may be re-applied when attempted
     * updates fail due to contention among threads.  The function
     * is applied with the current value as its first argument,
     * and the given update as the second argument.
     *
     * @param x                   the update value
     * @param accumulatorFunction a side-effect-free function of two arguments
     * @return the previous value
     * @since 1.8
     */
    public final double getAndAccumulate( double x, DoubleBinaryOperator accumulatorFunction ) {
        double prev, next;
        do {
            prev = get();
            next = accumulatorFunction.applyAsDouble(prev, x);
        } while (!compareAndSet(prev, next));
        return prev;
    }

    /**
     * Atomically adds the given value to the current value.
     *
     * @param delta the value to add
     * @return the previous value
     */
    public final double getAndAdd( double delta ) {
        return toDouble(bits.getAndAdd(toLong(delta)));
    }

    public final double getAndDecrement() {
        return getAndAdd(-1.0d);
    }

    /**
     * Atomically increments by one the current value.
     *
     * @return the previous value
     */
    public final double getAndIncrement() {
        return getAndAdd(1.0d);
    }

    /**
     * Atomically increments by one the current value.
     *
     * @return the updated value
     */
    public final double incrementAndGet() {
        return addAndGet(1.0d);
    }

    /**
     * Atomically updates the current value with the results of
     * applying the given function, returning the previous value. The
     * function should be side-effect-free, since it may be re-applied
     * when attempted updates fail due to contention among threads.
     *
     * @param updateFunction a side-effect-free function
     * @return the previous value
     * @since 1.8
     */
    public final double getAndUpdate( DoubleUnaryOperator updateFunction ) {
        double prev, next;
        do {
            prev = get();
            next = updateFunction.applyAsDouble(prev);
        } while (!compareAndSet(prev, next));
        return prev;
    }


    /**
     * Eventually sets to the given value.
     *
     * @param newValue the new value
     * @since 1.6
     */
    public final void lazySet( double newValue ) {
        bits.lazySet(toLong(newValue));
        // unsafe.putOrderedLong(this, valueOffset, newValue);
    }

    /**
     * Returns the value of this {@code AtomicLong} as a {@code long}.
     */
    public long longValue() {
        return (long) get();
    }

    /**
     * Returns the String representation of the current value.
     *
     * @return the String representation of the current value
     */
    public String toString() {
        return Double.toString(get());
    }

    /**
     * Atomically updates the current value with the results of
     * applying the given function, returning the updated value. The
     * function should be side-effect-free, since it may be re-applied
     * when attempted updates fail due to contention among threads.
     *
     * @param updateFunction a side-effect-free function
     * @return the updated value
     * @since 1.8
     */
    public final double updateAndGet( DoubleUnaryOperator updateFunction ) {
        double prev, next;
        do {
            prev = get();
            next = updateFunction.applyAsDouble(prev);
        } while (!compareAndSet(prev, next));
        return next;
    }
    /**
     * Returns the value of this {@code AtomicLong} as an {@code int}
     * after a narrowing primitive conversion.
     *
     * @jls 5.1.3 Narrowing Primitive Conversions
     */
    public int intValue() {
        return (int) get();
    }

    /**
     * Returns the value of this {@code AtomicLong} as a {@code float}
     * after a widening primitive conversion.
     *
     * @jls 5.1.2 Widening Primitive Conversions
     */
    public float floatValue() {
        return (float) get();
    }

    /**
     * Returns the value of this {@code AtomicLong} as a {@code double}
     * after a widening primitive conversion.
     *
     * @jls 5.1.2 Widening Primitive Conversions
     */
    public double doubleValue() {
        return MyUtil.doubleFormat(get());
    }

    private static double toDouble( long l ) {
        return longBitsToDouble(l);
    }

    private static long toLong( double delta ) {
        return doubleToLongBits(delta);
    }

}