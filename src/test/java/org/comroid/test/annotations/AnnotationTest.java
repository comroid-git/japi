package org.comroid.test.annotations;

import org.comroid.annotations.Ignore;
import org.comroid.annotations.internal.Annotations;
import org.comroid.api.func.exc.ThrowingBiFunction;
import org.comroid.annotations.internal.Expect;
import org.comroid.annotations.internal.Expects;
import org.comroid.api.info.Log;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.stream.Stream;

import static java.util.stream.Stream.of;
import static org.comroid.annotations.internal.Annotations.*;
import static org.comroid.api.func.util.Streams.Multi.*;

@SuppressWarnings({"RedundantMethodOverride","unused"})
public class AnnotationTest {
    @Test
    public void test() throws NoSuchMethodException {
        Assert.assertFalse("Some expectations weren't met",
                of(Obj1.class, Obj2.class, Obj3.class, Obj4.class)
                        .flatMap(expandFlat($ -> of("getX", "getY", "getZ")))
                        .map(crossA2B(ThrowingBiFunction.rethrowing(Class::getMethod)))
                        .map(crossB2A(mtd -> mtd.getAnnotation(Expects.class)))
                        .flatMap(flatMapA(Stream::ofNullable))
                        .flatMap(flatMapA(expect -> Arrays.stream(expect.value())))
                        .flatMap(filterA(expect -> !expect.onTarget().equals("ignore-why")))
                        .flatMap(merge((expect, method) -> {
                            var result = ignore(method);
                            if (String.valueOf(result).equals(expect.value()))
                                return of(true);
                            Log.at(Level.WARNING, Annotations.toString(expect, method));
                            return of(false);
                        }))
                        .toList()
                        .contains(false));
        ;
    }

    /** should not be ignored */
    @Expect(value = "false", onTarget = "ignore")
    public static class Obj1 {
        /** should be ignored */
        @Ignore
        @Expect(value = "true", onTarget = "ignore")
        public void getX() {}

        /** should be ignored */
        @Ignore
        @Expect(value = "true", onTarget = "ignore")
        public void getY() {}

        /** should not be ignored */
        @Expect(value = "false", onTarget = "ignore")
        public void getZ() {}}

    /** should not be ignored */
    @Expect(value = "false", onTarget = "ignore")
    public static class Obj2 extends Obj1 {
        /** should not be ignored, reason: Obj1.getX() annotation taken down by @Ignore.Ancestor */
        @Ignore.Inherit
        @Expect(value = "false", onTarget = "ignore")
        @Expect(value = "obj1.x", onTarget = "ignore-why")
        public void getX() {}

        /** should be ignored, reason: Obj1.getY() annotation */
        @Expect(value = "true", onTarget = "ignore")
        @Expect(value = "obj1.y", onTarget = "ignore-why")
        public void getY() {}

        /** should not be ignored */
        @Expect(value = "false", onTarget = "ignore")
        public void getZ() {}}

    /** should be ignored */
    @Ignore
    @Expect(value = "true", onTarget = "ignore")
    @Expect(value = "obj3", onTarget = "ignore-why")
    public static class Obj3 extends Obj2 {
        /** should be ignored, reason: Obj3 annotation */
        @Expect(value = "true", onTarget = "ignore")
        @Expect(value = "obj3", onTarget = "ignore-why")
        public void getX() {}

        /** should not be ignored, reason: Obj3 annotation taken down by @Ignore.Ancestor */
        @Ignore.Inherit(Ignore.class)
        @Expect(value = "false", onTarget = "ignore")
        @Expect(value = "obj3.y", onTarget = "ignore-why")
        public void getY() {}

        /** should be ignored, reason: Obj3 annotation */
        @Expect(value = "true", onTarget = "ignore")
        @Expect(value = "obj3", onTarget = "ignore-why")
        public void getZ() {}}

    /** should not be ignored */
    @Ignore.Inherit
    @Expect(value = "false", onTarget = "ignore")
    @Expect(value = "obj4", onTarget = "ignore-why")
    public static class Obj4 extends Obj3 {
        /** should not be ignored */
        @Expect(value = "false", onTarget = "ignore")
        @Expect(value = "obj4", onTarget = "ignore-why")
        public void getX() {}

        /** should not be ignored */
        @Expect(value = "false", onTarget = "ignore")
        @Expect(value = "obj4", onTarget = "ignore-why")
        public void getY() {}

        /** should not be ignored */
        @Expect(value = "false", onTarget = "ignore")
        @Expect(value = "obj4", onTarget = "ignore-why")
        public void getZ() {}
    }
}
