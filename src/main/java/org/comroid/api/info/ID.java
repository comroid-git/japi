package org.comroid.api.info;

import lombok.experimental.UtilityClass;
import org.comroid.api.Polyfill;
import org.comroid.api.attr.UUIDContainer;
import org.comroid.api.data.seri.DataStructure;
import org.comroid.api.func.exc.ThrowingFunction;
import org.comroid.api.java.ReflectionHelper;
import org.comroid.api.java.SoftDepend;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.UUID;

import static org.comroid.api.func.ext.Wrap.empty;

@UtilityClass
public class ID {
    public @NotNull String of(@NotNull Object it) {
        if (it instanceof UUID uuid)
            return uuid.toString();
        if (it instanceof UUIDContainer uuidContainer)
            return of(uuidContainer.getUuid());
        return SoftDepend.type("javax.persistence.Id", "jakarta.persistence.Id")
                .map(Polyfill::<Class<Annotation>>uncheckedCast)
                .flatMap(idAT -> ReflectionHelper.fieldWithAnnotation(it.getClass(), idAT).stream())
                .filter(fld -> !Modifier.isStatic(fld.getModifiers()) && (fld.canAccess(it) || fld.trySetAccessible()))
                .findAny()
                .map(ThrowingFunction.fallback(fld -> fld.get(it), empty()))
                .or(() -> DataStructure.of(it.getClass())
                        .getProperty("id").wrap()
                        .map(prop -> prop.getFrom(it)))
                .map(ID::of)
                .orElseGet(it::toString);
    }
}
