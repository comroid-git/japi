package org.comroid.api.java;

import lombok.Value;
import lombok.extern.java.Log;
import org.comroid.api.data.seri.DataNode;
import org.comroid.api.data.seri.DataStructure;
import org.comroid.api.func.util.Invocable;
import org.comroid.api.Polyfill;
import org.comroid.api.func.util.Cache;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.comroid.api.Polyfill.uncheckedCast;

@Log
@Value
public class Activator<T> {
    public static <R> Activator<R> get(Class<R> target) {
        return Cache.get("Activator"+target.getCanonicalName(), ()->new Activator<>(target));
    }

    Class<T> target;
    DataStructure<T> struct = DataStructure.of(target);

    public T createInstance(DataNode data) {
        final var obj = data.asObject();
        return struct.getConstructors().stream()
                .filter(ctor -> ctor.getArgs().size() < data.size())
                .filter(ctor -> ctor.getArgs().stream()
                        .map(Parameter::getName)
                        .allMatch(obj::containsKey))
                .findAny()
                .map(ctor -> {
                    var args = new Object[ctor.getArgs().size()];

                    for (int i = 0; i < args.length; i++) {
                        var entry = ctor.getArgs().get(i);
                        args[i] = obj.get(entry.getName()).asType(uncheckedCast(entry.getType()));
                    }

                    final var it = ctor.getCtor().invokeRethrow(args);

                    obj.entrySet().stream()
                            .flatMap(e -> struct.getProperty(e.getKey())
                                    .filter(DataStructure.Property::canSet)
                                    .peek(prop -> prop.setFor(it, e.getValue()
                                            .asType(uncheckedCast(prop.getType().getTargetClass()))))
                                    .stream())
                            .forEach(prop -> log.fine("Injected %s for %s".formatted(prop, it)));

                    return it;
                })
                .orElseThrow(() -> new NoSuchElementException("No suitable constructor was found for "+struct));
    }
}
