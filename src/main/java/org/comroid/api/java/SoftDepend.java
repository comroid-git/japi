package org.comroid.api.java;

import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;
import org.comroid.api.func.ext.Wrap;
import org.comroid.api.func.util.Cache;
import org.comroid.api.func.util.Invocable;
import org.intellij.lang.annotations.Language;

import java.lang.reflect.Modifier;
import java.util.logging.Level;

@Log
@UtilityClass
public class SoftDepend {
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
                return Wrap.of(value).castRef();
            } catch (Throwable t) {
                log.log(Level.WARNING, "Could not load soft dependency: " + name + "\n\t"+t);
                return Wrap.empty();
            }
        });
    }

    public <T> Wrap<Class<T>> type(@Language(value = "Java", prefix = "import ", suffix = ";") String name) {
        return Cache.get("SoftDepend @ type " + name, () -> {
            try {
                var type = ClassLoader.getSystemClassLoader().loadClass(name);
                return Wrap.of(type).castRef();
            } catch (ClassNotFoundException ignored) {
                log.log(Level.WARNING, "Could not load soft dependency class: " + name);
                return Wrap.empty();
            }
        });
    }
}
