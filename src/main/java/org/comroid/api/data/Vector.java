package org.comroid.api.data;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.comroid.api.info.Assert;
import org.jetbrains.annotations.Contract;

import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

@SuppressWarnings("unused")
public interface Vector {
    int IndexX = 0;
    int IndexY = 1;
    int IndexZ = 2;
    int IndexW = 3;
    Vector Zero = units(4,0);
    Vector One = units(4,1);
    N4 UnitX = new N4(1,0,0,0);
    N4 UnitY = new N4(0,1,0,0);
    N4 UnitZ = new N4(0,0,1,0);
    N4 UnitW = new N4(0,0,0,1);

    default int n() {
        return 0;
    }
    
    double[] toArray();
    default Vector ctor() {
        return ctor(new double[4]);
    }
    Vector ctor(double... dim);
    default Vector map(DoubleUnaryOperator op) {
        return ctor(DoubleStream.of(toArray()).map(op).toArray());
    }

    default double get(int dim) {
        if (dim < n()) switch (dim) {
            case 0: return getX();
            case 1: return getY();
            case 2: return getZ();
            case 3: return getW();
        }
        throw outOfBounds(dim+1);
    }
    default Vector set(int dim, double value) {
        if (dim < n()) switch (dim) {
            case 0: return setX(value);
            case 1: return setY(value);
            case 2: return setZ(value);
            case 3: return setW(value);
        }
        throw outOfBounds(dim+1);
    }
    double getX();
    double getY();
    double getZ();
    double getW();
    Vector setX(double value);
    Vector setY(double value);
    Vector setZ(double value);
    Vector setW(double value);

    @Contract(mutates = "this") default void $(BiFunction<Vector, Vector, Vector> mod, Vector other) {var result = mod.apply(this, other);
        Assert.Equal(n(),result.n());IntStream.range(0, n()).forEach(i->set(i,result.get(i)));}
    @Contract(mutates = "this") default void $(BiFunction<Vector, Double, Vector> mod, double other) {var result = mod.apply(this, other);Assert.Equal(n(),result.n());IntStream.range(0, n()).forEach(i->set(i,result.get(i)));}
    default Vector not() {return muli(-1);}
    default Vector addi(double other) {return addi(units(n(), other));}
    default Vector subi(double other) {return subi(units(n(), other));}
    default Vector muli(double other) {return muli(units(n(), other));}
    default Vector divi(double other) {return divi(units(n(), other));}
    default Vector modi(double other) {return modi(units(n(), other));}
    default Vector addi(Vector other) {var a = toArray();var b = toArray();var r = ctor();for (var i = 0;i<n();i++)r.set(i,a[i]+b[i]);return r;}
    default Vector subi(Vector other) {var a = toArray();var b = toArray();var r = ctor();for (var i = 0;i<n();i++)r.set(i,a[i]-b[i]);return r;}
    default Vector muli(Vector other) {var a = toArray();var b = toArray();var r = ctor();for (var i = 0;i<n();i++)r.set(i,a[i]*b[i]);return r;}
    default Vector divi(Vector other) {var a = toArray();var b = toArray();var r = ctor();for (var i = 0;i<n();i++)r.set(i,a[i]/b[i]);return r;}
    default Vector modi(Vector other) {var a = toArray();var b = toArray();var r = ctor();for (var i = 0;i<n();i++)r.set(i,a[i]%b[i]);return r;}

    default double magnitude() {
        var sum = 0d;
        for (double dim : toArray())
            sum += pow(dim, 2);
        return sqrt(sum);
    }
    
    default Vector normalize() {
        return divi(magnitude());
    }
    
    static Vector of(double... dim) {
        switch (dim.length) {
            case 2: return new N2(dim[0],dim[1]);
            case 3: return new N3(dim[0],dim[1],dim[2]);
            case 4: return new N4(dim[0],dim[1],dim[2],dim[3]);
        }
        throw outOfBounds(dim.length);
    }

    static Vector units(int n, double length) {
        var arr = new double[n];
        Arrays.fill(arr, length);
        return of(arr);
    }

    private static UnsupportedOperationException outOfBounds(int n) {
        return new UnsupportedOperationException("Unsupported Vector dimension: " + n);
    }

    @Data
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PROTECTED)
    class N2 implements Vector {
        public static final N2 Zero = new N2();
        public static final N2 One = new N2(1,1);
        double x, y;

        public N2(double x, double y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public int n() {
            return 2;
        }

        @Override
        public double[] toArray() {
            return new double[]{x,y};
        }

        @Override
        public Vector.N2 ctor(double... dim) {
            return new N2(dim[0],dim[1]);
        }

        @Override
        public double getZ() {
            return 0;
        }

        @Override
        public double getW() {
            return 0;
        }

        @Override
        public Vector setZ(double value) {
            throw Vector.outOfBounds(IndexZ);
        }

        @Override
        public Vector setW(double value) {
            throw Vector.outOfBounds(IndexW);
        }
    }

    @Data
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    @FieldDefaults(level = AccessLevel.PROTECTED)
    class N3 extends N2 {
        public static final N3 Zero = new N3();
        public static final N3 One = new N3(1,1,1);
        double z;

        public N3(double x, double y, double z) {
            super(x, y);
            this.z = z;
        }

        @Override
        public int n() {
            return 3;
        }

        @Override
        public double[] toArray() {
            return new double[]{x,y,z};
        }

        @Override
        public Vector.N2 ctor(double... dim) {
            return new N3(dim[0],dim[1],dim[2]);
        }
    }

    @Data
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    @FieldDefaults(level = AccessLevel.PROTECTED)
    class N4 extends N3 {
        public static final N4 Zero = new N4();
        public static final N4 One = new N4(1,1,1,1);
        double w;

        public N4(double x, double y, double z, double w) {
            super(x, y, z);
            this.w = w;
        }

        @Override
        public int n() {
            return 4;
        }

        @Override
        public double[] toArray() {
            return new double[]{x,y,z,w};
        }

        @Override
        public Vector.N2 ctor(double... dim) {
            return new N4(dim[0],dim[1],dim[2],dim[3]);
        }
    }
}
