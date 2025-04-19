package org.comroid.api.data;

public interface NumberOps<T extends Number> {
    INumberDescriptor<T> numDesc();

    T plus(T other);

    T minus(T other);

    T multiply(T other);

    T divide(T other);

    T modulus(T other);

    default T negate() {
        return multiply(numDesc().getNegativeOne());
    }
}
