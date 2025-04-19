package org.comroid.api.data;

import lombok.Value;
import lombok.experimental.NonFinal;

import java.util.function.Function;

public interface INumberDescriptor<T> {
    int getSizeof();

    boolean isSupportDecimals();

    T getMinValue();

    T getMaxValue();

    T getZero();

    T getOne();

    T getNegativeOne();

    Function<String, T> getParse();

    @Value
    @NonFinal
    abstract class Constant<T> implements INumberDescriptor<T> {
        int     sizeof;
        boolean supportDecimals;
        T       minValue, maxValue, zero, one, negativeOne;
    }
}
