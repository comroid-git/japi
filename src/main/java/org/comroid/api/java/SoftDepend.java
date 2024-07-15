package org.comroid.api.java;

import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;
import org.comroid.api.Polyfill;
import org.comroid.api.func.ext.Wrap;
import org.comroid.api.func.util.Cache;
import org.comroid.api.func.util.Invocable;
import org.intellij.lang.annotations.Language;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Stream;

import static org.comroid.api.func.ext.Wrap.*;

@Log
@UtilityClass
public class SoftDepend {
    private final Map<String, Class<?>> typeCache = new ConcurrentHashMap<>();
    private final Set<String> nonexistentTypes = new HashSet<>();

    public <T> Wrap<T> run(final @Language(value = "Java", prefix = "import static ", suffix = ";") String name) {
        return Cache.get("SoftDepend @ static " + name, () -> {
            try {
                // type
                var last = name.lastIndexOf('.');
                var typeName = name.substring(0, last);
                var type = Class.forName(typeName);

                // member
                var memberName = name.substring(last + 1);
                last = memberName.indexOf('(');
                if (memberName.indexOf(')') - last > 1)
                    throw new IllegalArgumentException("No method parameters allowed");
                if (last != -1)
                    memberName = memberName.substring(0, last);

                Invocable<T> member;
                try {
                    var method = type.getMethod(memberName);
                    if (!Modifier.isStatic(method.getModifiers()))
                        throw new IllegalArgumentException("Non-static member supplied");

                    member = Invocable.ofMethodCall(method);
                } catch (NoSuchMethodException t) {
                    member = Invocable.ofFieldGet(type.getField(memberName));
                    // false positive
                    //noinspection ConstantValue
                    if (member == null)
                        throw t;
                }

                T value = member.silentAutoInvoke();
                return of(value).castRef();
            } catch (Throwable t) {
                log.log(Level.WARNING, "Could not load soft dependency: " + name + "\n\t"+t);
                return empty();
            }
        });
    }

    public <T> Stream<Class<T>> type(final @Language(value = "Java", prefix = "import ", suffix = ";") String... names) {
        return Arrays.stream(names)
                .map(SoftDepend::type)
                .flatMap(Wrap::stream)
                .map(Polyfill::uncheckedCast);
    }

    public <T> Wrap<Class<T>> type(final @Language(value = "Java", prefix = "import ", suffix = ";") String name) {
        if (nonexistentTypes.contains(name))
            return empty();
        if (typeCache.containsKey(name))
            return of(typeCache.get(name)).castRef();
        try {
            var type = SoftDepend.class.getClassLoader().loadClass(name);
            return of(type).castRef();
        } catch (ClassNotFoundException ignored) {
            log.log(Level.WARNING, "Could not load soft dependency class: " + name);
            nonexistentTypes.add(name);
            return empty();
        }
    }

    public Wrap<Package> pkg(final @Language(value = "Java", prefix = "package ", suffix = ";") String name) {
        if (nonexistentTypes.contains(name))
            return empty();
        if (typeCache.containsKey(name))
            return of(typeCache.get(name)).castRef();
        var pkg = SoftDepend.class.getClassLoader().getDefinedPackage(name);
        return of(pkg).castRef();
    }
}
