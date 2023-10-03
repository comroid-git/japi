package org.comroid.util;

import lombok.experimental.UtilityClass;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterator;

@UtilityClass
public class Constraint {
    @UtilityClass
    public class Range {
        public void inside(double xIncl, double yIncl, double actual, String nameof) {
            Length.min(xIncl, actual, nameof+" range lower end");
            Length.max(yIncl, actual, nameof+" range upper end");
        }
    }
    @UtilityClass
    public class Length {
        public void min(double min, Object target, String nameof) {
            int len = Integer.MIN_VALUE;
            if (target.getClass().isArray())
                len = ((Object[]) target).length;
            else if (target instanceof Collection<?>)
                len = ((Collection<?>) target).size();
            else if (target instanceof Map<?,?>)
                len = ((Map<?, ?>) target).size();
            if (len < min)
                throw err("length", target.getClass(), nameof, len, "at least", min);
        }
        public void max(double max, Object target, String nameof) {
            int len = Integer.MAX_VALUE;
            if (target.getClass().isArray())
                len = ((Object[]) target).length;
            else if (target instanceof Collection<?>)
                len = ((Collection<?>) target).size();
            else if (target instanceof Map<?,?>)
                len = ((Map<?, ?>) target).size();
            else if (target instanceof Iterable<?>)
                len = (int)Streams.of((Iterable<?>)target).count();
            else if (target instanceof Spliterator<?>)
                len = (int)Streams.of((Spliterator<?>)target).count();
            if (len > max)
                throw err("length", target.getClass(), nameof, len, "at most", max);
        }
    }
    public void equals(Object x, Object y, String nameof) {
        if (!Objects.equals(x, y))
            throw err("equals", x.getClass(), nameof, x, "equal to [%s] ".formatted(y.getClass().getSimpleName()), y);
    }
    private IllegalArgumentException err(String constraint, Class<?> typeof, String nameof, Object actual, String actualShouldBe$Expected, Object expected) {
        return new IllegalArgumentException("Unmet %s constraint for argument %s %s; %s should be %s %s"
                .formatted(constraint, typeof.getSimpleName(), nameof, actual, actualShouldBe$Expected, expected));
    }
}
