package org.comroid.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.annotations.Upgrade;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Optional;

@Experimental
public interface Upgradeable<T> extends Specifiable<T> {
    @Internal
    Logger logger = LogManager.getLogger();

    @SuppressWarnings("unchecked")
    @Experimental
    default <R extends T> @NotNull R upgrade(Class<? super R> target) {
        if (target.isInstance(this))
            return (R) target.cast(this);
        return Arrays.stream(target.getMethods())
                .filter(mtd -> {
                    int mod = mtd.getModifiers();
                    return Modifier.isStatic(mod) && Modifier.isPublic(mod);
                })
                .filter(mtd -> mtd.isAnnotationPresent(Upgrade.class) || mtd.getName().equals("upgrade"))
                .filter(mtd -> mtd.getParameterCount() == 1 && mtd.getParameterTypes()[0].isInstance(this))
                .findAny()
                .map(mtd -> {
                    try {
                        return (R) mtd.invoke(null, this);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException("Error occurred during Upgrade", e);
                    } catch (IllegalAccessException e) {
                        // theoretically unreachable
                        throw new AssertionError("Cannot access Upgrade method " + mtd, e);
                    } catch (ClassCastException e) {
                        // theoretically unreachable
                        throw new AssertionError("Upgrade method returned an invalid type", e);
                    }
                })
                .orElseThrow(() -> new NoSuchElementException("Could not find suitable upgrade method in " + target));
    }

    @Override
    default <R extends T> Optional<R> as(Class<R> type) {
        return Specifiable.super.as(type)
                .map(Optional::ofNullable)
                .orElseGet(() -> {
                    try {
                        return Optional.of(upgrade(type));
                    } catch (Throwable t) {
                        logger.warn("Could not upgrade to type {} when specifying", type, t);
                        return Optional.empty();
                    }
                });
    }
}
