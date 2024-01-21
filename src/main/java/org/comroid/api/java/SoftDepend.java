package org.comroid.api.java;

import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;
import org.comroid.api.func.ext.Wrap;
import org.comroid.api.func.util.Cache;
import org.comroid.api.func.util.Invocable;
import org.intellij.lang.annotations.Language;

import java.util.logging.Level;

@Log
@UtilityClass
public class SoftDepend {
    public <T> Wrap<T> run(@Language(value = "Java", prefix = "import static ", suffix = ";") String name) {
        return Cache.get("SoftDepend @ static " + name, () -> {
            try {
                var last = name.lastIndexOf('.');
                var typeName = name.substring(0, last);
                var type = Class.forName(typeName);
                var memberName = name.substring(last + 1);

                Invocable<T> member;
                try {
                    member = Invocable.ofMethodCall(type.getMethod(memberName));
                } catch (NoSuchMethodException t) {
                    member = Invocable.ofFieldGet(type.getField(memberName));
                    // false positive
                    //noinspection ConstantValue
                    if (member == null)
                        throw t;
                }
                return Wrap.of(member.silentAutoInvoke()).castRef();
            } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException ignored) {
                log.log(Level.WARNING, "Could not load soft dependency: " + name);
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
