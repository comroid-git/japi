package org.comroid.units;

import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

import static org.jetbrains.annotations.ApiStatus.*;

@Value
@Builder
@Experimental
public class UValue extends Number {
    public static final Pattern PATTERN = Pattern.compile("(?<value>-?[0-9]+(\\.[0-9]+)?)(?<unit>.+)?");

    public static UValue parse(String str) {
        var matcher = PATTERN.matcher(str);
        if (!matcher.matches()) throw new IllegalArgumentException("Cannot parse UnitValue");
        var unit = matcher.group("unit");
        return new UValue(Double.parseDouble(matcher.group("value")), unit == null ? null : new Unit(unit));
    }

    double value;
    @Nullable Unit unit;

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
