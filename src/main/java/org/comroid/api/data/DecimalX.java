package org.comroid.api.data;

import lombok.Value;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.LongPredicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Value
public class DecimalX extends Number implements NumberOps<DecimalX> {
    private static final Comparator<Digit>           DIGITS_BY_POSITION = Comparator.comparingLong(Digit::pos).reversed();
    private static final Digit                       DIGIT_ZERO         = new Digit(0, 0);
    public static final  DecimalX                    ZERO;
    public static final  DecimalX                    ONE;
    public static final  DecimalX                    ONE_NEGATIVE;
    public static final  DecimalX                    MIN_VALUE;
    public static final  DecimalX                    MAX_VALUE;
    public static final  INumberDescriptor<DecimalX> NUMBER_DESCRIPTOR;

    static {
        ZERO         = parse("0");
        ONE          = parse("1");
        ONE_NEGATIVE = parse("-1");

        var ultra = IntStream.range(0, Integer.MAX_VALUE).mapToObj(p -> new Digit(9, p)).toArray(Digit[]::new);
        MIN_VALUE = new DecimalX(ultra, true);
        MAX_VALUE = new DecimalX(ultra, false);

        NUMBER_DESCRIPTOR = new INumberDescriptor.Constant<>(Integer.MAX_VALUE,
                true,
                MIN_VALUE,
                MAX_VALUE,
                ZERO,
                ONE,
                ONE_NEGATIVE) {
            @Override
            public Function<String, DecimalX> getParse() {
                return DecimalX::parse;
            }
        };
    }

    public static DecimalX parse(String str) {
        str = str.trim();

        final var   bytes     = str.getBytes(StandardCharsets.US_ASCII);
        List<Digit> buf       = new ArrayList<>();
        var         iDecPoint = str.indexOf('.') - 1;
        var         negative  = bytes[0] == '-';
        char        c;

        for (var i = negative ? 1 : 0; i < bytes.length; i++) {
            c = (char) bytes[i];
            if (c == '.') {
                iDecPoint++;
                continue;
            } else if (Character.toUpperCase(c) == 'E') {
                var expLen = bytes.length - i - 1;
                var arr    = new byte[expLen];
                System.arraycopy(bytes, i + 1, arr, 0, arr.length);
                var exp = Long.parseLong(new String(arr));
                var old = buf.toArray(Digit[]::new);
                buf.clear();
                for (var digit : old) buf.add(new Digit(digit.digit, digit.pos + exp));
                break;
            }
            var digit = c - '0';
            if (digit == 0) continue;
            buf.add(new Digit(digit, iDecPoint == -1 ? (bytes.length - (i + 1)) : iDecPoint - i));
        }

        return new DecimalX(buf.toArray(Digit[]::new), negative);
    }

    Digit[] digits;
    boolean negative;

    private DecimalX(Digit[] digits, boolean negative) {
        this.digits   = digits;
        this.negative = negative;
    }

    @Override
    public INumberDescriptor<DecimalX> numDesc() {
        return NUMBER_DESCRIPTOR;
    }

    @Override
    public String toString() {
        final var sb = new StringBuilder();
        if (negative) sb.append('-');

        var min = stream().mapToLong(Digit::pos).min().orElse(0) - 1;
        var max = stream().mapToLong(Digit::pos).max().orElse(0);
        for (var p = max; p > min; p--) {
            if (p == -1) sb.append('.');
            var digit = pos(p).orElse(new Digit(0, p));
            sb.append(digit.digit);
        }

        return sb.toString();
    }

    @Override
    public int intValue() {
        return (int) longValue();
    }

    @Override
    public long longValue() {
        return (long) concat(i -> i >= 0);
    }

    @Override
    public float floatValue() {
        return (float) doubleValue();
    }

    @Override
    public double doubleValue() {
        double buf = longValue();
        if (negative) buf *= -1;
        buf = buf + concat(i -> i < 0);
        if (negative) buf *= -1;
        return buf;
    }

    private double concat(LongPredicate posFilter) {
        var buf = 0d;
        for (var digit : array())
            if (posFilter.test(digit.pos)) buf += digit.digit * Math.pow(10, digit.pos);
        if (negative) buf *= -1;
        return buf;
    }

    private Stream<Digit> stream() {
        return Arrays.stream(digits).sorted(DIGITS_BY_POSITION);
    }

    private Digit[] array() {
        return stream().toArray(Digit[]::new);
    }

    private Optional<Digit> pos(long i) {
        return stream().filter(digit -> digit.pos == i).findAny();
    }

    private record Digit(long digit, long pos) {}
}
