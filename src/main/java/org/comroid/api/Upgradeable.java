package org.comroid.api;

import org.comroid.annotations.Upgrade;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

@Experimental
public interface Upgradeable extends LoggerCarrier {
    @SuppressWarnings("unchecked")
    @Experimental
    default <R> @NotNull R upgrade(Class<? super R> target) {
        if (target.isInstance(this))
            return (R) target.cast(this);
        return Stream.concat(Arrays.stream(target.getMethods()), Arrays.stream(target.getConstructors()))
                .filter(mtd -> {
                    int mod = mtd.getModifiers();
                    return Modifier.isStatic(mod) && Modifier.isPublic(mod);
                })
                .filter(mtd -> mtd.isAnnotationPresent(Upgrade.class) || mtd.getName().equals("upgrade"))
                .filter(mtd -> mtd.getParameterCount() == 1 && mtd.getParameterTypes()[0].isInstance(this))
                .findAny()
                .map(Invocable::ofExecutable)
                .map(task -> {
                    try {
                        return (R) task.invoke(null, this);
                    } catch (InvocationTargetException | InstantiationException e) {
                        throw new RuntimeException("Error occurred during Upgrade", e);
                    } catch (IllegalAccessException e) {
                        // theoretically unreachable
                        throw new AssertionError("Cannot access Upgrade method " + task, e);
                    } catch (ClassCastException e) {
                        // theoretically unreachable
                        throw new AssertionError("Upgrade method returned an invalid type", e);
                    }
                })
                .orElseThrow(() -> new NoSuchElementException("Could not find suitable upgrade method in " + target));
    }
}
