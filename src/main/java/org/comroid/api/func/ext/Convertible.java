package org.comroid.api.func.ext;

import org.comroid.annotations.internal.Annotations;
import org.comroid.annotations.Convert;
import org.comroid.api.func.util.Invocable;
import org.comroid.api.info.Constraint;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Modifier;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Experimental
public interface Convertible {
    @Experimental
    default <R> @NotNull R convert(Class<? super R> target) {
        return convert(this, target);
    }

    @Experimental
    @SuppressWarnings("unchecked")
    static <R> @NotNull R convert(Object it_, Class<? super R> target) {
        var it = it_ instanceof Supplier && !Annotations.ignore(it_.getClass(), Convertible.class)
                ? ((Supplier<?>) it_).get() : it_;
        Constraint.notNull(it, "source object")
                .setHint("check implementation")
                .run();
        if (target.isInstance(it))
            return (R) target.cast(it);
        return Stream.concat(Stream.of(target.getConstructors()),
                        Stream.of(it.getClass(), target)
                                .flatMap(tgt -> Stream.of(tgt.getMethods())
                                        .filter(mtd -> target.isAssignableFrom(mtd.getReturnType())))
                                .filter(mtd -> Modifier.isStatic(mtd.getModifiers())))
                .filter(exe -> exe.isAnnotationPresent(Convert.class) || exe.getName().equals("upgrade"))
                .filter(exe -> exe.getParameterCount() == 0
                        || exe.getParameterCount() == 1 && exe.getParameterTypes()[0].isInstance(it))
                .findAny()
                .map(Invocable::ofExecutable)
                .map(task -> {
                    try {
                        return (R) task.invoke(it, it);
                    } catch (IllegalAccessException e) {
                        // semantically unreachable; thus we throw an AssertionError
                        throw new AssertionError("Cannot access Upgrade method " + task, e);
                    } catch (ClassCastException e) {
                        // semantically unreachable; thus we throw an AssertionError
                        throw new AssertionError("Upgrade method returned an invalid type", e);
                    } catch (Throwable t) {
                        throw new RuntimeException("Error occurred during Upgrade", t);
                    }
                })
                .orElseThrow(() -> new NoSuchElementException("Could not find suitable converter method from "+it+" to " + target));
    }
}
