package org.comroid.api;

import org.comroid.annotations.Convert;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Experimental
public interface Convertible {
    @SuppressWarnings("unchecked")
    @Experimental
    default <R> @NotNull R convert(Class<? super R> target) {
        final var it = this instanceof Supplier ? ((Supplier<?>) this).get() : this;
        if (target.isInstance(it))
            return (R) target.cast(it);
        return Stream.concat(Stream.of(target.getConstructors()),
                        Stream.of(it.getClass(), target)
                                .flatMap(tgt -> Stream.of(tgt.getMethods())
                                        .filter(mtd -> target.isAssignableFrom(mtd.getReturnType()))))
                .filter(exe -> exe.isAnnotationPresent(Convert.class) || exe.getName().equals("upgrade"))
                .filter(exe -> exe.getParameterCount() == 0
                        || exe.getParameterCount() == 1 && exe.getParameterTypes()[0].isInstance(it))
                .findAny()
                .map(Invocable::ofExecutable)
                .map(task -> {
                    try {
                        return (R) task.invoke(this, this);
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
                .orElseThrow(() -> new NoSuchElementException("Could not find suitable upgrade method in " + target));
    }
}
