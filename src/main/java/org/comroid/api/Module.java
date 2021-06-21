package org.comroid.api;

import org.apache.logging.log4j.Logger;
import org.comroid.util.ReflectionHelper;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

public interface Module extends Named, LifeCycle, ContextualProvider.Underlying {
    @Override
    ContextualProvider getUnderlyingContextualProvider();

    static <CTX extends ContextualProvider> Collection<? extends Module> findModules(CTX forClass) {
        String cname = forClass.getClass().getName();
        String modList = "modules/" + cname + ".properties";
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        final Set<Module> found = new HashSet<>();

        try {
            Enumeration<URL> resources = loader.getResources(modList);

            Properties prop;
            while (resources.hasMoreElements()) {
                try (InputStream is = resources.nextElement().openStream()) {
                    prop = new Properties();
                    prop.load(is);

                    for (Object it : prop.values()) {
                        Class<?> cls = Class.forName(it.toString());
                        Module module = ReflectionHelper.obtainInstance(cls, forClass).into(Module.class);
                        if (module == null)
                            continue;
                        found.add(module);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Could not find Modules for class " + forClass.getClass());
                } catch (ClassNotFoundException e) {
                    forClass.getLogger().warn("Could not find module class", e);
                }
            }
        } catch (IOException e) {
            forClass.getLogger().warn("No Modules found for " + cname);
        }

        return found;
    }

    class Carrier implements UncheckedCloseable, LoggerCarrier, LifeCycle {
        private final Collection<? extends Module> modules;

        public <CTX extends ContextualProvider> Carrier(CTX context) {
            modules = findModules(context);
        }

        public Carrier() {
            if (!(this instanceof ContextualProvider))
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
        public final void initialize() {
            Logger logger = getLogger();

            for (Module module : modules) {
                try {
                    logger.debug("Initializing Module: " + module.getName());
                    module.initialize();
                    logger.info("Module {} initialized", module.getName());
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        }

        @Override
        public final void terminate() {
            Logger logger = getLogger();

            for (Module module : modules) {
                try {
                    logger.debug("Terminating Module: " + module.getName());
                    module.terminate();
                    logger.info("Module {} terminated", module.getName());
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        }
    }

    abstract class Abstract<CTX extends ContextualProvider> implements Module {
        private final CTX context;
        private final String name;

        @Override
        public final String getName() {
            return name;
        }

        @Override
        public final CTX getUnderlyingContextualProvider() {
            return context;
        }

        protected Abstract(CTX context) {
            this.context = context;
            this.name = getClass().getSimpleName();
        }

        protected Abstract(CTX context, String name) {
            this.context = context;
            this.name = name;
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
