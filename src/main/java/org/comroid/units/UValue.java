package org.comroid.units;

import lombok.Builder;
import lombok.Value;

import static org.jetbrains.annotations.ApiStatus.*;

@Value
@Builder
@Experimental
public class UValue extends Number {
    double value;
    Unit   unit;

    @Override
    public int intValue() {
        return (int) value;
    }

    @Override
    public long longValue() {
        return (long) value;
    }

    @Override
    public float floatValue() {
        return (float) value;
    }

    @Override
    public double doubleValue() {
        return value;
    }
}
