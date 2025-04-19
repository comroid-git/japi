package org.comroid.api.data;

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
}
