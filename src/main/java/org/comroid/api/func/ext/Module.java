package org.comroid.api.func.ext;

import org.comroid.api.Polyfill;
import org.comroid.api.attr.LoggerCarrier;
import org.comroid.api.attr.Named;
import org.comroid.api.java.ReflectionHelper;
import org.comroid.api.tree.LifeCycle;
import org.comroid.api.tree.UncheckedCloseable;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings({ "removal", "rawtypes" }) // todo: Fix removal warning
public interface Module extends Named, LifeCycle, Context.Underlying {
    static <CTX extends Context> Collection<? extends Module> findModules(CTX forClass) {
        String            cname   = forClass.getClass().getName();
        String            modList = "modules/" + cname + ".properties";
        ClassLoader       loader  = ClassLoader.getSystemClassLoader();
        final Set<Module> found   = new HashSet<>();

        try {
            Enumeration<URL> resources = loader.getResources(modList);

            Properties prop;
            while (resources.hasMoreElements()) {
                try (InputStream is = resources.nextElement().openStream()) {
                    prop = new Properties();
                    prop.load(is);

                    for (Object it : prop.values()) {
                        Class<?> cls    = Class.forName(it.toString());
                        Module   module = ReflectionHelper.obtainInstance(cls, forClass).into(Module.class);
                        if (module == null)
                            continue;
                        found.add(module);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Could not find Modules for class " + forClass.getClass());
                } catch (ClassNotFoundException e) {
                    forClass.getLogger().log(Level.WARNING, "Could not find module class", e);
                }
            }
        } catch (IOException e) {
            forClass.getLogger().warning("No Modules found for " + cname);
        }

        return found;
    }

    @Override
    Context getUnderlyingContextualProvider();

    class Carrier implements UncheckedCloseable, LoggerCarrier, LifeCycle {
        private final Collection<? extends Module> modules;

        public <CTX extends Context> Carrier(CTX context) {
            modules = findModules(context);
        }

        public Carrier() {
            if (!(this instanceof Context))
                throw new IllegalStateException("ModuleCarrier must implement ContextualProvider");
            modules = findModules(Polyfill.uncheckedCast(this));
        }

        public final Collection<? extends Module> getModules() {
            return Collections.unmodifiableCollection(modules);
        }

        @Override
        public void close() {
            terminate();
        }

        @Override
        public final void terminate() {
            Logger logger = getLogger();

            for (Module module : modules) {
                try {
                    logger.log(Level.FINE, "Terminating Module: " + module.getName());
                    module.terminate();
                    logger.log(Level.INFO, "Module {} terminated", module.getName());
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        }

        @Override
        public final void initialize() {
            Logger logger = getLogger();

            for (Module module : modules) {
                try {
                    logger.log(Level.FINE, "Initializing Module: " + module.getName());
                    module.initialize();
                    logger.log(Level.INFO, "Module {} initialized", module.getName());
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        }
    }

    abstract class Abstract<CTX extends Context> implements Module {
        private final CTX context;
        private final String name;

        protected Abstract(CTX context) {
            this.context = context;
            this.name    = getClass().getSimpleName();
        }

        protected Abstract(CTX context, String name) {
            this.context = context;
            this.name    = name;
        }

        @Override
        public final String getName() {
            return name;
        }

        @Override
        public final CTX getUnderlyingContextualProvider() {
            return context;
        }

        @Override
        @OverrideOnly
        public void terminate() throws Throwable {
        }

        @Override
        @OverrideOnly
        public void initialize() throws Throwable {
        }
    }
}
