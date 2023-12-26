package org.comroid.api.java;

import org.comroid.api.data.seri.DataNode;
import org.comroid.api.func.util.Invocable;
import org.comroid.api.Polyfill;
import org.comroid.api.func.util.Cache;

import java.util.Map;

public final class Activator<T> {
    public static <R> Activator<R> get(Class<R> target) {
        return Cache.get("Activator"+target.getCanonicalName(), ()->new Activator<>(target));
    }

    private final Class<? extends T> target;

    private Activator(Class<T> target) {
        this.target = target;
    }

    public <R extends T> R createInstance(DataNode.Object dataNode, Object... args) {
        throw new UnsupportedOperationException("todo");//todo
    }
    public <R extends T> R createInstance(Object... args) {
        return Polyfill.uncheckedCast(Invocable.newInstance(target, args));
    }
    public <R extends T> R createInstance(Map<String, Object> args) {
        var ctor = ReflectionHelper.findConstructor(target, args.values()
                .stream()
                .map(Object::getClass)
                .toArray(Class[]::new))
                .orElseThrow(()->new IllegalArgumentException("No suitable constructor found"));
        throw new UnsupportedOperationException("todo");//todo
    }
}
