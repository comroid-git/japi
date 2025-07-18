package org.comroid.api.data;

import jakarta.persistence.AttributeConverter;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import org.comroid.api.info.Assert;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import static java.lang.Math.*;

@SuppressWarnings("unused")
public interface Vector extends Cloneable {
    int    IndexX = 0;
    int    IndexY = 1;
    int    IndexZ = 2;
    int    IndexW = 3;
    Vector Zero   = units(4, 0);
    Vector One    = units(4, 1);
    N4     UnitX  = new N4(1, 0, 0, 0);
    N4     UnitY  = new N4(0, 1, 0, 0);
    N4     UnitZ  = new N4(0, 0, 1, 0);
    N4     UnitW  = new N4(0, 0, 0, 1);

    static Vector units(int n, double length) {
        var arr = new double[n];
        Arrays.fill(arr, length);
        return of(arr);
    }

    static Vector of(double... dim) {
        return switch (dim.length) {
            case 2 -> new N2(dim[0], dim[1]);
            case 3 -> new N3(dim[0], dim[1], dim[2]);
            case 4 -> new N4(dim[0], dim[1], dim[2], dim[3]);
            default -> throw outOfBounds(dim.length);
        };
    }

    static Vector min(Vector a, Vector b) {
        var vec = a.ctor();
        for (var dim = 0; dim < a.n(); dim++) vec.set(dim, Math.min(a.get(dim), b.get(dim)));
        return vec;
    }

    static Vector max(Vector a, Vector b) {
        var vec = a.ctor();
        for (var dim = 0; dim < a.n(); dim++) vec.set(dim, Math.max(a.get(dim), b.get(dim)));
        return vec;
    }

    static Vector rel(Vector root, Vector vector) {
        return root.subi(vector);
    }

    static double dist(Vector a, Vector b) {
        Vector min = min(a, b), max = max(a, b);
        var    acc = 0.0;
        for (var i = 0; i < a.n(); i++)
            acc += pow(max.get(i) - min.get(i), 2);
        return sqrt(acc);
    }

    double getX();

    Vector setX(double value);

    double getY();

    Vector setY(double value);

    double getZ();

    Vector setZ(double value);

    double getW();

    Vector setW(double value);

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    default Vector clone() {
        return switch (n()) {
            case 2 -> new N2(getX(), getY());
            case 3 -> new N3(getX(), getY(), getZ());
            case 4 -> new N4(getX(), getY(), getZ(), getW());
            default -> throw new RuntimeException(new CloneNotSupportedException());
        };
    }

    default Vector intCast() {
        return map(x -> (int) x);
    }

    default Vector intRound() {
        return map(Math::round);
    }

    default Vector intFloor() {
        return map(Math::floor);
    }

    default Vector intCeil() {
        return map(Math::ceil);
    }

    default DoubleStream stream() {
        return Arrays.stream(toArray());
    }

    default Vector map(DoubleUnaryOperator op) {
        return ctor(DoubleStream.of(toArray()).map(op).toArray());
    }

    default double get(int dim) {
        if (dim < n()) switch (dim) {
            case 0:
                return getX();
            case 1:
                return getY();
            case 2:
                return getZ();
            case 3:
                return getW();
        }
        throw outOfBounds(dim + 1);
    }

    @Contract(mutates = "this")
    default void $(BiFunction<Vector, Vector, Vector> mod, Vector other) {
        var result = mod.apply(this, other);
        Assert.Equal(n(), result.n());
        IntStream.range(0, n()).forEach(i -> set(i, result.get(i)));
    }

    default Vector muli(Vector other) {
        var a = toArray();
        var b = other.toArray();
        var r = ctor();
        for (var i = 0; i < n(); i++) r.set(i, a[i] * b[i]);
        return r;
    }

    double[] toArray();

    default int n() {
        return 0;
    }

    default Vector set(int dim, double value) {
        if (dim < n()) switch (dim) {
            case 0:
                return setX(value);
            case 1:
                return setY(value);
            case 2:
                return setZ(value);
            case 3:
                return setW(value);
        }
        throw outOfBounds(dim + 1);
    }

    default Vector ctor() {
        return ctor(new double[4]);
    }

    @Contract(mutates = "this")
    default void $(BiFunction<Vector, Double, Vector> mod, double other) {
        var result = mod.apply(this, other);
        Assert.Equal(n(), result.n());
        IntStream.range(0, n()).forEach(i -> set(i, result.get(i)));
    }

    default Vector not() {
        return muli(-1);
    }

    default Vector muli(double other) {
        return muli(units(n(), other));
    }

    Vector ctor(double... dim);

    default Vector addi(double other) {
        return addi(units(n(), other));
    }

    default Vector addi(Vector other) {
        var a = toArray();
        var b = other.toArray();
        var r = ctor();
        for (var i = 0; i < n(); i++) r.set(i, a[i] + b[i]);
        return r;
    }

    default Vector subi(double other) {
        return subi(units(n(), other));
    }

    default Vector subi(Vector other) {
        var a = toArray();
        var b = other.toArray();
        var r = ctor();
        for (var i = 0; i < n(); i++) r.set(i, a[i] - b[i]);
        return r;
    }

    default Vector modi(double other) {
        return modi(units(n(), other));
    }

    default Vector modi(Vector other) {
        var a = toArray();
        var b = other.toArray();
        var r = ctor();
        for (var i = 0; i < n(); i++) r.set(i, a[i] % b[i]);
        return r;
    }

    default Vector normalize() {
        return divi(magnitude());
    }

    default Vector divi(double other) {
        return divi(units(n(), other));
    }

