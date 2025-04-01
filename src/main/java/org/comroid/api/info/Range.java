package org.comroid.api.info;

import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.stream.IntStream;

@Value
@Setter
public class Range<T extends Number> {
    @Nullable T start, end;
    @NonFinal @Nullable T lowerBound, upperBound;

    @SuppressWarnings("unchecked")
    public Range(@Nullable T start, @Nullable T end) {
        this(start, end, (T) (Integer) 0, (T) (Integer) 64);
    }

    public Range(@Nullable T start, @Nullable T end, @Nullable T lowerBound, @Nullable T upperBound) {
        this.start      = start;
        this.end        = end;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    public IntStream asIndices() {
        var low  = Objects.requireNonNullElse(start, lowerBound);
        var high = Objects.requireNonNullElse(end, upperBound);
        return IntStream.range((int) low, (int) high);
    }

    public IntStream asEntries() {
        return asIndices().map(x -> x + 1);
    }
}
