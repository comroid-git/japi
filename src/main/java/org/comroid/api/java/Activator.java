package org.comroid.api.java;

import lombok.Value;
import lombok.extern.java.Log;
import org.comroid.annotations.internal.Annotations;
import org.comroid.api.data.seri.DataNode;
import org.comroid.api.data.bind.DataStructure;
import org.comroid.api.data.seri.type.ValueType;
import org.comroid.api.func.ext.Wrap;
import org.comroid.api.func.util.Cache;
import org.comroid.api.func.util.Streams;

import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

import static org.comroid.api.Polyfill.uncheckedCast;

@Log
@Value
public class Activator<T> {
    public static <R> Activator<R> get(Class<R> target) {
        return Cache.get("Activator"+target.getCanonicalName(), ()->new Activator<>(target));
    }

    Class<T> target;
    DataStructure<T> struct;

    private Activator(Class<T> target) {
        this.target = target;
        this.struct = DataStructure.of(target);
    }

    public T createInstance(DataNode data) {
        if (data instanceof DataNode.Value<?> value)
            return uncheckedCast(value.getValue());
        final var obj = data.asObject();
        return struct.getConstructors().stream()
                .sorted(Comparator.<DataStructure<T>.Constructor>comparingInt(ctor -> ctor.getArgs().size()).reversed())
                .filter(ctor -> ctor.getArgs().size() <= data.size())
                .filter(ctor -> ctor.getArgs().stream()
                        .flatMap(param -> Annotations.aliases(param).stream())
                        .allMatch(obj::containsKey))
                .findFirst()
                .map(ctor -> {
                    var args = new Object[ctor.getArgs().size()];

                    for (int i = 0; i < args.length; i++) {
                        var param = ctor.getArgs().get(i);
                        args[i] = Wrap.ofOptional(Annotations.aliases(param).stream()
                                .collect(Streams.append(param.getName()))
                                .filter(Predicate.not(String::isBlank))
                                .filter(obj::containsKey)
                                .findAny()
                                .map(obj::get))
                                .flatMap(it -> it.as(ValueType.of(param.getType())))
                                .orElseThrow();
                    }

                    final var it = ctor.getCtor().invokeRethrow(args);

                    obj.entrySet().stream()
                            .flatMap(e -> struct.getProperty(e.getKey())
                                    .filter(DataStructure.Property::canSet)
                                    .peek(prop -> prop.setFor(it, e.getValue()
                                            .as(uncheckedCast(prop.getType().getTargetClass()), "unable to cast")))
                                    .stream())
                            .forEach(prop -> log.fine("Injected %s for %s".formatted(prop, it)));

                    return it;
                })
                .orElseThrow(() -> new NoSuchElementException("No suitable constructor was found for "+struct));
    }
}
