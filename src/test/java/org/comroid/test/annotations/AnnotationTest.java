package org.comroid.test.annotations;

import org.comroid.annotations.Ignore;
import org.comroid.api.func.exc.ThrowingBiFunction;
import org.comroid.annotations.internal.Expect;
import org.comroid.annotations.internal.Expects;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Stream.of;
import static org.comroid.annotations.internal.Annotations.*;
import static org.comroid.api.func.util.Streams.Multi.*;
import static org.comroid.test.Dummy.*;
import static org.junit.Assert.*;

@SuppressWarnings({"RedundantMethodOverride","unused"})
public class AnnotationTest {
    @Test
    public void testAlias() {
        assertArrayEquals("no alias detected", new Object[]{AliasFruit}, aliases(Fruit.class).toArray());
        assertEquals("not all aliases detected", 2, aliases(Apple.class).stream().filter(List.of(AliasApple, AliasFruit)::contains).count());
        assertArrayEquals("invalid aliases", new Object[]{AliasBanana}, aliases(Banana.class).toArray());
    }

    @Test
    public void testInheritance() throws NoSuchMethodException {
        of(Obj1.class, Obj2.class, Obj3.class, Obj4.class)
                .flatMap(explodeFlat($->of("getX","getY","getZ")))
                .map(crossA2B(ThrowingBiFunction.rethrowing(Class::getMethod)))
                .map(crossB2A(mtd -> mtd.getAnnotation(Expects.class)))
                .flatMap(flatMapA(Stream::ofNullable))
                .flatMap(flatMapA(expect -> Arrays.stream(expect.value())))
                .
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
        @Ignore.Ancestor
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
        @Ignore.Ancestor(Ignore.class)
        @Expect(value = "false", onTarget = "ignore")
        @Expect(value = "obj3.y", onTarget = "ignore-why")
        public void getY() {}

        /** should be ignored, reason: Obj3 annotation */
        @Expect(value = "true", onTarget = "ignore")
        @Expect(value = "obj3", onTarget = "ignore-why")
        public void getZ() {}}

    /** should not be ignored */
    @Ignore.Ancestor
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