    default Vector divi(Vector other) {
        var a = toArray();
        var b = other.toArray();
        var r = ctor();
        for (var i = 0; i < n(); i++) r.set(i, a[i] / b[i]);
        return r;
    }

    default double magnitude() {
        var sum = 0d;
        for (double dim : toArray())
            sum += pow(dim, 2);
        return sqrt(sum);
    }

    default N2 as2() {
        return (N2) this;
    }

    default N3 as3() {
        return (N3) this;
    }

    default N4 as4() {
        return (N4) this;
    }

    default Vector negate() {
        return map(x -> -x);
    }

    private static UnsupportedOperationException outOfBounds(int n) {
        return new UnsupportedOperationException("Unsupported Vector dimension: " + n);
    }

    @Data
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PROTECTED)
    class N2 implements Vector {
        public static final N2 Zero = new N2();
        public static final N2 One  = new N2(1, 1);
        double x, y;

        public N2(double x, double y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public double getZ() {
            return 0;
        }

        @Override
        public Vector setZ(double value) {
            throw Vector.outOfBounds(IndexZ);
        }

        @Override
        public double getW() {
            return 0;
        }

        @Override
        public Vector setW(double value) {
            throw Vector.outOfBounds(IndexW);
        }

        @Override
        public double[] toArray() {
            return new double[]{ x, y };
        }

        @Override
        public int n() {
            return 2;
        }

        @Override
        public Vector.N2 ctor(double... dim) {
            return new N2(dim[0], dim[1]);
        }

        @Override
        public final int hashCode() {
            return Objects.hash((Object[]) stream().boxed().toArray(Double[]::new));
        }

        @Override
        public final boolean equals(Object o) {
            if (!(o instanceof Vector ov)) return false;
            double[] it = toArray(), ot = ov.toArray();
            var      eq = 0;
            for (var i = 0; i < n(); i++) if (abs(Double.compare(it[i], ot[i])) < 0.000_01) eq++;
            return eq == n();
        }

        @Override
        public Vector clone() {
            return Vector.super.clone();
        }

        @Override
        public final String toString() {
            final var dims = new char[]{ 'x', 'y', 'z', 'w' };
            var       ls   = new StringBuilder("(");
            for (var n = 0; n < n(); n++)
                ls.append(dims[n]).append('=').append(get(n)).append(';');
            return ls.substring(0, ls.length() - 1) + ')';
        }

        @ApiStatus.Experimental
        public N3 to3(double z) {
            return new N3(x, y, z);
        }
    }

    @Getter
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PROTECTED)
    class N3 extends N2 {
        public static final N3     Zero = new N3();
        public static final N3     One  = new N3(1, 1, 1);
        @Setter             double z;

        public N3(double x, double y, double z) {
            super(x, y);
            this.z = z;
        }

        @Override
        public double[] toArray() {
            return new double[]{ x, y, z };
        }

        @Override
        public int n() {
            return 3;
        }

        @Override
        public Vector.N2 ctor(double... dim) {
            return new N3(dim[0], dim[1], dim[2]);
        }

        @ApiStatus.Experimental
        public N4 to4(double w) {
            return new N4(x, y, z, w);
        }

        @Value
        @jakarta.persistence.Converter(autoApply = true)
        public static class Converter implements AttributeConverter<N3, byte[]> {
            @Override
            public byte[] convertToDatabaseColumn(N3 attribute) {
                var buf = ByteBuffer.allocate(attribute.n() */*byte count of double*/8);
                var i   = -8;
                buf.putDouble(i += 8, attribute.x);
                buf.putDouble(i += 8, attribute.y);
                buf.putDouble(i, attribute.z);
                return buf.array();
            }

            @Override
            public N3 convertToEntityAttribute(byte[] dbData) {
                var buf = ByteBuffer.wrap(dbData);
                var i   = -8;
                var x   = buf.getDouble(i += 8);
                var y   = buf.getDouble(i += 8);
                var z   = buf.getDouble(i);
                return new N3(x, y, z);
            }
        }
    }

    @Getter
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PROTECTED)
    class N4 extends N3 {
        public static final N4     Zero = new N4();
        public static final N4     One  = new N4(1, 1, 1, 1);
        @Setter             double w;

        public N4(double x, double y, double z, double w) {
            super(x, y, z);
            this.w = w;
        }

        @Override
        public double[] toArray() {
            return new double[]{ x, y, z, w };
        }

        @Override
        public int n() {
            return 4;
        }

        @Override
        public Vector.N2 ctor(double... dim) {
            return new N4(dim[0], dim[1], dim[2], dim[3]);
        }

        @Value
        @jakarta.persistence.Converter(autoApply = true)
        public static class Converter implements AttributeConverter<N4, byte[]> {
            @Override
            public byte[] convertToDatabaseColumn(N4 attribute) {
                var buf = ByteBuffer.allocate(attribute.n() */*byte count of double*/8);
                var i   = -8;
                buf.putDouble(i += 8, attribute.x);
                buf.putDouble(i += 8, attribute.y);
                buf.putDouble(i += 8, attribute.z);
                buf.putDouble(i, attribute.w);
                return buf.array();
            }

            @Override
            public N4 convertToEntityAttribute(byte[] dbData) {
                var buf = ByteBuffer.wrap(dbData);
                var i   = -8;
                var x   = buf.getDouble(i += 8);
                var y   = buf.getDouble(i += 8);
                var z   = buf.getDouble(i += 8);
                var w   = buf.getDouble(i);
                return new N4(x, y, z, w);
            }
        }
    }
}
