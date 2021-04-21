package org.comroid.api;

import org.comroid.annotations.Upgrade;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.NotNull;

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
                .filter(mtd -> Modifier.isStatic(mtd.getModifiers()))
                .filter(mtd -> mtd.isAnnotationPresent(Upgrade.class) || mtd.getName().equals("upgrade"))
                .filter(mtd -> mtd.getParameterCount() == 1 && mtd.getParameterTypes()[0].isInstance(this))
                .findAny()
                .map(Invocable::<R>ofMethodCall)
                .map(invoc -> invoc.autoInvoke(this))
                .orElseThrow(() -> new RuntimeException(String.format("Could not upgrade %s to %s", this, target)));
    }
}
