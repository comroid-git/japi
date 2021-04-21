package org.comroid.api;

import org.comroid.annotations.Upgrade;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;

@Experimental
public interface Upgradeable<T> {
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
                        throw new RuntimeException("Error ocurred during Upgrade", e);
                    } catch (IllegalAccessException e) {
                        // theoretically unreachable
                        throw new AssertionError("Cannot access Upgrade method " + mtd, e);
                    } catch (ClassCastException e) {
                        // theoretically unreachable
                        throw new AssertionError("Upgrade method returned an invalid type", e);
                    }
                })
                .orElseThrow(() -> new RuntimeException(String.format("Could not upgrade %s to %s", this, target)));
    }
}
