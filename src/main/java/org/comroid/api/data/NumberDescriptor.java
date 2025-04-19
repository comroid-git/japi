package org.comroid.api.data;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.function.Function;

@Getter
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public enum NumberDescriptor implements INumberDescriptor<Number> {
    BYTE(1, false, Byte.MIN_VALUE, Byte.MAX_VALUE, (byte) 0, (byte) 1, (byte) -1, Byte::parseByte),
    SHORT(2, false, Short.MIN_VALUE, Short.MAX_VALUE, (short) 0, (short) 1, (short) -1, Short::parseShort),
    INTEGER(4, false, Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 1, -1, Integer::parseInt),
    LONG(8, false, Long.MIN_VALUE, Long.MAX_VALUE, (long) 0, (long) 1, (long) -1, Long::parseLong),
    FLOAT(4, false, Float.MIN_VALUE, Float.MAX_VALUE, (float) 0, (float) 1, (float) -1, Float::parseFloat),
    DOUBLE(8, false, Double.MIN_VALUE, Double.MAX_VALUE, (double) 0, (double) 1, (double) -1, Double::parseDouble);

    int     sizeof;
    boolean supportDecimals;
    Number  minValue, maxValue, zero, one, negativeOne;
    Function<String, Number> parse;
}
